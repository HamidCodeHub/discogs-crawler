package com.hamid.discogs.crawler.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DiscogsProperties.class)
public class DiscogsClientConfig {

    @Bean
    public RestClient discogsRestClient(DiscogsProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Discogs token=" + props.token())
                .defaultHeader("User-Agent", "DiscogsCrawler/1.0")
                .build();
    }
}
