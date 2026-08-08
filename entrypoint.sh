#!/bin/sh
set -e

export PGDATA=/var/lib/postgresql/data

mkdir -p "$PGDATA"
chown -R postgres:postgres /var/lib/postgresql

su-exec postgres initdb -D "$PGDATA" >/tmp/initdb.log 2>&1
su-exec postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses=127.0.0.1 -c port=5432" -w start

su-exec postgres psql -U postgres -c "CREATE USER admin WITH SUPERUSER PASSWORD 'admin';"
su-exec postgres psql -U postgres -c "CREATE DATABASE urlshortener OWNER admin;"
su-exec postgres psql -U admin -d urlshortener -f /app/init.sql

redis-server --daemonize yes --bind 127.0.0.1

exec java -jar /app/app.jar
