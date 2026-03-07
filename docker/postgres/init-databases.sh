#!/bin/bash
set -e

echo "Creating databases..."

# Подключаемся к postgres и создаем базы
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE userdb;
    CREATE DATABASE eventdb;
    CREATE DATABASE requestdb;
    CREATE DATABASE categorydb;

    -- права
    GRANT ALL PRIVILEGES ON DATABASE userdb TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE eventdb TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE requestdb TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE categorydb TO "$POSTGRES_USER";
EOSQL

echo "All databases created successfully"