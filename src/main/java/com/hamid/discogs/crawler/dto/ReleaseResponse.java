package com.hamid.discogs.crawler.dto;

import com.hamid.discogs.crawler.entity.ReleaseEntity;

import java.time.LocalDateTime;

public record ReleaseResponse(
        Long discogsId,
        String title,
        String artist,
        Integer releaseYear,
        String country,
        String genres,
        String notes,
        LocalDateTime fetchedAt
) {
    public static ReleaseResponse from(ReleaseEntity e) {
        return new ReleaseResponse(
                e.getDiscogsId(),
                e.getTitle(),
                e.getArtistsSort(),
                e.getReleaseYear(),
                e.getCountry(),
                e.getGenres(),
                e.getNotes(),
                e.getFetchedAt()
        );
    }
}