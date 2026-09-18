import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

version = "mc${property("minecraft_version")}-${property("mod_version")}"
group = property("maven_group") as String
val archivesBaseName = property("archives_base_name") as String

repositories {
    mavenCentral()
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
}

val echoBuild = (extra["echoBuild"] as Map<*, *>).mapKeys { it.key.toString() }

fun echoBuildString(key: String): String {
    return echoBuild[key]?.toString()
        ?: error("Missing echoBuild['$key'] in build script")
}

fun echoBuildBoolean(key: String): Boolean {
    return echoBuild[key]?.toString()?.toBooleanStrictOrNull()
        ?: error("Missing or invalid echoBuild['$key'] in build script")
}

fun optionalProperty(name: String): String? {
    val value = findProperty(name)?.toString()?.trim()
    if (value.isNullOrEmpty()) {
        return null
    }

    return value
}

fun addOptionalDependency(configurationName: String, coordinates: String?) {
    if (coordinates == null) {
        return
    }

    val resolved = configurations.findByName(configurationName)
        ?: configurations.findByName("compileOnly")
        ?: return
    dependencies.add(resolved.name, coordinates)
}

fun invokeNoArg(target: Any, name: String): Any? {
    val method = target.javaClass.methods.firstOrNull {
        it.name == name && it.parameterCount == 0
    } ?: return null

    return method.invoke(target)
}

val lwjglVersion = "3.3.3"
val winNatives = "natives-windows"
val linuxNatives = "natives-linux"
val macosNatives = "natives-macos"
val macosArmNatives = "natives-macos-arm64"

val javaReleaseVersion = (findProperty("java_release_version")?.toString() ?: "21").toInt()
val javaToolchainVersion = (findProperty("java_toolchain_version")?.toString() ?: "25").toInt()
val useAccessWidener = (findProperty("use_access_widener")?.toString() ?: "true").toBoolean()
val accessWidenerFilePath = findProperty("access_widener_file")?.toString()
    ?: "src/main/resources/echo.accesswidener"
val accessWidenerFile = rootProject.file(accessWidenerFilePath)
val usesDefaultAccessWidenerFile = accessWidenerFilePath == "src/main/resources/echo.accesswidener"
val minecraftDependency = optionalProperty("minecraft_dependency")
    ?: property("minecraft_version").toString()
val loomExtension = extensions.getByName("loom")

dependencies {
    val lwjglVulkan = "org.lwjgl:lwjgl-vulkan:$lwjglVersion"
    add("implementation", lwjglVulkan)
    add("include", lwjglVulkan)

    fun includeNatives(name: String) {
        val base = "$name:$lwjglVersion"
        val windows = "$name:$lwjglVersion:$winNatives"
        val linux = "$name:$lwjglVersion:$linuxNatives"
        val macos = "$name:$lwjglVersion:$macosNatives"
        val macosArm = "$name:$lwjglVersion:$macosArmNatives"

        add("implementation", base)
        add("include", base)

        add("runtimeOnly", windows)
        add("include", windows)

        add("runtimeOnly", linux)
        add("include", linux)

        add("runtimeOnly", macos)
        add("include", macos)

        add("runtimeOnly", macosArm)
        add("include", macosArm)
    }

    includeNatives("org.lwjgl:lwjgl-vma")
    includeNatives("org.lwjgl:lwjgl-shaderc")

    val vulkanMacos = "org.lwjgl:lwjgl-vulkan:$lwjglVersion:$macosNatives"
    val vulkanMacosArm = "org.lwjgl:lwjgl-vulkan:$lwjglVersion:$macosArmNatives"
    add("runtimeOnly", vulkanMacos)
    add("include", vulkanMacos)
    add("runtimeOnly", vulkanMacosArm)
    add("include", vulkanMacosArm)
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${property("minecraft_version")}")

    if (echoBuildBoolean("includeMappings")) {
        val officialMappings = invokeNoArg(loomExtension, "officialMojangMappings")
        if (officialMappings != null) {
            add("mappings", officialMappings)
        }
    }

    add(echoBuildString("loaderConfiguration"), "net.fabricmc:fabric-loader:${property("loader_version")}")
    add(echoBuildString("fabricApiConfiguration"), "net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    addOptionalDependency(
        echoBuildString("compileOnlyConfiguration"),
        optionalProperty("sodium_version")?.let { "maven.modrinth:sodium:${property("sodium_version")}" }
    )
    addOptionalDependency(
        echoBuildString("compileOnlyConfiguration"),
        optionalProperty("vulkanmod_version")?.let { "maven.modrinth:vulkanmod:${property("vulkanmod_version")}" }
    )

    add("implementation", "net.java.dev.jna:jna:5.18.1")
    add("implementation", "net.java.dev.jna:jna-platform:5.18.1")

    add("compileOnly", "org.projectlombok:lombok:1.18.38")
    add("annotationProcessor", "org.projectlombok:lombok:1.18.38")

    add("testCompileOnly", "org.projectlombok:lombok:1.18.38")
    add("testAnnotationProcessor", "org.projectlombok:lombok:1.18.38")

    add("testImplementation", platform("org.junit:junit-bom:5.10.0"))
    add("testImplementation", "org.junit.jupiter:junit-jupiter")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
}

