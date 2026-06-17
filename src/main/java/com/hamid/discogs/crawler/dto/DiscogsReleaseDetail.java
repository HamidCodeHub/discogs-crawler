package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscogsReleaseDetail {

    private long id;
    private String title;
    private int year;
    private String country;
    private List<String> genres;
    private List<String> styles;
    private String notes;

    @JsonProperty("artists_sort")
    private String artistsSort;
}
