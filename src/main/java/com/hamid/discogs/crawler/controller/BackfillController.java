package com.hamid.discogs.crawler.controller;

import com.hamid.discogs.crawler.service.BackfillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backfill")
@RequiredArgsConstructor
@Tag(name = "Backfill", description = "Backfill marketplace prices for existing releases")
public class BackfillController {

    private final BackfillService backfillService;

    @Operation(
        summary = "Start price backfill",
        description = "Fetches marketplace stats for all releases that have never had a price check. Uses the same rate limiter as the crawler."
    )
    @PostMapping
    public ResponseEntity<Map<String, String>> start() {
        if (backfillService.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "already running"));
        }
        backfillService.backfillPrices();
        return ResponseEntity.accepted()
                .body(Map.of("status", "started"));
    }

    @Operation(summary = "Check backfill status")
    @GetMapping
    public Map<String, Object> status() {
        return Map.of("running", backfillService.isRunning());
    }

    @Operation(summary = "Stop a running backfill")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> stop() {
        if (!backfillService.isRunning()) {
            return ResponseEntity.notFound().build();
        }
        backfillService.stop();
        return ResponseEntity.ok(Map.of("status", "stop requested"));
    }
}
