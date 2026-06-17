package com.hamid.discogs.crawler.service;

import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
import com.hamid.discogs.crawler.dto.PaginationDto;
import com.hamid.discogs.crawler.dto.SearchResult;
import com.hamid.discogs.crawler.entity.ReleaseEntity;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscogsCrawlerServiceTest {

    @Mock
    private DiscogsApiClient apiClient;

    @Mock
    private ReleaseRepository releaseRepository;

    @InjectMocks
    private DiscogsCrawlerService crawlerService;

    @Test
    void crawl_skipsExistingReleaseAndSavesNewOne() {
        SearchResult existing = searchResult(1L, "Existing Album");
        SearchResult novel   = searchResult(2L, "New Album");

        when(apiClient.searchReleases("jazz", 1))
                .thenReturn(searchResponse(existing, novel));
        when(releaseRepository.existsByDiscogsId(1L)).thenReturn(true);
        when(releaseRepository.existsByDiscogsId(2L)).thenReturn(false);
        when(apiClient.getReleaseById(2L)).thenReturn(releaseDetail());

        crawlerService.crawl("jazz");

        // existing release must not trigger a detail fetch or a save
        verify(apiClient, never()).getReleaseById(1L);

        // new release must be fetched, mapped, and persisted
        verify(apiClient).getReleaseById(2L);

        ArgumentCaptor<ReleaseEntity> captor = ArgumentCaptor.forClass(ReleaseEntity.class);
        verify(releaseRepository, times(1)).save(captor.capture());

        ReleaseEntity saved = captor.getValue();
        assertThat(saved.getDiscogsId()).isEqualTo(2L);
        assertThat(saved.getTitle()).isEqualTo("New Album");
        assertThat(saved.getArtistsSort()).isEqualTo("Artist, The");
        assertThat(saved.getYear()).isEqualTo(2023);
        assertThat(saved.getCountry()).isEqualTo("DE");
        assertThat(saved.getGenres()).isEqualTo("Electronic, Techno");
        assertThat(saved.getNotes()).isEqualTo("Repress");
        assertThat(saved.getFetchedAt()).isNotNull();
    }

    @Test
    void crawl_skipsReleaseWhenDetailFetchReturnsNull() {
        when(apiClient.searchReleases("jazz", 1))
                .thenReturn(searchResponse(searchResult(3L, "Mystery Release")));
        when(releaseRepository.existsByDiscogsId(3L)).thenReturn(false);
        when(apiClient.getReleaseById(3L)).thenReturn(null);

        crawlerService.crawl("jazz");

        verify(releaseRepository, never()).save(any());
    }

    // --- helpers ---

    private static SearchResult searchResult(long id, String title) {
        SearchResult r = new SearchResult();
        r.setId(id);
        r.setTitle(title);
        return r;
    }

    private static DiscogsSearchResponse searchResponse(SearchResult... results) {
        PaginationDto pagination = new PaginationDto(1, 1, 50, results.length);
        DiscogsSearchResponse response = new DiscogsSearchResponse();
        response.setPagination(pagination);
        response.setResults(List.of(results));
        return response;
    }

    private static DiscogsReleaseDetail releaseDetail() {
        DiscogsReleaseDetail detail = new DiscogsReleaseDetail();
        detail.setId(2L);
        detail.setTitle("New Album");
        detail.setArtistsSort("Artist, The");
        detail.setYear(2023);
        detail.setCountry("DE");
        detail.setGenres(List.of("Electronic", "Techno"));
        detail.setNotes("Repress");
        return detail;
    }
}
