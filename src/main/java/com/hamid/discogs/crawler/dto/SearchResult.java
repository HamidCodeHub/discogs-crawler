package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResult {

    private long id;
    private String title;
    private String year;
    private String country;
    private List<String> genre;
    private List<String> style;

    @JsonProperty("resource_url")
    private String resourceUrl;
}
