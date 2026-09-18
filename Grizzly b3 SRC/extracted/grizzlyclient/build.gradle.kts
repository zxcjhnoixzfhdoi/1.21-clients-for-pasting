import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

base {
    archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
	maven {
		name = "Hypixel"
		url = uri("https://repo.hypixel.net/repository/Hypixel/")
	}
	exclusiveContent {
		forRepository {
			maven {
				name = "Modrinth"
				url = uri("https://api.modrinth.com/maven")
			}
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("medved") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// Official Hypixel Mod API Fabric implementation, compatible with 26.1 and 26.2.
	implementation("net.hypixel:mod-api:1.0.2")
	implementation("maven.modrinth:1A2mKfBx:SlqCF6Or")
	include("maven.modrinth:1A2mKfBx:SlqCF6Or")
}

val fontTiers = listOf(1, 2, 3, 4)
val fontSpec = layout.projectDirectory.file("src/client/fonts.json")
val generatedFontDir = layout.buildDirectory.dir("generated/fonts")

val generateFontDefinitions by tasks.registering {
    inputs.file(fontSpec)
    outputs.dir(generatedFontDir)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val fonts = groovy.json.JsonSlurper().parse(fontSpec.asFile) as Map<String, Map<String, Any>>
        val outputDir = generatedFontDir.get().dir("assets/medved/font").asFile
        outputDir.mkdirs()

        for ((name, config) in fonts) {
            for (tier in fontTiers) {
                val provider = linkedMapOf<String, Any>(
                    "type" to "ttf",
                    "file" to "medved:" + (config["file"] as String),
                    "size" to (config["size"] as Number).toDouble(),
                    "oversample" to tier.toDouble(),
                )
                config["shift"]?.let { provider["shift"] = it }

                val suffix = if (tier == 1) "" else "_" + tier + "x"
                val json = groovy.json.JsonOutput.prettyPrint(
                    groovy.json.JsonOutput.toJson(mapOf("providers" to listOf(provider)))
                )
                outputDir.resolve(name + suffix + ".json").writeText(json + "\n")
            }
        }
    }
}

val generatedStampDir = layout.buildDirectory.dir("generated/buildstamp")

fun gitOutput(vararg command: String): String = runCatching {
    val process = ProcessBuilder(*command)
        .directory(layout.projectDirectory.asFile)
        .redirectErrorStream(true)
        .start()
    val text = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) text else ""
}.getOrDefault("")

val generateBuildStamp by tasks.registering {
    outputs.dir(generatedStampDir)
    outputs.upToDateWhen { false }

    doLast {
        val env = System.getenv()
        val commit = env["GITHUB_SHA"]?.takeIf { it.isNotBlank() } ?: gitOutput("git", "rev-parse", "HEAD")
        val branch = env["GITHUB_REF_NAME"]?.takeIf { it.isNotBlank() }
            ?: gitOutput("git", "rev-parse", "--abbrev-ref", "HEAD")
        val commitSeconds = gitOutput("git", "log", "-1", "--format=%ct").toLongOrNull() ?: 0L
        val ci = !env["GITHUB_SHA"].isNullOrBlank()
        val dirty = !ci && gitOutput("git", "status", "--porcelain").isNotEmpty()

        val stamp = mapOf(
            "commit" to commit,
            "branch" to branch,
            "commitTime" to commitSeconds * 1000L,
            "dirty" to dirty,
            "ci" to ci,
        )
        val file = generatedStampDir.get().dir("assets/medved").asFile
        file.mkdirs()
        file.resolve("build.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(stamp)))
    }
}

val updaterSources = sourceSets.register("updater")

val updaterJar by tasks.registering(Jar::class) {
    from(updaterSources.get().output)
    archiveFileName = "updater.jar"
    destinationDirectory = layout.buildDirectory.dir("tmp/updater-jar")
    manifest { attributes("Main-Class" to "onl.luka.grizzly.updater.UpdaterMain") }
}

val generatedUpdaterDir = layout.buildDirectory.dir("generated/updater")

val embedUpdater by tasks.registering(Copy::class) {
    into(generatedUpdaterDir)
    from(updaterJar) { into("assets/medved") }
}

sourceSets.getByName("client") {
    resources.srcDir(generatedFontDir)
    resources.srcDir(generateBuildStamp)
    resources.srcDir(embedUpdater)
}

tasks.withType<AbstractCopyTask>().configureEach {
    dependsOn(generateFontDefinitions)
}

tasks.processResources {
    val props = project.properties.filterValues { it is String }.mapValues {
        (it.value as String)
			.replace("-snapshot-", "-alpha.")
			.replace("-rc-", "-rc.")
    }
    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	archiveVersion = ""
    inputs.property("archivesName", base.archivesName)

    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
