package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtistDto(
        long id,
        String name,
        String role
) {}
