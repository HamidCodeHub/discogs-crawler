package com.hamid.discogs.crawler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "releases")
public class ReleaseEntity {

    @Id
    private Long discogsId;

    private String title;

    private String artistsSort;

    private Integer year;

    private String country;

    @Column(columnDefinition = "TEXT")
    private String genres;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime fetchedAt;
}
