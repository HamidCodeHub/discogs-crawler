package com.hamid.discogs.crawler.repository;

import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {

    Optional<CrawlJob> findTopByQueryAndStatusInOrderByStartedAtDesc(String query, List<CrawlStatus> statuses);

    boolean existsByStatus(CrawlStatus status);

    List<CrawlJob> findAllByOrderByStartedAtDesc();
}