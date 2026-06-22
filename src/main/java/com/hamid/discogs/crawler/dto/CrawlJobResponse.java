package com.hamid.discogs.crawler.dto;

import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;

import java.time.LocalDateTime;

public record CrawlJobResponse(
        Long id,
        String query,
        CrawlStatus status,
        int currentPage,
        int totalPages,
        int savedCount,
        int skippedCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
    public static CrawlJobResponse from(CrawlJob job) {
        return new CrawlJobResponse(
                job.getId(), job.getQuery(), job.getStatus(),
                job.getCurrentPage(), job.getTotalPages(),
                job.getSavedCount(), job.getSkippedCount(),
                job.getStartedAt(), job.getCompletedAt(),
                job.getErrorMessage()
        );
    }
}