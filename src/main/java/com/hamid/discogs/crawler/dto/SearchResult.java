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
    private String type;
    private String uri;
    private String catno;
    private String thumb;

    @JsonProperty("cover_image")
    private String coverImage;

    @JsonProperty("resource_url")
    private String resourceUrl;

    @JsonProperty("master_id")
    private Long masterId;

    @JsonProperty("master_url")
    private String masterUrl;

    @JsonProperty("format_quantity")
    private Integer formatQuantity;

    private List<String> genre;
    private List<String> style;
    private List<String> format;
    private List<String> label;
    private List<String> barcode;

    @JsonProperty("user_data")
    private UserData userData;

    private Community community;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        @JsonProperty("in_wantlist")
        private boolean inWantlist;

        @JsonProperty("in_collection")
        private boolean inCollection;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Community {
        private Integer want;
        private Integer have;
    }
}
