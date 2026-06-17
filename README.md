# Discogs Crawler

A Spring Boot application that periodically crawls the [Discogs](https://www.discogs.com/) API to collect and persist music release and artist data into a PostgreSQL database.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| HTTP Client | Spring `RestClient` |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Scheduling | Spring `@Scheduled` |
| Build | Maven |
| Utilities | Lombok |

## Package Structure

```
com.hamid.discogs.crawler
├── config       # DiscogsProperties, RestClient bean, @EnableScheduling
├── client       # DiscogsApiClient — wraps all Discogs REST API calls
├── dto          # Records for API response mapping (Release, Artist, Search)
├── entity       # JPA entities persisted to PostgreSQL (Release, Artist)
├── repository   # Spring Data JPA repositories
├── service      # CrawlerService — orchestrates fetch → map → persist
└── scheduler    # CrawlerScheduler — cron-triggered entry point
```

## Configuration

Add the following to `application.properties` (or use environment variables):

```properties
# Discogs API
discogs.base-url=https://api.discogs.com
discogs.token=YOUR_DISCOGS_PERSONAL_ACCESS_TOKEN
discogs.requests-per-minute=60

# Crawl schedule (default: every hour)
discogs.crawl.cron=0 0 * * * *

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/discogs
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

A Discogs personal access token can be obtained from your [Discogs developer settings](https://www.discogs.com/settings/developers).

## Getting Started

```bash
# Start PostgreSQL (example with Docker)
docker run -d --name discogs-pg \
  -e POSTGRES_DB=discogs \
  -e POSTGRES_USER=discogs \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:16

# Build and run
./mvnw spring-boot:run
```

## How It Works

1. `CrawlerScheduler` fires on the configured cron (default: top of every hour).
2. It delegates to `CrawlerService`, which calls `DiscogsApiClient` to search and fetch releases.
3. Each release is mapped from a DTO to a JPA entity and saved to PostgreSQL.
4. Artists referenced by a release are upserted into the `artists` table.
