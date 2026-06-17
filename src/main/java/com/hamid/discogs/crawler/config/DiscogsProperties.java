package com.hamid.discogs.crawler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discogs")
public record DiscogsProperties(
        String baseUrl,
        String token,
        int requestsPerMinute,
        Crawl crawl
) {
    public record Crawl(long delay) {
        public Crawl {
            if (delay <= 0) delay = 1000;
        }
    }

    public DiscogsProperties {
        if (baseUrl == null) baseUrl = "https://api.discogs.com";
        if (requestsPerMinute <= 0) requestsPerMinute = 60;
        if (crawl == null) crawl = new Crawl(1000);
    }
}
