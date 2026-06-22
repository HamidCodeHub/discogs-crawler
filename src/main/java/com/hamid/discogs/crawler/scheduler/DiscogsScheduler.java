package com.hamid.discogs.crawler.scheduler;

import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import com.hamid.discogs.crawler.repository.CrawlJobRepository;
import com.hamid.discogs.crawler.service.DiscogsCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscogsScheduler {

    private final DiscogsCrawlerService crawlerService;
    private final CrawlJobRepository crawlJobRepository;

    @Value("${discogs.crawl.query:electronic}")
    private String crawlQuery;

    @Scheduled(fixedDelayString = "${discogs.crawl.delay:3600000}")
    public void scheduledCrawl() {
        if (crawlJobRepository.existsByStatus(CrawlStatus.RUNNING)) {
            log.info("Scheduled crawl skipped — a crawl is already running");
            return;
        }

        // Resume last incomplete job, or start fresh
        CrawlJob job = crawlJobRepository
                .findTopByQueryAndStatusInOrderByStartedAtDesc(
                        crawlQuery, List.of(CrawlStatus.FAILED, CrawlStatus.STOPPED))
                .orElseGet(() -> {
                    CrawlJob newJob = new CrawlJob();
                    newJob.setQuery(crawlQuery);
                    newJob.setStatus(CrawlStatus.PENDING);
                    return crawlJobRepository.save(newJob);
                });

        log.info("Scheduled crawl triggered — job={} query='{}' resuming from page {}",
                job.getId(), job.getQuery(), job.getCurrentPage() + 1);
        job.setStatus(CrawlStatus.PENDING);
        crawlJobRepository.save(job);
        crawlerService.crawl(job);
    }
}