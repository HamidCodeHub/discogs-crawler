package com.hamid.discogs.crawler.repository;

import com.hamid.discogs.crawler.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findByCountry(String country);

    boolean existsByIdAndCrawledAtIsNotNull(Long id);
}
