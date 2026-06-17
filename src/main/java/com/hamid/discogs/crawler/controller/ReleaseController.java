package com.hamid.discogs.crawler.controller;

import com.hamid.discogs.crawler.dto.ReleaseResponse;
import com.hamid.discogs.crawler.repository.ReleaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@Tag(name = "Releases", description = "Query releases crawled from Discogs")
public class ReleaseController {

    private final ReleaseRepository releaseRepository;

    @Operation(summary = "List all releases", description = "Returns a paginated list of all stored releases. Use ?page=0&size=20&sort=releaseYear,desc to control ordering.")
    @GetMapping
    public Page<ReleaseResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return releaseRepository.findAll(pageable).map(ReleaseResponse::from);
    }

    @Operation(summary = "Get release by Discogs ID")
    @GetMapping("/{id}")
    public ResponseEntity<ReleaseResponse> getById(
            @Parameter(description = "Discogs release ID") @PathVariable Long id) {
        return releaseRepository.findById(id)
                .map(ReleaseResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search releases", description = "Case-insensitive search across title and artist name.")
    @GetMapping("/search")
    public Page<ReleaseResponse> search(
            @Parameter(description = "Search term matched against title and artist") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return releaseRepository.search(q, pageable).map(ReleaseResponse::from);
    }

    @Operation(summary = "Filter releases", description = "Filter by genre, release year, or country. Only the first provided parameter is applied.")
    @GetMapping("/filter")
    public Page<ReleaseResponse> filter(
            @Parameter(description = "Genre substring (e.g. Jazz)") @RequestParam(required = false) String genre,
            @Parameter(description = "Exact release year (e.g. 1975)") @RequestParam(required = false) Integer year,
            @Parameter(description = "Country code or name (e.g. US)") @RequestParam(required = false) String country,
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
