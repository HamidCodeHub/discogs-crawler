package com.hamid.discogs.crawler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discogs")
public record DiscogsProperties(
        String baseUrl,
        String token,
        int requestsPerMinute
) {
    public DiscogsProperties {
        if (baseUrl == null) baseUrl = "https://api.discogs.com";
        if (requestsPerMinute <= 0) requestsPerMinute = 60;
    }
}
