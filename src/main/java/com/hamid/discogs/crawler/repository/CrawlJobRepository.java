package com.hamid.discogs.crawler.repository;

import com.hamid.discogs.crawler.entity.CrawlJob;
import com.hamid.discogs.crawler.entity.CrawlStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {

    Optional<CrawlJob> findTopByQueryAndStatusInOrderByStartedAtDesc(String query, List<CrawlStatus> statuses);

    boolean existsByStatus(CrawlStatus status);

    List<CrawlJob> findAllByOrderByStartedAtDesc();

    @Modifying
    @Query("UPDATE CrawlJob j SET j.status = com.hamid.discogs.crawler.entity.CrawlStatus.FAILED WHERE j.status = com.hamid.discogs.crawler.entity.CrawlStatus.RUNNING")
    int markAllRunningAsFailed();
}