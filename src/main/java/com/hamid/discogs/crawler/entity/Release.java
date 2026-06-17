package com.hamid.discogs.crawler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "releases")
@Getter
@Setter
@NoArgsConstructor
public class Release {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    private String country;

    private String released;

    @ElementCollection
    @CollectionTable(name = "release_genres", joinColumns = @JoinColumn(name = "release_id"))
    @Column(name = "genre")
    private List<String> genres = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "release_styles", joinColumns = @JoinColumn(name = "release_id"))
    @Column(name = "style")
    private List<String> styles = new ArrayList<>();

    private Long masterId;

    @Column(nullable = false, updatable = false)
    private Instant crawledAt = Instant.now();
}
