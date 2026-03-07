#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE user_service;
    CREATE DATABASE event_service;
    CREATE DATABASE request_service;
    CREATE DATABASE category_service;

    GRANT ALL PRIVILEGES ON DATABASE user_service TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE event_service TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE request_service TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE comment_service TO "$POSTGRES_USER";
EOSQL

echo "All databases created successfully"