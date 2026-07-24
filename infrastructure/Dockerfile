# The application image. Multi-stage: Gradle builds the boot jar, a JRE runs it.
#
# The build context is the REPOSITORY ROOT, not this directory — the build needs
# settings.gradle.kts, the version catalog, and all four modules. See compose.yaml.
#
#   docker build -f infrastructure/Dockerfile .

# ---------------------------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------------------------
# Pinned to the same Gradle the wrapper resolves (gradle/wrapper/gradle-wrapper.properties) and
# the same JDK the toolchain requires (build.gradle.kts: JavaLanguageVersion.of(25)). The image
# ships both, so nothing is downloaded to satisfy the toolchain. If you bump the wrapper, bump
# this tag with it.
FROM gradle:9.5.1-jdk25 AS build

# Root in the build stage only, so the cache mount below is writable regardless of which user
# the base image happens to default to.
USER root

# Gradle's home is redirected somewhere unambiguous so the cache mount lands on it. Without the
# cache, every build re-downloads the whole dependency graph.
ENV GRADLE_USER_HOME=/gradle-home

WORKDIR /workspace

# Build files first: these change rarely, so the dependency resolution below stays cached across
# ordinary source edits.
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

COPY domain/ domain/
COPY application/ application/
COPY infrastructure/ infrastructure/
COPY migrations/ migrations/

# `bootJar` does not run tests, and could not: the integration tests need a Docker daemon for
# Testcontainers, which is not available inside a build. `-x test` states that rather than
# relying on it.
RUN --mount=type=cache,target=/gradle-home \
    gradle --no-daemon :infrastructure:bootJar -x test

# Split the fat jar into layers that change at different rates, so a code-only change reships
# the application layer instead of every dependency. `jarmode=tools` is the Boot 3.3+ successor
# to `jarmode=layertools`.
RUN cp infrastructure/build/libs/infrastructure-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------------------------------------------------------------------------------------------
# Runtime
# ---------------------------------------------------------------------------------------------
# Alpine deliberately: nothing on the runtime classpath is native (BouncyCastle and the Postgres
# driver are pure Java), and busybox brings `wget`, which is what the container healthcheck uses.
# The Ubuntu-based tag has neither wget nor curl and would need an install layer for that alone.
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

# Ordered least-volatile first. Each COPY is its own layer.
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./

USER app

# Documentation only — neither Docker nor Kubernetes enforces this. 8080 serves the API under
# /v1; 8081 is the management context (health), which compose does not publish to the host.
EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "application.jar"]
