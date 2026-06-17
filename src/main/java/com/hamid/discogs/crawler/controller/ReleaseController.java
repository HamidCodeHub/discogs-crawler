package com.hamid.discogs.crawler.controller;

import com.hamid.discogs.crawler.dto.ReleaseResponse;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseRepository releaseRepository;

    @GetMapping
    public Page<ReleaseResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return releaseRepository.findAll(pageable).map(ReleaseResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReleaseResponse> getById(@PathVariable Long id) {
        return releaseRepository.findById(id)
                .map(ReleaseResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public Page<ReleaseResponse> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return releaseRepository.search(q, pageable).map(ReleaseResponse::from);
    }

    @GetMapping("/filter")
    public Page<ReleaseResponse> filter(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {

        if (genre != null) {
            return releaseRepository.findByGenresContainingIgnoreCase(genre, pageable).map(ReleaseResponse::from);
        }
        if (year != null) {
            return releaseRepository.findByReleaseYear(year, pageable).map(ReleaseResponse::from);
        }
        if (country != null) {
            return releaseRepository.findByCountryIgnoreCase(country, pageable).map(ReleaseResponse::from);
        }
        return releaseRepository.findAll(pageable).map(ReleaseResponse::from);
    }
}