package com.hamid.discogs.crawler.service;

import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.ReleaseDto;
import com.hamid.discogs.crawler.dto.SearchResultDto;
import com.hamid.discogs.crawler.entity.Artist;
import com.hamid.discogs.crawler.entity.Release;
import com.hamid.discogs.crawler.repository.ArtistRepository;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final DiscogsApiClient apiClient;
    private final ReleaseRepository releaseRepository;
    private final ArtistRepository artistRepository;

    @Transactional
    public void crawlRelease(long releaseId) {
        if (releaseRepository.existsByIdAndCrawledAtIsNotNull(releaseId)) {
            log.debug("Release {} already crawled, skipping", releaseId);
            return;
        }
        ReleaseDto dto = apiClient.getRelease(releaseId);
        Release release = mapToEntity(dto);
        releaseRepository.save(release);
        dto.artists().forEach(a -> artistRepository.findById(a.id()).orElseGet(() -> {
            Artist artist = new Artist();
            artist.setId(a.id());
            artist.setName(a.name());
            return artistRepository.save(artist);
        }));
        log.info("Crawled release {}: {}", releaseId, dto.title());
    }

    public void searchAndCrawl(String query, int maxPages) {
        for (int page = 1; page <= maxPages; page++) {
            SearchResultDto result = apiClient.search(query, page, 50);
            result.results().forEach(item -> {
                if ("release".equals(item.type())) {
                    crawlRelease(item.id());
                }
            });
            if (page >= result.pagination().pages()) break;
        }
    }

    private Release mapToEntity(ReleaseDto dto) {
        Release r = new Release();
        r.setId(dto.id());
        r.setTitle(dto.title());
        r.setCountry(dto.country());
        r.setReleased(dto.released());
        r.setMasterId(dto.masterId());
        if (dto.genres() != null) r.setGenres(dto.genres());
        if (dto.styles() != null) r.setStyles(dto.styles());
        return r;
    }
}
