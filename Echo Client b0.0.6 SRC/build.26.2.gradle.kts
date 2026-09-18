plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
    id("dev.kikugie.stonecutter")
}

extra["echoBuild"] = mapOf(
    "loaderConfiguration" to "implementation",
    "fabricApiConfiguration" to "implementation",
    "compileOnlyConfiguration" to "modCompileOnly",
    "includeMappings" to false
)

apply(from = rootProject.file("gradle/echo.shared.gradle.kts"))

val runClientUsername = (
    findProperty("run_client_username")
        ?: rootProject.findProperty("run_client_username")
        ?: "Player"
).toString()

loom {
    runs {
        named("client") {
            programArg("--username=$runClientUsername")
            vmArg("-Dfabric.development=true")
        }
    }
}
