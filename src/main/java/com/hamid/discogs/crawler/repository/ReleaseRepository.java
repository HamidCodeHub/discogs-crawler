package com.hamid.discogs.crawler.repository;

import com.hamid.discogs.crawler.entity.ReleaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, Long> {

    boolean existsByDiscogsId(Long id);
}
