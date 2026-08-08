# URL Shortener

A Spring Boot URL shortener built as a hands-on exercise in scaling a simple CRUD service into a distributed-systems-shaped design: decentralized ID generation, non-enumerable short codes, a sharded database, a multi-node app layer behind a load balancer, and cache stampede protection.

## How it works

- Each registered URL gets a **Snowflake-style 64-bit ID** (`timestamp | machine ID | sequence`), minted independently by whichever app node handles the request — no shared counter, no central coordinator.
- That ID is **not** exposed directly. It's run through [Sqids](https://sqids.org/java) (`ShortCodeCodec`), which shuffles its encoding alphabet per value so short codes don't reveal registration order or let anyone walk consecutive codes to discover other users' links.
- URL mappings are **sharded** across two independent Postgres instances. Every read and write hashes the row's id (`ShardRouter`, a MurmurHash3 finalizer over the full 64-bit value — not the raw id, since its low bits are usually zero) and mods by shard count to pick a target. The routing function is pure and stateless, so every app node computes the same shard for the same id without talking to any other node.
- The app layer is **stateless and horizontally scaled** — two identical app nodes behind an nginx reverse proxy doing round-robin load balancing.
- Redirects are served via `302 Found`. Redis caches lookups (20-minute TTL) in front of the sharded DB, with `@Cacheable(sync = true)` collapsing concurrent same-node requests for a newly-expired hot key into a single DB hit instead of a thundering herd.
- There's no dedup — the same URL submitted twice yields two different short codes. Removing dedup was a deliberate trade-off: canonical-lookup dedup requires a cross-shard uniqueness check, which defeats the point of sharding by id.

## Tech Stack

- **Java 25** / Spring Boot 4
- **PostgreSQL 16** — sharded persistent storage (2 instances)
- **Redis 7** — lookup cache
- **nginx** — reverse proxy / load balancer across app nodes
- **Docker Compose** — local orchestration
- **Sqids** — non-enumerable short code encoding

## API

### Register a URL
```
POST /
Content-Type: application/json

{ "url": "https://www.example.com" }
```
Returns the short URL as a string, e.g. `http://localhost:8080/PKhZ1gjO6nM`.

### Redirect
```
GET /{code}
```
Returns `302 Found` with a `Location` header pointing to the original URL. Returns `404` if the code does not exist or `400` if the code is malformed.

### Frontend
```
GET /
```
Serves a minimal single-page form (paste a URL, get a short link back) — `src/main/resources/static/index.html`.

## Running locally

### Prerequisites
- Docker Desktop or OrbStack
- JDK 25+

### With Docker Compose (recommended)

```bash
./gradlew bootJar
docker compose up --build
```
or, equivalently:
```bash
./scripts/up.sh
```

This brings up nginx, both app nodes, both Postgres shards, and Redis. The app is available at `http://localhost:8080`.

To stop:
```bash
docker compose down
```

To stop and wipe the databases:
```bash
docker compose down -v
```

### Without Docker Compose

The app shards across two Postgres instances — start both, plus Redis. Each shard needs `init.sql` mounted so its schema exists before the app starts (`spring.jpa.hibernate.ddl-auto=validate` checks the schema but won't create it):
```bash
docker run -d --name postgres-shard0 -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=urlshortener -p 5432:5432 -v "$(pwd)/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" postgres:16.2
docker run -d --name postgres-shard1 -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=urlshortener -p 5433:5432 -v "$(pwd)/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" postgres:16.2
docker run -d --name redis-local -p 6379:6379 redis:7.2
```

DB credentials have no default committed (see `application.properties`). For local dev, create `src/main/resources/application-local.properties` (gitignored) with:
```properties
DB_USERNAME=admin
DB_PASSWORD=admin
```

Then activate that profile and run the app from IntelliJ (set `spring.profiles.active=local` as a VM/program arg or env var) or:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Scripts

- **`scripts/up.sh`** — `./gradlew bootJar && docker compose up --build` in one step.
- **`scripts/check-shards.sh`** — queries both Postgres shards directly and prints their `url_mappings` rows, to verify data is actually split across shards rather than landing on one.

## Sharding

URL mappings are partitioned across `app.sharding.shards[0..N-1]` (two shards by default, configured via `SHARD_0_URL`/`SHARD_1_URL` etc.). See "How it works" above for the routing mechanism.

To see how rows are actually distributed across shards:
```bash
./scripts/check-shards.sh
```

## Caching

Redis keys follow the pattern `urlCache::<shortCode>` with a TTL of 20 minutes. You can observe cache hits and misses in real time:
```bash
docker exec -it redis-local redis-cli monitor
```
A `GET` followed by a `SET` indicates a cache miss (DB was hit). A `GET` with no subsequent `SET` indicates a cache hit.

`lookupByShortCode` uses `@Cacheable(sync = true)` for cache stampede protection — concurrent requests for the same code on the same app node block on one DB lookup instead of each independently missing the cache. This only synchronizes within a single node; it doesn't prevent two different app nodes from independently missing the same key at the same instant, which would need a distributed lock in Redis to fully close.

## Internet Deployment

The `deploy` branch runs everything — Postgres, Redis, and the app — as three processes inside **one container**, for the simplest possible free-tier hosting, alongside the `main` branch's local multi-node/sharded demo setup.

**What's different on `deploy`:**
- `Dockerfile` installs `postgresql16` and `redis` alongside the JDK; `entrypoint.sh` initializes a fresh Postgres data directory, creates the `admin`/`urlshortener` DB, applies `init.sql`, starts Redis, then starts the app — all on every container start, since free-tier compute has no persistent disk anyway (data doesn't survive a cold-start regardless of where it lives, so there's no downside to keeping it in-container).
- Postgres and Redis are both bound to `127.0.0.1` only — never reachable from outside the container, only from the app process alongside them.
- `server.port` reads Render's injected `PORT` env var.
- No external database or cache service — `SHARD_0_URL`/Redis host both default to `localhost` already in `application.properties`, matching this setup with zero extra config.

**Setup** (free, no credit card):

**App — [Render](https://render.com)**: New → Blueprint → connect this GitHub repo → select the **`deploy`** branch (not `main`) → Render reads `render.yaml` and deploys. No manual env var entry needed — `render.yaml` already sets the (non-secret, container-internal-only) DB credentials.

**Known trade-off of staying on free tiers:** Render's free web service sleeps after 15 minutes idle, and since the database lives in the same container, a cold-start wipes it — every wake-up is a fresh, empty database. Fine for a resume-link demo where nobody's relying on old links surviving; not fine for anything real.

## Future Improvements

Deliberately left undone, with reasoning:

- **Read/write path separation** (DB read replicas) — deprioritized. Its value is conditional on traffic shape this project can't observe: Redis already absorbs read load for hot/viral keys within the TTL window, so replicas would mainly help a long-tail-of-rarely-repeated-codes access pattern rather than the hot-link case actually optimized for here.
- **Load testing with real numbers** — benchmark single-node vs. multi-node setups (k6/JMeter/wrk) and publish actual latency/throughput results, instead of just describing the architecture.
- **A production concern beyond the happy path** — either rate limiting/abuse prevention on `POST /`, or async click analytics (Kafka/SQS + consumer) so tracking redirects doesn't add latency to the hot read path.
