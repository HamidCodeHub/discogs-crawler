package com.hamid.discogs.crawler.client;

import com.hamid.discogs.crawler.dto.DiscogsMarketplaceStats;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class DiscogsApiClient {

    private final RestClient restClient;

    public DiscogsApiClient(RestClient discogsRestClient) {
        this.restClient = discogsRestClient;
    }

    public DiscogsSearchResponse searchReleases(String query, int page) {
        try {
            return restClient.get()
                    .uri(b -> b.path("/database/search")
                            .queryParam("q", query)
                            .queryParam("type", "release")
                            .queryParam("page", page)
                            .queryParam("per_page", 100)
                            .build())
                    .retrieve()
                    .body(DiscogsSearchResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to search releases for query='{}' page={}: {}", query, page, e.getMessage());
            return null;
        }
    }

    public DiscogsReleaseDetail getReleaseById(long id) {
        try {
            return restClient.get()
                    .uri("/releases/{id}", id)
                    .retrieve()
                    .body(DiscogsReleaseDetail.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch release id={}: {}", id, e.getMessage());
            return null;
        }
    }

    public DiscogsMarketplaceStats getMarketplaceStats(long id) {
        try {
            return restClient.get()
                    .uri("/marketplace/stats/{id}", id)
                    .retrieve()
                    .body(DiscogsMarketplaceStats.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch marketplace stats for release id={}: {}", id, e.getMessage());
            return null;
        }
    }
}
