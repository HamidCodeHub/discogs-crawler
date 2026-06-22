package com.hamid.discogs.crawler.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public RateLimiter discogsRateLimiter(
            @Value("${discogs.requests-per-minute:60}") int requestsPerMinute) {
        // 90% of limit for a safety margin
        return RateLimiter.create(requestsPerMinute * 0.9 / 60.0);
    }

    // One active crawl at a time; extra requests queue up to 5
    @Bean("crawlerExecutor")
    public ThreadPoolTaskExecutor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("crawler-");
        executor.initialize();
        return executor;
    }

    // Parallel threads for fetching release details within a page
    @Bean("detailFetchExecutor")
    public ThreadPoolTaskExecutor detailFetchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("detail-");
        executor.initialize();
        return executor;
    }
}