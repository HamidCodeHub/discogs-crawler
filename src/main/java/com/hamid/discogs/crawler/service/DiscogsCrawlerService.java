package com.hamid.discogs.crawler.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsMarketplaceStats;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
import com.hamid.discogs.crawler.dto.SearchResult;
import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import com.hamid.discogs.crawler.entity.ReleaseEntity;
import com.hamid.discogs.crawler.repository.CrawlJobRepository;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DiscogsCrawlerService {

    private final DiscogsApiClient apiClient;
    private final ReleaseRepository releaseRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final RateLimiter rateLimiter;
    private final Executor detailFetchExecutor;

    public DiscogsCrawlerService(
            DiscogsApiClient apiClient,
            ReleaseRepository releaseRepository,
            CrawlJobRepository crawlJobRepository,
            RateLimiter rateLimiter,
            @Qualifier("detailFetchExecutor") Executor detailFetchExecutor) {
        this.apiClient = apiClient;
        this.releaseRepository = releaseRepository;
        this.crawlJobRepository = crawlJobRepository;
        this.rateLimiter = rateLimiter;
        this.detailFetchExecutor = detailFetchExecutor;
    }

    @Async("crawlerExecutor")
    public CompletableFuture<Void> crawl(CrawlJob job) {
        log.info("Crawl job={} starting for query='{}' at page {}", job.getId(), job.getQuery(), job.getCurrentPage() + 1);

        if (shouldStop(job.getId())) {
            log.info("Crawl job={} was stopped before it started", job.getId());
            return CompletableFuture.completedFuture(null);
        }

        job.setStatus(CrawlStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        crawlJobRepository.save(job);

        int page = job.getCurrentPage() + 1;
        int totalPages = Math.max(job.getTotalPages(), 1);
        AtomicInteger saved = new AtomicInteger(job.getSavedCount());
        AtomicInteger skipped = new AtomicInteger(job.getSkippedCount());
        boolean stoppedExternally = false;

        // Pipeline: holds the pre-fetched next page search result
        CompletableFuture<DiscogsSearchResponse> prefetched = null;

        try {
            do {
                if (shouldStop(job.getId())) {
                    log.info("Crawl job={} stopped at page {}/{}", job.getId(), page, totalPages);
                    stoppedExternally = true;
                    if (prefetched != null) prefetched.cancel(true);
                    break;
                }

                // Use pre-fetched result if available, otherwise fetch now
                DiscogsSearchResponse response;
                if (prefetched != null) {
                    response = prefetched.join();
                    prefetched = null;
                } else {
                    rateLimiter.acquire();
                    response = apiClient.searchReleases(job.getQuery(), page);
                }

                if (response == null || response.getResults() == null) {
                    log.warn("Empty response for query='{}' page={}, stopping", job.getQuery(), page);
                    break;
                }

                if (response.getPagination() != null) {
                    totalPages = response.getPagination().pages();
                }

                // Batch DB check — one query for the whole page instead of one per item
                List<Long> pageIds = response.getResults().stream().map(SearchResult::getId).toList();
                Set<Long> existingIds = releaseRepository.findExistingIds(pageIds);
                List<SearchResult> newResults = response.getResults().stream()
                        .filter(item -> !existingIds.contains(item.getId()))
                        .toList();
                skipped.addAndGet(pageIds.size() - newResults.size());

                // Pipeline: pre-fetch next page search while processing this page's details
                int nextPage = page + 1;
                if (nextPage <= totalPages) {
                    prefetched = CompletableFuture.supplyAsync(() -> {
                        rateLimiter.acquire();
                        return apiClient.searchReleases(job.getQuery(), nextPage);
                    }, detailFetchExecutor);
                }

                // Fetch detail + marketplace stats in parallel for each new release
                List<CompletableFuture<Void>> futures = newResults.stream()
                        .map(item -> CompletableFuture.runAsync(
                                () -> fetchAndSave(item, saved, skipped), detailFetchExecutor))
                        .toList();
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

                job.setCurrentPage(page);
                job.setTotalPages(totalPages);
                job.setSavedCount(saved.get());
                job.setSkippedCount(skipped.get());
                crawlJobRepository.save(job);

                page++;
            } while (page <= totalPages);

            if (!stoppedExternally) {
                job.setStatus(CrawlStatus.COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                crawlJobRepository.save(job);
                log.info("Crawl job={} completed — saved={} skipped={}", job.getId(), saved.get(), skipped.get());
            }
        } catch (Exception e) {
            log.error("Crawl job={} failed: {}", job.getId(), e.getMessage(), e);
            job.setStatus(CrawlStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            crawlJobRepository.save(job);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void fetchAndSave(SearchResult item, AtomicInteger saved, AtomicInteger skipped) {
        // Sequential calls within the thread — avoids submitting nested tasks back to the
        // same pool (which would deadlock when the pool is fully occupied by fetchAndSave tasks)
        rateLimiter.acquire();
        DiscogsReleaseDetail detail = apiClient.getReleaseById(item.getId());

        rateLimiter.acquire();
        DiscogsMarketplaceStats stats = apiClient.getMarketplaceStats(item.getId());

        if (detail == null) {
            log.warn("No detail for release {}, skipping", item.getId());
            skipped.incrementAndGet();
            return;
        }

        try {
            releaseRepository.save(toEntity(item, detail, stats));
            log.info("Saved release {} - {} (price={}{})",
                    item.getId(), item.getTitle(),
                    stats != null && stats.getLowestPrice() != null ? stats.getLowestPrice().getValue() : "n/a",
                    stats != null && stats.getLowestPrice() != null ? " " + stats.getLowestPrice().getCurrency() : "");
            saved.incrementAndGet();
        } catch (DataIntegrityViolationException e) {
            log.debug("Release {} already saved by concurrent thread, skipping", item.getId());
            skipped.incrementAndGet();
        }
    }

    private boolean shouldStop(Long jobId) {
        if (Thread.currentThread().isInterrupted()) return true;
        return crawlJobRepository.findById(jobId)
                .map(j -> j.getStatus() == CrawlStatus.STOPPED)
                .orElse(false);
    }

    private ReleaseEntity toEntity(SearchResult item, DiscogsReleaseDetail detail, DiscogsMarketplaceStats stats) {
        ReleaseEntity entity = new ReleaseEntity();

        entity.setDiscogsId(item.getId());
        entity.setTitle(item.getTitle());
        entity.setReleaseYear(parseYear(item.getYear()));
        entity.setCountry(item.getCountry());
        entity.setType(item.getType());
        entity.setUri(item.getUri());
        entity.setCatno(item.getCatno());
        entity.setThumb(item.getThumb());
        entity.setCoverImage(item.getCoverImage());
        entity.setResourceUrl(item.getResourceUrl());
        entity.setMasterId(item.getMasterId());
        entity.setMasterUrl(item.getMasterUrl());
        entity.setFormatQuantity(item.getFormatQuantity());
        entity.setGenres(joinList(item.getGenre()));
        entity.setStyles(joinList(item.getStyle()));
        entity.setFormats(joinList(item.getFormat()));
        entity.setLabels(joinList(item.getLabel()));
        entity.setBarcodes(joinList(item.getBarcode()));

        if (item.getUserData() != null) {
            entity.setInWantlist(item.getUserData().isInWantlist());
            entity.setInCollection(item.getUserData().isInCollection());
        }
        if (item.getCommunity() != null) {
            entity.setCommunityWant(item.getCommunity().getWant());
            entity.setCommunityHave(item.getCommunity().getHave());
        }

        entity.setArtistsSort(detail.getArtistsSort());
        entity.setNotes(detail.getNotes());

        if (stats != null && stats.getLowestPrice() != null) {
            entity.setLowestPrice(stats.getLowestPrice().getValue());
            entity.setPriceCurrency(stats.getLowestPrice().getCurrency());
        }
        if (stats != null) {
            entity.setNumForSale(stats.getNumForSale());
        }

        entity.setFetchedAt(LocalDateTime.now());
        return entity;
    }

    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) return null;
        try {
            int y = Integer.parseInt(year.trim());
            return y > 0 ? y : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String joinList(List<String> list) {
        return (list == null || list.isEmpty()) ? null : String.join(", ", list);
    }
}
