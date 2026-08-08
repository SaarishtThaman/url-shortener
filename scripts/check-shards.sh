#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

for shard in postgres-shard0 postgres-shard1; do
  echo "=== $shard ==="
  docker compose exec -T "$shard" psql -U admin -d urlshortener -c "SELECT id, original_url FROM url_mappings ORDER BY id;"
  echo
done