tasks.named("processResources", ProcessResources::class.java) {
    inputs.property("version", project.version)
    inputs.property("accessWidenerFile", accessWidenerFilePath)
    inputs.property("minecraftDependency", minecraftDependency)

    val stonecutterVersionName = project.name
    val templateVersion = when {
        stonecutterVersionName == "26.1" || stonecutterVersionName.startsWith("26.1.") -> "26.1"
        stonecutterVersionName == "26.2" || stonecutterVersionName.startsWith("26.2.") || stonecutterVersionName.startsWith("26.2-") -> "26.2"
        else -> null
    }

    val selectedFabricModTemplate = templateVersion?.let {
        rootProject.file("src/main/resources/fabric.mod.$it.json")
    } ?: rootProject.file("src/main/resources/fabric.mod.json")
    val selectedEchoMixinsTemplate = templateVersion?.let {
        rootProject.file("src/main/resources/echo.mixins.$it.json")
    } ?: rootProject.file("src/main/resources/echo.mixins.json")

    inputs.property("resourceTemplateVersion", templateVersion ?: "default")
    inputs.file(selectedFabricModTemplate)
    inputs.file(selectedEchoMixinsTemplate)

    exclude(
        "fabric.mod.26.1.json",
        "echo.mixins.26.1.json",
        "fabric.mod.26.2.json",
        "echo.mixins.26.2.json"
    )

    if (templateVersion != null) {
        exclude("fabric.mod.json", "echo.mixins.json")

        from(selectedFabricModTemplate) {
            rename { "fabric.mod.json" }
            filteringCharset = "UTF-8"
            val versionValue = project.version.toString()
            val minecraftDependencyValue = minecraftDependency
            filter { line: String ->
                line
                    .replace("\${version}", versionValue)
                    .replace("\${minecraft_dependency}", minecraftDependencyValue)
                    .replace("\${aw_file}", "echo.accesswidener")
            }
        }

        from(selectedEchoMixinsTemplate) {
            rename { "echo.mixins.json" }
        }
    } else {
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to project.version.toString(),
                    "aw_file" to "echo.accesswidener",
                    "minecraft_dependency" to minecraftDependency
                )
            )
        }
    }

    if (useAccessWidener && !usesDefaultAccessWidenerFile) {
        from(accessWidenerFile) {
            rename { "echo.accesswidener" }
        }
    }

    doLast {
        val outputDir = destinationDir
        val fabricModOutput = outputDir.resolve("fabric.mod.json")
        val echoMixinsOutput = outputDir.resolve("echo.mixins.json")

        val fabricModJson = selectedFabricModTemplate.readText(Charsets.UTF_8)
            .replace("\${version}", project.version.toString())
            .replace("\${minecraft_dependency}", minecraftDependency)
            .replace("\${aw_file}", "echo.accesswidener")

        fabricModOutput.writeText(fabricModJson, Charsets.UTF_8)
        echoMixinsOutput.writeText(selectedEchoMixinsTemplate.readText(Charsets.UTF_8), Charsets.UTF_8)
    }

}

