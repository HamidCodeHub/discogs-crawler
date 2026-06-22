package com.hamid.discogs.crawler.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscogsMarketplaceStats {

    @JsonProperty("lowest_price")
    private Price lowestPrice;

    @JsonProperty("num_for_sale")
    private Integer numForSale;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private String currency;
        private Double value;
    }
}
