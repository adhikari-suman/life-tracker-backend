plugins {
	`java-library`
}

// The domain is plain Java: zero PRODUCTION dependencies, not even Spring.
// The architecture test (in :infrastructure) enforces this; the empty dependency
// graph is what makes the rule impossible to violate by accident.
//
// Tests are plain JUnit only. These are test-scoped, so the main classpath the
// architecture test checks stays empty — an import you can't write in production.
dependencies {
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}
