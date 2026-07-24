package com.lifetracker.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	// Pinned, and pinned to the same tag as compose.yaml. `latest` meant the tests proved the
	// schema against whichever Postgres happened to be pulled, and against a different one than
	// the local stack ran — drift of exactly the kind `ddl-auto: validate` exists to catch.
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

}
