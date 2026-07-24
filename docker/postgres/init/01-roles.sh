#!/bin/sh
# Creates the two database roles the stack runs as.
#
# Postgres runs everything in /docker-entrypoint-initdb.d exactly once, against an empty data
# directory, as the superuser. Delete the db-data volume to make it run again.
#
# This is a .sh rather than a .sql because a .sql file cannot read environment variables, and the
# role names and passwords are defined once as YAML anchors in compose.yaml.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE ${MIGRATOR_USER} LOGIN PASSWORD '${MIGRATOR_PASSWORD}';
    CREATE ROLE ${APP_USER} LOGIN PASSWORD '${APP_PASSWORD}';

    -- The migrator owns the schema. It is the only role that may create, alter or drop, and the
    -- only one the Liquibase job connects as.
    ALTER SCHEMA public OWNER TO ${MIGRATOR_USER};

    -- The app may enter the schema and work with rows in it. Nothing more. Postgres 15+ already
    -- withholds CREATE on public from PUBLIC, but stating it means the intent survives a version
    -- bump and reads as deliberate rather than inherited.
    GRANT USAGE ON SCHEMA public TO ${APP_USER};
    REVOKE CREATE ON SCHEMA public FROM PUBLIC;
    REVOKE CREATE ON SCHEMA public FROM ${APP_USER};

    -- Grants are NOT written per-table in changesets (ADR-0016). Every table the migrator creates
    -- from here on grants DML to the app automatically, so a new changeset can never forget one.
    -- Note the scope: this binds tables created AFTER this runs, BY the migrator. An existing
    -- database would need a one-off catch-up GRANT as well.
    ALTER DEFAULT PRIVILEGES FOR ROLE ${MIGRATOR_USER} IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_USER};
EOSQL

echo "roles ${MIGRATOR_USER} (DDL) and ${APP_USER} (DML) created"
