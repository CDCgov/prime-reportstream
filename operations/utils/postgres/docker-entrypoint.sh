#!/bin/bash
set -e

# Initialize database if it does not exist
if [ ! -s "$PGDATA/PG_VERSION" ]; then
    echo "Initializing PostgreSQL database..."
    initdb --username=postgres --pwfile=<(echo "$POSTGRES_PASSWORD") -D "$PGDATA"
    
    # Configure PostgreSQL
    echo "listen_addresses = '*'" >> "$PGDATA/postgresql.conf"
    echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
    echo "host all all ::1/128 md5" >> "$PGDATA/pg_hba.conf"
    
    # Start PostgreSQL temporarily to create user and database
    pg_ctl -D "$PGDATA" -o "-c listen_addresses='localhost'" -w start
    
    # Create user and database if specified
    if [ "$POSTGRES_USER" ] && [ "$POSTGRES_USER" != "postgres" ]; then
        psql --username postgres -c "CREATE USER \"$POSTGRES_USER\" WITH PASSWORD '$POSTGRES_PASSWORD';"
        psql --username postgres -c "ALTER USER \"$POSTGRES_USER\" WITH SUPERUSER;"
    fi
    
    if [ "$POSTGRES_DB" ] && [ "$POSTGRES_DB" != "postgres" ]; then
        psql --username postgres -c "CREATE DATABASE \"$POSTGRES_DB\" OWNER \"${POSTGRES_USER:-postgres}\";"
    fi
    
    # Stop temporary instance
    pg_ctl -D "${PGDATA}" -m fast -w stop
fi

# Start PostgreSQL
exec postgres -D "${PGDATA}"