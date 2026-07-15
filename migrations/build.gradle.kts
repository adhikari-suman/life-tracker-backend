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
// Connection details default to a local Postgres and can be overridden with
//   -Pdb.url=... -Pdb.username=... -Pdb.password=...
dependencies {
	liquibaseRuntime(libs.liquibase.core)
	liquibaseRuntime(libs.postgresql)
	liquibaseRuntime(libs.picocli)
}

liquibase {
	activities.register("main") {
		this.arguments = mapOf(
			"changelogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",
			"url" to (findProperty("db.url") as String? ?: "jdbc:postgresql://localhost:5432/lifetracker"),
			"username" to (findProperty("db.username") as String? ?: "postgres"),
			"password" to (findProperty("db.password") as String? ?: "postgres"),
		)
	}
	runList = "main"
}
