package com.hamid.discogs.crawler.client;

import com.hamid.discogs.crawler.dto.ReleaseDto;
import com.hamid.discogs.crawler.dto.SearchResultDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DiscogsApiClient {

    private final RestClient restClient;

    public DiscogsApiClient(RestClient discogsRestClient) {
        this.restClient = discogsRestClient;
    }

    public ReleaseDto getRelease(long releaseId) {
        return restClient.get()
                .uri("/releases/{id}", releaseId)
                .retrieve()
                .body(ReleaseDto.class);
    }

    public SearchResultDto search(String query, int page, int perPage) {
        return restClient.get()
                .uri(b -> b.path("/database/search")
                        .queryParam("q", query)
                        .queryParam("page", page)
                        .queryParam("per_page", perPage)
                        .build())
                .retrieve()
                .body(SearchResultDto.class);
    }
}
