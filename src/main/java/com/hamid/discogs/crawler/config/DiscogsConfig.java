package com.hamid.discogs.crawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DiscogsProperties.class)
public class DiscogsConfig {

    @Bean
    public RestClient discogsRestClient(@Value("${discogs.token}") String token) {
        return RestClient.builder()
                .baseUrl("https://api.discogs.com")
                .defaultHeader("Authorization", "Discogs token=" + token)
                .defaultHeader("User-Agent", "DiscogsCrawler/1.0")
                .build();
    }
}
