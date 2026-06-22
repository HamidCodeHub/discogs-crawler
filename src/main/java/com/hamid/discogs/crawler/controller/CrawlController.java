package com.hamid.discogs.crawler.controller;

import com.hamid.discogs.crawler.dto.CrawlJobResponse;
import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import com.hamid.discogs.crawler.repository.CrawlJobRepository;
import com.hamid.discogs.crawler.service.DiscogsCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Tag(name = "Crawl", description = "Start, monitor, and stop crawl jobs")
public class CrawlController {

    private final DiscogsCrawlerService crawlerService;
    private final CrawlJobRepository crawlJobRepository;

    @Operation(
        summary = "Start or resume a crawl",
        description = "Resumes the last incomplete job for the query unless restart=true, in which case a fresh job is created."
    )
    @PostMapping
    public ResponseEntity<CrawlJobResponse> start(
            @RequestParam String query,
            @RequestParam(defaultValue = "false") boolean restart) {

        CrawlJob job = null;
        if (!restart) {
            job = crawlJobRepository
                    .findTopByQueryAndStatusInOrderByStartedAtDesc(
                            query, List.of(CrawlStatus.RUNNING, CrawlStatus.FAILED, CrawlStatus.STOPPED))
                    .orElse(null);
        }

        if (job == null) {
            job = new CrawlJob();
            job.setQuery(query);
        }
        job.setStatus(CrawlStatus.PENDING);
        job = crawlJobRepository.save(job);

        crawlerService.crawl(job);
        return ResponseEntity.accepted().body(CrawlJobResponse.from(job));
    }

    @Operation(summary = "List all crawl jobs")
    @GetMapping
    public List<CrawlJobResponse> list() {
        return crawlJobRepository.findAllByOrderByStartedAtDesc().stream()
                .map(CrawlJobResponse::from)
                .toList();
    }

    @Operation(summary = "Get crawl job status")
    @GetMapping("/{id}")
    public ResponseEntity<CrawlJobResponse> get(@PathVariable Long id) {
        return crawlJobRepository.findById(id)
                .<ResponseEntity<CrawlJobResponse>>map(j -> ResponseEntity.ok(CrawlJobResponse.from(j)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Stop a running crawl job")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> stop(@PathVariable Long id) {
        CrawlJob job = crawlJobRepository.findById(id).orElse(null);
        if (job == null || (job.getStatus() != CrawlStatus.RUNNING && job.getStatus() != CrawlStatus.PENDING)) {
            return ResponseEntity.notFound().build();
        }
        job.setStatus(CrawlStatus.STOPPED);
        crawlJobRepository.save(job);
        return ResponseEntity.noContent().build();
    }
}