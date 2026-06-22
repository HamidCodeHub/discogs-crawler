package com.hamid.discogs.crawler.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hamid.discogs.crawler.client.DiscogsApiClient;
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
import java.util.ArrayList;
import java.util.List;
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

        job.setStatus(CrawlStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        crawlJobRepository.save(job);

        int page = job.getCurrentPage() + 1;
        int totalPages = Math.max(job.getTotalPages(), 1);
        AtomicInteger saved = new AtomicInteger(job.getSavedCount());
        AtomicInteger skipped = new AtomicInteger(job.getSkippedCount());

        try {
            do {
                if (shouldStop(job.getId())) {
                    log.info("Crawl job={} stopped at page {}/{}", job.getId(), page, totalPages);
                    break;
                }

                log.debug("Fetching page {}/{} for query='{}'", page, totalPages, job.getQuery());
                rateLimiter.acquire();
                DiscogsSearchResponse response = apiClient.searchReleases(job.getQuery(), page);

                if (response == null || response.getResults() == null) {
                    log.warn("Empty response for query='{}' page={}, stopping", job.getQuery(), page);
                    break;
                }

                if (response.getPagination() != null) {
                    totalPages = response.getPagination().pages();
                }

                List<SearchResult> newResults = response.getResults().stream()
                        .filter(item -> !releaseRepository.existsByDiscogsId(item.getId()))
                        .toList();
                skipped.addAndGet(response.getResults().size() - newResults.size());

                List<CompletableFuture<Void>> detailFutures = new ArrayList<>();
                for (SearchResult item : newResults) {
                    detailFutures.add(CompletableFuture.runAsync(() -> fetchAndSave(item, saved, skipped), detailFetchExecutor));
                }
                CompletableFuture.allOf(detailFutures.toArray(CompletableFuture[]::new)).join();

                job.setCurrentPage(page);
                job.setTotalPages(totalPages);
                job.setSavedCount(saved.get());
                job.setSkippedCount(skipped.get());
                crawlJobRepository.save(job);

                page++;
            } while (page <= totalPages);

            if (job.getStatus() != CrawlStatus.STOPPED) {
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
        rateLimiter.acquire();
        DiscogsReleaseDetail detail = apiClient.getReleaseById(item.getId());
        if (detail == null) {
            log.warn("No detail for release {}, skipping", item.getId());
            skipped.incrementAndGet();
            return;
        }
        try {
            releaseRepository.save(toEntity(item, detail));
            log.info("Saved release {} - {}", item.getId(), item.getTitle());
            saved.incrementAndGet();
        } catch (DataIntegrityViolationException e) {
            // concurrent insert from a parallel thread — harmless
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

    private ReleaseEntity toEntity(SearchResult item, DiscogsReleaseDetail detail) {
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