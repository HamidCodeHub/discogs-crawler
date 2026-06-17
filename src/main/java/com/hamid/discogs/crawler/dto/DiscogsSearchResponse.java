package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscogsSearchResponse {

    private PaginationDto pagination;
    private List<SearchResult> results;
}
