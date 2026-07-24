package com.lifetracker.infrastructure;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;

/**
 * The test database, running the same two-role split the deployed stack does: Liquibase connects as
 * {@code lifetracker_migrator} (the only role with DDL), and the application connects as
 * {@code lifetracker_app} (SELECT/INSERT/UPDATE/DELETE and nothing more).
 * <p>
 * This matters because a superuser can never fail a permission check. Connecting the tests as one
 * meant an entity needing a privilege the app role lacks would pass every test and fail in
 * production — the exact class of bug {@code ddl-auto: validate} exists to catch one level down.
 * <p>
 * The roles are created by the SAME script the compose stack uses, mounted rather than copied, so
 * the tests prove that script and not a divergent second copy of it. There is no
 * {@code @ServiceConnection}: it derives credentials from the container's superuser, which is
 * precisely what we are trying not to use. The two {@code ConnectionDetails} beans below replace
 * it, and being plain beans they also serve {@link TestLifeTrackerBackendApplication}.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	// Distinct from the compose values: a different database, and nothing should read across.
	private static final String MIGRATOR_USER = "lifetracker_migrator";
	private static final String MIGRATOR_PASSWORD = "migrator-test-only";
	private static final String APP_USER = "lifetracker_app";
	private static final String APP_PASSWORD = "app-test-only";

	// Relative to the module directory, which is Gradle's working directory for the test task.
	// Same reach across modules as the changelog resources in infrastructure/build.gradle.kts.
	private static final Path ROLES_SCRIPT = Path.of("..", "docker", "postgres", "init", "01-roles.sh");

	// Pinned, and pinned to the same tag as compose.yaml. `latest` meant the tests proved the
	// schema against whichever Postgres happened to be pulled, and against a different one than
	// the local stack ran — drift of exactly the kind `ddl-auto: validate` exists to catch.
	@Bean
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
				.withCopyFileToContainer(
						MountableFile.forHostPath(ROLES_SCRIPT, 0755),
						"/docker-entrypoint-initdb.d/01-roles.sh")
				.withEnv("MIGRATOR_USER", MIGRATOR_USER)
				.withEnv("MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
				.withEnv("APP_USER", APP_USER)
				.withEnv("APP_PASSWORD", APP_PASSWORD);
	}

	/** The application's connection: DML only. It cannot CREATE or ALTER, and the tests rely on that. */
	@Bean
	JdbcConnectionDetails jdbcConnectionDetails(PostgreSQLContainer container) {
		return new JdbcConnectionDetails() {
			@Override
			public String getUsername() {
				return APP_USER;
			}

			@Override
			public String getPassword() {
				return APP_PASSWORD;
			}

			@Override
			public String getJdbcUrl() {
				return container.getJdbcUrl();
			}
		};
	}

	/** Liquibase's connection: the schema owner, and the only role that may write DDL. */
	@Bean
	LiquibaseConnectionDetails liquibaseConnectionDetails(PostgreSQLContainer container) {
		return new LiquibaseConnectionDetails() {
			@Override
			public String getUsername() {
				return MIGRATOR_USER;
			}

			@Override
			public String getPassword() {
				return MIGRATOR_PASSWORD;
			}

			@Override
			public String getJdbcUrl() {
				return container.getJdbcUrl();
			}
		};
	}

}
