// Root is an aggregator only — no plugins, no artifact of its own.
// Each module owns its own plugins and dependencies.

allprojects {
	group = "com.lifetracker"
	// Tracks life-tracker-contracts' info.version, deliberately: the three repos ship as one
	// product, and a reader asking "which contract does this build speak?" should not have to
	// cross-reference two numbers. Nothing is published to a repository, so this is metadata —
	// the Dockerfile globs infrastructure-*.jar rather than naming a version.
	version = "0.8.2"
}

// Shared Java configuration for the JVM modules. Guarded on the Java plugin so the
// migrations module (Liquibase-only, no `java` plugin) is left untouched.
subprojects {
	plugins.withType<JavaPlugin> {
		extensions.configure<JavaPluginExtension> {
			toolchain {
				languageVersion = JavaLanguageVersion.of(25)
			}
		}
		tasks.withType<Test> {
			useJUnitPlatform()
		}
	}
}
