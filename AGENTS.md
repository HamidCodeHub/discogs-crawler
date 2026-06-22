# AGENTS.md - Discogs Crawler AI Agent Guide

## Quick Start
- **Java 21** + **Spring Boot 4.1.0** (see `pom.xml`)
- **PostgreSQL** required; configured in `application.yml`
- Build/test: `./mvnw clean test` or `./mvnw spring-boot:run`
- Application starts with `@EnableScheduling`; crawl occurs on fixed delay or cron

## Architecture Overview

### Data Flow: Scheduling → Crawling → Persistence
```
DiscogsScheduler (@Scheduled on fixedDelayString)
  └─→ DiscogsCrawlerService.crawl(query: String)
      └─→ DiscogsApiClient.searchReleases(query, page)
      └─→ For each new release:
          └─→ DiscogsApiClient.getReleaseById(id)
          └─→ toEntity(SearchResult + DiscogsReleaseDetail)
          └─→ ReleaseRepository.save(ReleaseEntity)
```

### Component Responsibilities

| Component | Location | Role |
|-----------|----------|------|
| **DiscogsScheduler** | `scheduler/` | Entry point; triggers crawl on `fixedDelayString` (default 3600000ms) via `@Value("${discogs.crawl.query:electronic}")` |
| **DiscogsCrawlerService** | `service/DiscogsCrawlerService.java` | Pagination loop, deduplication (skip if `releaseRepository.existsByDiscogsId()`), mapping, rate limiting (`Thread.sleep(1100ms)`) |
| **DiscogsApiClient** | `client/DiscogsApiClient.java` | Wraps RestClient; calls `/database/search` and `/releases/{id}` endpoints; returns DTOs or null on failure |
| **DTOs** | `dto/` | `SearchResult`, `DiscogsReleaseDetail`, `DiscogsSearchResponse` all use `@JsonIgnoreProperties(ignoreUnknown = true)` for API resilience |
| **ReleaseEntity** | `entity/ReleaseEntity.java` | JPA entity; `discogsId` is `@Id` (primary key); many `columnDefinition = "TEXT"` for large fields |
| **DiscogsProperties** | `config/DiscogsProperties.java` | Java record with compact constructor defaults (`baseUrl`, `token`, `requestsPerMinute`, `Crawl.delay`) |
| **DiscogsConfig** | `config/DiscogsConfig.java` | Provides `@Bean RestClient discogsRestClient` configured with auth header & user-agent |

## Critical Patterns & Conventions

### 1. **Configuration Management**
- Uses `@ConfigurationProperties(prefix = "discogs")` Java record at `DiscogsProperties`
- Compact constructor validates inputs (e.g., `if (delay <= 0) delay = 1000`)
- Application config in `application.yml`: API token, base URL, request limits, crawl delay/cron
- **Pattern**: Always validate external config at bean creation time, not at usage time

### 2. **API Client Error Handling**
- `DiscogsApiClient` catches `RestClientException` and **returns null** (no exceptions thrown)
- Caller (`DiscogsCrawlerService`) checks for null and logs a warning, then continues
- **Pattern**: Silent failures are intentional; null signals "skip this item" downstream

### 3. **Deduplication & Idempotency**
- Before fetching release detail, check `releaseRepository.existsByDiscogsId(item.getId())`
- Prevents redundant API calls and duplicate DB inserts
- Crawling is safe to re-run; already-persisted releases are skipped

### 4. **DTO-to-Entity Mapping with Type Coercion**
- `toEntity()` merges `SearchResult` (search endpoint) + `DiscogsReleaseDetail` (detail endpoint) into single `ReleaseEntity`
- Lists (genre, style, format, label, barcode) are **joined to comma-separated strings** via `joinList()`
- Year parsing: tries `Integer.parseInt()`; returns null if invalid (see `parseYear()`)
- Nested objects (`UserData`, `Community`) are flattened into scalar fields
- **Pattern**: All data converges to the single entity shape; no sub-tables for arrays

### 5. **Rate Limiting**
- `sleep()` method sleeps 1100ms between API calls (arbitrary margin above 1000ms for 60 req/min limit)
- Catches `InterruptedException` and re-interrupts thread to signal cancellation
- Placed after both `searchReleases()` and `getReleaseById()` calls

### 6. **Pagination & Interruption**
- `do-while (page <= totalPages && !Thread.currentThread().isInterrupted())`
- Respects interruption signals (e.g., graceful shutdown) to break out of loop

### 7. **Lombok Conventions**
- Entities use `@Getter @Setter @NoArgsConstructor`
- Services use `@RequiredArgsConstructor` for final field constructor injection
- All use `@Slf4j` for logging

### 8. **Testing**
- Uses `MockitoExtension` + `@Mock` / `@InjectMocks` (not Spring test context unless integration test)
- Test in `DiscogsCrawlerServiceTest.java` verifies:
  - Existing releases are skipped (no detail fetch)
  - New releases are fetched, mapped, and saved
  - Failed detail fetches skip the release
  - Uses `ArgumentCaptor` to inspect saved entity state

## Key Files to Know

- `pom.xml` - Maven config; note `spring-boot-starter-data-jpa`, `postgresql`, `springdoc-openapi-starter-webmvc-ui` (Swagger)
- `application.yml` - DB connection, Discogs API token, crawl delay
- `DiscogsProperties.java` - Typed configuration with defaults & validation
- `DiscogsApiClient.java` - Only file that talks to Discogs API
- `DiscogsCrawlerService.java` - Heart of the logic; pagination, dedup, mapping, rate limiting
- `ReleaseEntity.java` - Database schema shape; note `discogsId` as PK
- `DiscogsCrawlerServiceTest.java` - Shows testing patterns (mocking, ArgumentCaptor, null checks)

## Development Workflows

### Local Setup
1. Start PostgreSQL (e.g., Docker: `docker run -e POSTGRES_DB=discogs -p 5432:5432 postgres:16`)
2. Set `discogs.token` in `application.yml` or env var
3. Run: `./mvnw spring-boot:run`
4. Scheduler kicks off; monitor logs for crawl progress

### Testing
```bash
./mvnw test                                    # Run all tests
./mvnw test -Dtest=DiscogsCrawlerServiceTest  # Single test class
```

### Build & Package
```bash
./mvnw clean package  # Creates target/discogs-crawler-0.0.1-SNAPSHOT.jar
```

## Common Tasks for AI Agents

- **Adding a new crawl field**: Update both DTO (add `@JsonProperty` if needed), entity, and `toEntity()` mapping
- **Changing rate limit or cron**: Edit `application.yml` or add env var override
- **Handling new API errors**: Wrap in try-catch in `DiscogsApiClient`, return null, log
- **Optimizing deduplication**: Consider adding indices on `discogsId` (already PK) or `title` in `ReleaseEntity`
- **Debugging a failed crawl**: Check logs for API errors (DiscogsApiClient), mapping errors (toEntity), or DB constraints (ReleaseEntity)

## Gotchas
- RestClient auth header is set globally in `DiscogsConfig.discogsRestClient()` — all requests auto-include Discogs token
- Null-checking is defensive; most API failures silently skip the item rather than throwing
- `columnDefinition = "TEXT"` on many fields to avoid VARCHAR length limits in PostgreSQL
- No transaction boundaries visible; relies on Spring's implicit TX per `save()` call

