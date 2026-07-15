plugins {
	`java-library`
}

dependencies {
	// `api` so that :infrastructure sees domain types transitively.
	// Application depends on nothing else — no web, no persistence, no Spring.
	api(project(":domain"))
}