tasks.withType(JavaCompile::class.java).configureEach {
    options.release.set(javaReleaseVersion)
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    archiveBaseName.set(archivesBaseName)
}

extensions.configure(JavaPluginExtension::class.java) {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaToolchainVersion))
    }

    sourceCompatibility = JavaVersion.toVersion(javaReleaseVersion)
    targetCompatibility = JavaVersion.toVersion(javaReleaseVersion)
}

tasks.named("jar", org.gradle.jvm.tasks.Jar::class.java) {
    inputs.property("archivesName", archivesBaseName)

    from("LICENSE") {
        rename { "${it}_$archivesBaseName" }
    }
}

tasks.named("test", org.gradle.api.tasks.testing.Test::class.java) {
    useJUnitPlatform()
}

extensions.configure(PublishingExtension::class.java) {
    publications {
        create("mavenJava", MavenPublication::class.java) {
            artifactId = property("archives_base_name") as String
            from(components.getByName("java"))
        }
    }
}

tasks.register("transformSources") {
    description = "Transforms Concat.of() calls before compilation"

    val originalSourceDir = rootProject.file("src/main/java")
    val stonecutterCacheDir = file("build/stonecutter-cache/sources/main/java")
    val transformedDir = file("build/transformed-sources")
    val featuresJsonPath = rootProject.file("features.json").absolutePath

    dependsOn("stonecutterPrepare")

    inputs.dir(originalSourceDir)
    outputs.dir(transformedDir)

    doLast {
        delete(transformedDir)
        transformedDir.mkdirs()

        copy {
            from(originalSourceDir)
            into(transformedDir)
        }

        if (stonecutterCacheDir.exists()) {
            copy {
                from(stonecutterCacheDir)
                into(transformedDir)
            }
            println("Overlaid Stonecutter-processed sources from cache")
        } else {
            println("WARNING: Stonecutter cache not found - debug guards will NOT be processed!")
        }

        val stonecutterKts = rootProject.file("stonecutter.gradle.kts")
        var authEnabled = false
        if (stonecutterKts.exists()) {
            val content = stonecutterKts.readText()
            val authMatch = Regex("constants\\[\\\"auth\\\"\\]\\s*=\\s*(true|false)").find(content)
            if (authMatch != null) {
                authEnabled = authMatch.groupValues[1] == "true"
            }
        }

        if (authEnabled) {
            println("Auth enabled - running SettingNameTransformer...")
            SettingNameTransformer.transform(transformedDir.absolutePath, featuresJsonPath)

            println("Auth enabled - running ValueLiteralTransformer...")
            //ValueLiteralTransformer.transform(transformedDir.absolutePath, featuresJsonPath)
        } else {
            println("Auth disabled - skipping SettingNameTransformer (inline strings preserved)")
        }

        println("Source transformation completed!")
    }
}

tasks.named("compileJava", JavaCompile::class.java) {
    dependsOn(tasks.named("transformSources"))
    setSource(fileTree("build/transformed-sources"))

    doFirst {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        extra["originalSource"] = sourceSets.getByName("main").java.srcDirs
    }
}

tasks.named("clean") {
    doLast {
        delete("build/transformed-sources")
        println("Transformed sources cleaned!")
    }
}

tasks.register("cleanBuildSrc") {
    doLast {
        delete("buildSrc/build")
        println("buildSrc cleaned!")
    }
}

if (useAccessWidener) {
    if (!usesDefaultAccessWidenerFile) {
        val sourceSets = extensions.getByType(SourceSetContainer::class.java)
        sourceSets.getByName("main").resources.exclude("echo.accesswidener")
    }

    val accessWidenerProperty = invokeNoArg(loomExtension, "getAccessWidenerPath")
    accessWidenerProperty?.javaClass?.methods?.firstOrNull {
        it.name == "fileValue" && it.parameterCount == 1
    }?.invoke(accessWidenerProperty, accessWidenerFile)
}
