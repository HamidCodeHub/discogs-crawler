package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResultItemDto(
        long id,
        String title,
        String type,
        @JsonProperty("resource_url") String resourceUrl
) {}
