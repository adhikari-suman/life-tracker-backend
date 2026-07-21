plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

dependencies {
	implementation(project(":application"))

	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.spring.security.crypto)
	runtimeOnly(libs.postgresql)
	runtimeOnly(libs.bouncycastle.bcprov)   // Argon2id implementation used by spring-security-crypto

	testImplementation(libs.spring.boot.starter.actuator.test)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
	testImplementation(libs.archunit.junit5)
	// Spring Boot's Liquibase auto-config, so the Testcontainers boot applies the :migrations
	// changelog before Hibernate validates (test-only; prod runs the separate migration job).
	// Brings liquibase-core transitively.
	testImplementation(libs.spring.boot.liquibase)
	testRuntimeOnly(libs.junit.platform.launcher)
}

// The Liquibase changelogs live in :migrations. Put them on the TEST classpath so Spring Boot's
// Liquibase applies them before Hibernate validates the entities. Production never bundles
// Liquibase — the migration job runs separately (ADR-0009).
sourceSets {
	test {
		resources.srcDir("../migrations/src/main/resources")
	}
}
