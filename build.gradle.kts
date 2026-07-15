// Root is an aggregator only — no plugins, no artifact of its own.
// Each module owns its own plugins and dependencies.

allprojects {
	group = "com.lifetracker"
	version = "0.0.1-SNAPSHOT"
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
