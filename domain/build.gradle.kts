plugins {
	`java-library`
}

// The domain is plain Java: zero dependencies, not even Spring.
// The architecture test (in :infrastructure) enforces this; the empty dependency
// graph is what makes the rule impossible to violate by accident.
