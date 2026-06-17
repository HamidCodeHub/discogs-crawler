package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseDto(
        long id,
        String title,
        String country,
        String released,
        List<ArtistDto> artists,
        List<String> genres,
        List<String> styles,
        @JsonProperty("master_id") Long masterId
) {}
