plugins {
	`java-library`
}

dependencies {
	// `api` so that :infrastructure sees domain types transitively.
	// Application depends on nothing else — no web, no persistence, no Spring.
	api(project(":domain"))

	// Test-scoped only, so the main classpath the architecture test checks stays domain-only.
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}
