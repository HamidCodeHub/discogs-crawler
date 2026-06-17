package com.hamid.discogs.crawler.scheduler;

import com.hamid.discogs.crawler.service.DiscogsCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscogsScheduler {

    private final DiscogsCrawlerService crawlerService;

    @Value("${discogs.crawl.query:electronic}")
    private String crawlQuery;

    @Scheduled(fixedDelayString = "${discogs.crawl.delay:3600000}")
    public void scheduledCrawl() {
        log.info("Scheduled crawl triggered for query='{}'", crawlQuery);
        crawlerService.crawl(crawlQuery);
    }
}
