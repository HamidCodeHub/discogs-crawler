package com.hamid.discogs.crawler.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hamid.discogs.crawler.client.DiscogsApiClient;
import com.hamid.discogs.crawler.dto.DiscogsReleaseDetail;
import com.hamid.discogs.crawler.dto.DiscogsSearchResponse;
import com.hamid.discogs.crawler.dto.PaginationDto;
import com.hamid.discogs.crawler.dto.SearchResult;
import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import com.hamid.discogs.crawler.entity.ReleaseEntity;
import com.hamid.discogs.crawler.repository.CrawlJobRepository;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscogsCrawlerServiceTest {

    @Mock private DiscogsApiClient apiClient;
    @Mock private ReleaseRepository releaseRepository;
    @Mock private CrawlJobRepository crawlJobRepository;
    @Mock private RateLimiter rateLimiter;
    @Mock private Executor detailFetchExecutor;

    private DiscogsCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        // run submitted tasks inline so the async detail fetches execute synchronously in tests
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(detailFetchExecutor).execute(any());

        crawlerService = new DiscogsCrawlerService(
                apiClient, releaseRepository, crawlJobRepository, rateLimiter, detailFetchExecutor);
    }

    @Test
    void crawl_skipsExistingReleaseAndSavesNewOne() {
        CrawlJob job = pendingJob("jazz");
        SearchResult existing = searchResult(1L, "Existing Album");
        SearchResult novel    = fullSearchResult(2L, "New Album");

        when(crawlJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(crawlJobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(apiClient.searchReleases("jazz", 1)).thenReturn(searchResponse(existing, novel));
        when(releaseRepository.existsByDiscogsId(1L)).thenReturn(true);
        when(releaseRepository.existsByDiscogsId(2L)).thenReturn(false);
        when(apiClient.getReleaseById(2L)).thenReturn(releaseDetail());

        crawlerService.crawl(job).join();

        verify(apiClient, never()).getReleaseById(1L);
        verify(apiClient).getReleaseById(2L);

        ArgumentCaptor<ReleaseEntity> captor = ArgumentCaptor.forClass(ReleaseEntity.class);
        // one save for the release entity (job saves are also counted, filter by type)
        verify(releaseRepository, times(1)).save(captor.capture());

        ReleaseEntity saved = captor.getValue();
        assertThat(saved.getDiscogsId()).isEqualTo(2L);
        assertThat(saved.getTitle()).isEqualTo("New Album");
        assertThat(saved.getArtistsSort()).isEqualTo("Artist, The");
        assertThat(saved.getReleaseYear()).isEqualTo(2023);
        assertThat(saved.getCountry()).isEqualTo("DE");
        assertThat(saved.getGenres()).isEqualTo("Electronic, Techno");
        assertThat(saved.getNotes()).isEqualTo("Repress");
        assertThat(saved.getFetchedAt()).isNotNull();
    }

    @Test
    void crawl_skipsReleaseWhenDetailFetchReturnsNull() {
        CrawlJob job = pendingJob("jazz");

        when(crawlJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(crawlJobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(apiClient.searchReleases("jazz", 1))
                .thenReturn(searchResponse(searchResult(3L, "Mystery Release")));
        when(releaseRepository.existsByDiscogsId(3L)).thenReturn(false);
        when(apiClient.getReleaseById(3L)).thenReturn(null);

        crawlerService.crawl(job).join();

        verify(releaseRepository, never()).save(any(ReleaseEntity.class));
    }

    // --- helpers ---

    private static CrawlJob pendingJob(String query) {
        CrawlJob job = new CrawlJob();
        job.setId(1L);
        job.setQuery(query);
        job.setStatus(CrawlStatus.PENDING);
        return job;
    }

    private static SearchResult searchResult(long id, String title) {
        SearchResult r = new SearchResult();
        r.setId(id);
        r.setTitle(title);
        return r;
    }

    private static SearchResult fullSearchResult(long id, String title) {
        SearchResult r = searchResult(id, title);
        r.setYear("2023");
        r.setCountry("DE");
        r.setGenre(List.of("Electronic", "Techno"));
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
        detail.setNotes("Repress");
        return detail;
    }
}
