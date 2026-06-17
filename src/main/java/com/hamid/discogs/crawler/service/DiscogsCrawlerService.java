package com.hamid.discogs.crawler.service;

import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
import com.hamid.discogs.crawler.dto.SearchResult;
import com.hamid.discogs.crawler.entity.ReleaseEntity;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscogsCrawlerService {

    private final DiscogsApiClient apiClient;
    private final ReleaseRepository releaseRepository;

    public void crawl(String query) {
        log.info("Starting crawl for query='{}'", query);
        int page = 1;
        int totalPages = 1;

        do {
            log.debug("Fetching search results page {}/{} for query='{}'", page, totalPages, query);
            DiscogsSearchResponse response = apiClient.searchReleases(query, page);

            if (response == null || response.getResults() == null) {
                log.warn("Empty response for query='{}' page={}, stopping", query, page);
                break;
            }

            sleep();

            if (response.getPagination() != null) {
                totalPages = response.getPagination().pages();
            }

            for (SearchResult item : response.getResults()) {
                if (releaseRepository.existsByDiscogsId(item.getId())) {
                    log.debug("Release {} already stored, skipping", item.getId());
                    continue;
                }

                DiscogsReleaseDetail detail = apiClient.getReleaseById(item.getId());
                sleep();

                if (detail == null) {
                    log.warn("Could not fetch detail for release {}, skipping", item.getId());
                    continue;
                }

                releaseRepository.save(toEntity(item, detail));
                log.info("Saved release {} - {}", item.getId(), item.getTitle());
            }

            page++;
        } while (page <= totalPages && !Thread.currentThread().isInterrupted());

        log.info("Crawl finished for query='{}', processed {} page(s)", query, page - 1);
    }

    private ReleaseEntity toEntity(SearchResult item, DiscogsReleaseDetail detail) {
        ReleaseEntity entity = new ReleaseEntity();

        // From search result
        entity.setDiscogsId(item.getId());
        entity.setTitle(item.getTitle());
        entity.setReleaseYear(parseYear(item.getYear()));
        entity.setCountry(item.getCountry());
        entity.setType(item.getType());
        entity.setUri(item.getUri());
        entity.setCatno(item.getCatno());
        entity.setThumb(item.getThumb());
        entity.setCoverImage(item.getCoverImage());
        entity.setResourceUrl(item.getResourceUrl());
        entity.setMasterId(item.getMasterId());
        entity.setMasterUrl(item.getMasterUrl());
        entity.setFormatQuantity(item.getFormatQuantity());
        entity.setGenres(joinList(item.getGenre()));
        entity.setStyles(joinList(item.getStyle()));
        entity.setFormats(joinList(item.getFormat()));
        entity.setLabels(joinList(item.getLabel()));
        entity.setBarcodes(joinList(item.getBarcode()));

        if (item.getUserData() != null) {
            entity.setInWantlist(item.getUserData().isInWantlist());
            entity.setInCollection(item.getUserData().isInCollection());
        }
        if (item.getCommunity() != null) {
            entity.setCommunityWant(item.getCommunity().getWant());
            entity.setCommunityHave(item.getCommunity().getHave());
        }

        // From detail endpoint
        entity.setArtistsSort(detail.getArtistsSort());
        entity.setNotes(detail.getNotes());

        entity.setFetchedAt(LocalDateTime.now());
        return entity;
    }

    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) return null;
        try {
            int y = Integer.parseInt(year.trim());
            return y > 0 ? y : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String joinList(List<String> list) {
        return (list == null || list.isEmpty()) ? null : String.join(", ", list);
    }

    private void sleep() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Crawl interrupted during rate-limit sleep");
        }
    }
}
