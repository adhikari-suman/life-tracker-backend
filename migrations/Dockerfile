# The migration job. Runs to completion before the app starts, and does nothing else.
#
# Build context is THIS directory:
#
#   docker build migrations/
#
# No Gradle and no JVM build: this module has no Java sources. The image is the official
# Liquibase distribution plus the changelogs, pinned to the exact version in the catalog
# (gradle/libs.versions.toml: liquibase = "4.33.0") so the container runs the same Liquibase the
# build does.
FROM liquibase/liquibase:4.33.0-alpine

# The changelogs are copied to the path they occupy in the repository, which looks redundant but
# is not. Liquibase stores the changelog path in DATABASECHANGELOG.FILENAME, and that path is
# part of a changeset's identity. `./gradlew :migrations:update` passes
# `src/main/resources/db/changelog/db.changelog-master.yaml`; if this image used a shorter path,
# the same changesets would carry a different FILENAME and Liquibase would apply all eleven a
# second time against a database that already has them. Keeping the paths identical makes the
# two routes interchangeable.
COPY --chown=liquibase:liquibase src/main/resources/db/changelog/ \
     /liquibase/src/main/resources/db/changelog/

ENV LIQUIBASE_SEARCH_PATH=/liquibase
ENV LIQUIBASE_COMMAND_CHANGELOG_FILE=src/main/resources/db/changelog/db.changelog-master.yaml

# Connection details arrive as LIQUIBASE_COMMAND_URL / _USERNAME / _PASSWORD from compose.
CMD ["update"]
