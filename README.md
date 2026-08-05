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

## Roadmap / TODO

Planned extensions to move this from demo to production-grade design:

- [ ] **Decentralized ID generation** — replace single-DB auto-increment with Snowflake IDs or per-node range allocation, so multiple app nodes can mint IDs without a shared bottleneck
- [ ] **Non-enumerable short codes** — current Base62 encoding of a sequential ID lets anyone walk consecutive keys; fix with a reversible bit permutation (Feistel cipher) over the ID space before encoding
- [ ] **Sharded database** — partition URL mappings across multiple DB shards, keyed by short code hash or ID range
- [ ] **Multi-node app layer** — run stateless app instances behind a load balancer, enabled by decentralized ID generation
- [ ] **Read/write path separation** — read replicas or read-optimized storage, since redirects vastly outnumber registrations
- [ ] **Cache stampede protection** — guard against thundering-herd DB hits when a hot key expires from Redis
- [ ] **Kubernetes deployment** — once the above work manually (e.g. via Docker Compose + nginx), move to a Deployment/Service/StatefulSet setup as the final "how would you actually deploy this" layer
- [ ] **Minimal frontend** — a simple static page (paste URL → get short link) so the project has a live demo, not just a curl-able API
- [ ] **Local deployment demo** — run the full multi-node/sharded/K8s design locally (Docker Compose or `kind`/`minikube`) and document it with diagrams/a recording, since a real multi-node cluster isn't free to host live
- [ ] **Internet deployment** — deploy a simplified single-node version live for the resume link: frontend on GitHub Pages (static-only, free), backend on a free-tier host like Render or Fly.io (spins down on idle, cold-starts on request), with a free-tier Postgres (Render/Supabase) and free Redis (Upstash)

## Fix / Hardening TODO

- [ ] **Remove URL dedup** — `findByOriginalUrl` + unique constraint on `original_url` forces every write through a canonical lookup, which conflicts with sharding by id/hash; drop dedup so writes can be routed purely by shard key
- [ ] **`PostBody` validation** — public field with no setter and no validation annotations (e.g. `@NotBlank`); consider a `record` with `@Valid`/`@NotBlank` instead
- [ ] **`registerUrl` transactionality** — read-then-write isn't wrapped in `@Transactional`; the unique-constraint catch is the real concurrency guard today, but this needs revisiting once dedup is removed
- [ ] **Credentials in `application.properties`** — plaintext `admin`/`admin` committed; move to env vars/secrets (already done correctly in `docker-compose.yml`)
- [ ] **Indexing strategy** — beyond the implicit PK, decide what needs indexing once the dedup-driven unique constraint on `original_url` is gone
