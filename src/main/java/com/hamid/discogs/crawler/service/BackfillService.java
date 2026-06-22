package com.hamid.discogs.crawler.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsMarketplaceStats;
import com.hamid.discogs.crawler.entity.ReleaseEntity;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class BackfillService {

    private static final int BATCH_SIZE = 100;

    private final DiscogsApiClient apiClient;
    private final ReleaseRepository releaseRepository;
    private final RateLimiter rateLimiter;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BackfillService(
            DiscogsApiClient apiClient,
            ReleaseRepository releaseRepository,
            RateLimiter rateLimiter) {
        this.apiClient = apiClient;
        this.releaseRepository = releaseRepository;
        this.rateLimiter = rateLimiter;
    }

    public boolean isRunning() {
        return running.get();
    }

    @Async("crawlerExecutor")
    public CompletableFuture<Void> backfillPrices() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Backfill already running, ignoring request");
            return CompletableFuture.completedFuture(null);
        }

        AtomicInteger updated = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        try {
            log.info("Backfill started — fetching marketplace prices for releases with no price");

            List<ReleaseEntity> batch;
            do {
                batch = releaseRepository.findAllWithoutPrice(PageRequest.of(0, BATCH_SIZE));
                if (batch.isEmpty()) break;

                for (ReleaseEntity release : batch) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("Backfill interrupted");
                        return CompletableFuture.completedFuture(null);
                    }

                    rateLimiter.acquire();
                    DiscogsMarketplaceStats stats = apiClient.getMarketplaceStats(release.getDiscogsId());

                    if (stats != null) {
                        if (stats.getLowestPrice() != null) {
                            release.setLowestPrice(stats.getLowestPrice().getValue());
                            release.setPriceCurrency(stats.getLowestPrice().getCurrency());
                        }
                        // Always set numForSale (even 0) — marks this release as checked so
                        // it won't be picked up again by findAllWithoutPrice
                        release.setNumForSale(stats.getNumForSale() != null ? stats.getNumForSale() : 0);
                        releaseRepository.save(release);
                        updated.incrementAndGet();
                    } else {
                        // API call failed — leave numForSale null so it will be retried
                        skipped.incrementAndGet();
                    }
                }

                log.info("Backfill progress — updated={} skipped={}", updated.get(), skipped.get());

            } while (!batch.isEmpty());

            log.info("Backfill completed — updated={} skipped={}", updated.get(), skipped.get());
        } catch (Exception e) {
            log.error("Backfill failed: {}", e.getMessage(), e);
        } finally {
            running.set(false);
        }

        return CompletableFuture.completedFuture(null);
    }

    public void stop() {
        if (running.get()) {
            log.info("Backfill stop requested");
            running.set(false);
        }
    }
}
