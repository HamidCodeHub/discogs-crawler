package com.hamid.discogs.crawler.repository;

import com.hamid.discogs.crawler.entity.ReleaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, Long> {

    boolean existsByDiscogsId(Long id);

    @Query("SELECT r FROM ReleaseEntity r WHERE " +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(r.artistsSort) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<ReleaseEntity> search(@Param("q") String q, Pageable pageable);

    Page<ReleaseEntity> findByGenresContainingIgnoreCase(String genre, Pageable pageable);

    Page<ReleaseEntity> findByReleaseYear(Integer releaseYear, Pageable pageable);

    Page<ReleaseEntity> findByCountryIgnoreCase(String country, Pageable pageable);
}
