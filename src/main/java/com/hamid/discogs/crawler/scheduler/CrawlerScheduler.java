package com.hamid.discogs.crawler.scheduler;

import com.hamid.discogs.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final CrawlerService crawlerService;

    @Scheduled(cron = "${discogs.crawl.cron:0 0 * * * *}")
    public void scheduledCrawl() {
        log.info("Starting scheduled Discogs crawl");
        crawlerService.searchAndCrawl("electronic", 5);
        log.info("Scheduled crawl finished");
    }
}
