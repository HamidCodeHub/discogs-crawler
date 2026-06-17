package com.hamid.discogs.crawler.service;

import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
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

            for (var item : response.getResults()) {
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

                releaseRepository.save(toEntity(detail));
                log.info("Saved release {} - {}", detail.getId(), detail.getTitle());
            }

            page++;
        } while (page <= totalPages && !Thread.currentThread().isInterrupted());

        log.info("Crawl finished for query='{}', processed {} page(s)", query, page - 1);
    }

    private ReleaseEntity toEntity(DiscogsReleaseDetail detail) {
        ReleaseEntity entity = new ReleaseEntity();
        entity.setDiscogsId(detail.getId());
        entity.setTitle(detail.getTitle());
        entity.setArtistsSort(detail.getArtistsSort());
        entity.setYear(detail.getYear() > 0 ? detail.getYear() : null);
        entity.setCountry(detail.getCountry());
        entity.setGenres(joinList(detail.getGenres()));
        entity.setNotes(detail.getNotes());
        entity.setFetchedAt(LocalDateTime.now());
        return entity;
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
