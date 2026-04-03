# URL Shortener

A Spring Boot URL shortener service backed by PostgreSQL and Redis.

## How it works

- Submitting a URL generates a short code derived from Base62 encoding of the auto-incremented database ID
- Redirects are served via HTTP 302 with a `Location` header
- Redis caches lookups with a 10-minute TTL to avoid repeated DB hits
- Duplicate URLs return the same short code
- Concurrent duplicate submissions are handled via a unique constraint on `original_url` with graceful fallback

## Tech Stack

- **Java 21** / Spring Boot 4
- **PostgreSQL 16** — persistent storage
- **Redis 7** — lookup cache
- **Docker Compose** — local orchestration

## API

### Register a URL
```
POST /
Content-Type: application/json

{ "url": "https://www.example.com" }
```
Returns the short URL as a string, e.g. `http://localhost:8080/3f`.

### Redirect
```
GET /{code}
```
Returns `302 Found` with a `Location` header pointing to the original URL. Returns `404` if the code does not exist or `400` if the code contains invalid characters.

## Running locally

### Prerequisites
- Docker Desktop or OrbStack
- JDK 21+

### With Docker Compose (recommended)

```bash
./gradlew bootJar
docker compose up --build
```

All three services (app, Postgres, Redis) start together. The app is available at `http://localhost:8080`.

To stop:
```bash
docker compose down
```

To stop and wipe the database:
```bash
docker compose down -v
```

### Without Docker Compose

Start Postgres and Redis manually:
```bash
docker run -d --name postgres-local -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=urlshortener -p 5432:5432 postgres:16.2
docker run -d --name redis-local -p 6379:6379 redis:7.2
```

Then run the app from IntelliJ or:
```bash
./gradlew bootRun
```

## Caching

Redis keys follow the pattern `urlCache::<shortCode>` with a TTL of 10 minutes. You can observe cache hits and misses in real time:

```bash
docker exec -it redis-local redis-cli monitor
```

A `GET` followed by a `SET` indicates a cache miss (DB was hit). A `GET` with no subsequent `SET` indicates a cache hit.
