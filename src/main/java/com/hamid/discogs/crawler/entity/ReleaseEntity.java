package com.hamid.discogs.crawler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "releases")
public class ReleaseEntity {

    @Id
    private Long discogsId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String artistsSort;

    @Column(name = "release_year")
    private Integer releaseYear;

    private String country;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "TEXT")
    private String catno;

    @Column(columnDefinition = "TEXT")
    private String thumb;

    @Column(columnDefinition = "TEXT")
    private String coverImage;

    @Column(columnDefinition = "TEXT")
    private String resourceUrl;

    private Long masterId;

    @Column(columnDefinition = "TEXT")
    private String masterUrl;

    private Integer formatQuantity;

    @Column(columnDefinition = "TEXT")
    private String genres;

    @Column(columnDefinition = "TEXT")
    private String styles;

    @Column(columnDefinition = "TEXT")
    private String formats;

    @Column(columnDefinition = "TEXT")
    private String labels;

    @Column(columnDefinition = "TEXT")
    private String barcodes;

    private Boolean inWantlist;

    private Boolean inCollection;

    private Integer communityWant;

    private Integer communityHave;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Double lowestPrice;
    private String priceCurrency;
    private Integer numForSale;

    private LocalDateTime fetchedAt;
}
