buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		// Liquibase must be on the plugin's (buildscript) classpath at APPLY time: the
		// plugin enumerates Liquibase's commands and global arguments the moment it is
		// applied. The copy on liquibaseRuntime below is the one used at task EXECUTION.
		classpath(libs.liquibase.core)
	}
}

plugins {
	alias(libs.plugins.liquibase)
}

// No `java` plugin: this module has no Java sources and produces no jar. It exists
// only to drive Liquibase against the changelogs from the command line, e.g.
//   ./gradlew :migrations:update
// Connection details default to the compose stack's Postgres, as the migrator role, and can be
// overridden with
//   -Pdb.url=... -Pdb.username=... -Pdb.password=...
dependencies {
	liquibaseRuntime(libs.liquibase.core)
	liquibaseRuntime(libs.postgresql)
	liquibaseRuntime(libs.picocli)
}

// Liquibase resolves `changelogFile` against the JVM's working directory, which for `./gradlew`
// is the ROOT project — so the relative path below found nothing and the task failed from
// anywhere but this module's own directory. A search path fixes that WITHOUT making the path
// absolute, which matters: Liquibase stores the changelog path in DATABASECHANGELOG.FILENAME as
// part of a changeset's identity. Keeping it the relative form is what lets this task and the
// migration image (which sets LIQUIBASE_SEARCH_PATH to the same effect) apply the same changesets
// to the same database without either one re-running the other's work.
val changelogSearchPath = layout.projectDirectory.asFile.path

liquibase {
	activities.register("main") {
		this.arguments = mapOf(
			"searchPath" to changelogSearchPath,
			"changelogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",
			// These mirror the anchors in compose.yaml — one file is YAML and one is Kotlin, so
			// they cannot share a definition. They defaulted to the `postgres` superuser, which
			// silently bypassed the role split: migrations ran with rights the migrator does not
			// have, and objects were created with the wrong owner, so ALTER DEFAULT PRIVILEGES
			// never applied and the app role got no grants.
			"url" to (findProperty("db.url") as String? ?: "jdbc:postgresql://localhost:5432/lifetracker"),
			"username" to (findProperty("db.username") as String? ?: "lifetracker_migrator"),
			"password" to (findProperty("db.password") as String? ?: "migrator-local-only"),
		)
	}
	runList = "main"
}
