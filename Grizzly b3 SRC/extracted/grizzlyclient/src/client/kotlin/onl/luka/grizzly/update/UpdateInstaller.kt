package onl.luka.grizzly.update

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipFile

object UpdateInstaller {

    private val workDir: Path
        get() = FabricLoader.getInstance().gameDir.resolve(".grizzly-update")

    @Volatile var lastRejection: String? = null
        private set

    @Volatile private var armed = false
    @Volatile private var hookRegistered = false
    private var command: List<String> = emptyList()

    val isArmed: Boolean get() = armed

    enum class Result { STAGED, ALREADY_CURRENT, INCOMPATIBLE, FAILED }

    class Pending(val label: String, val commit: String, val commitTime: Long)

    fun stage(build: RemoteBuild, announce: Boolean): Result {
        val target = BuildStamp.jar ?: return Result.FAILED
        val dir = workDir.resolve(UUID.randomUUID().toString())
        Files.createDirectories(dir)

        return runCatching {
            val download = dir.resolve("download.tmp")
            URI(build.downloadUrl).toURL().openStream().use { input ->
                Files.copy(input, download, StandardCopyOption.REPLACE_EXISTING)
            }

            val staged = dir.resolve("next.jar")
            if (build.zipped) extractJar(download, staged) else Files.move(download, staged)
            require(isLoadableMod(staged)) { "staged file is not a Grizzly jar" }

            incompatibility(staged)?.let { reason ->
                lastRejection = reason
                dir.toFile().deleteRecursively()
                return Result.INCOMPATIBLE
            }

            val incoming = BuildStamp.readFrom(staged)
            require(incoming.isPresent) { "downloaded build carries no version stamp" }
            if (incoming.isSameBuildAs(BuildStamp.local)) {
                dir.toFile().deleteRecursively()
                return Result.ALREADY_CURRENT
            }

            val updater = dir.resolve("updater.jar")
            javaClass.getResourceAsStream(UPDATER_RESOURCE)?.use { input ->
                Files.copy(input, updater, StandardCopyOption.REPLACE_EXISTING)
            } ?: error("updater jar missing from this build")

            arm(updater, dir.resolve("update.log"), staged, target, build.label.takeIf { announce })
            writePending(build.label, incoming)
            Result.STAGED
        }.onFailure {
            dir.toFile().deleteRecursively()
        }.getOrDefault(Result.FAILED)
    }

    private fun incompatibility(jar: Path): String? {
        val depends = runCatching {
            ZipFile(jar.toFile()).use { archive ->
                val entry = archive.getEntry("fabric.mod.json") ?: return@use null
                val text = archive.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                Gson().fromJson(text, JsonObject::class.java)?.getAsJsonObject("depends")
            }
        }.getOrNull() ?: return null

        val loader = FabricLoader.getInstance()
        for ((id, element) in depends.entrySet()) {
            val wanted = when {
                element.isJsonArray -> element.asJsonArray.mapNotNull { it.asString }
                element.isJsonPrimitive -> listOf(element.asString)
                else -> continue
            }
            if (wanted.any { it == "*" }) continue

            val installed = loader.getModContainer(id).orElse(null)?.metadata?.version
                ?: return "it needs $id, which you do not have"

            val satisfied = wanted.any { range ->
                runCatching { VersionPredicate.parse(range).test(installed) }.getOrDefault(false)
            }
            if (!satisfied) {
                return "it needs $id ${wanted.joinToString(" or ")}, you have ${installed.friendlyString}"
            }
        }
        return null
    }

    private fun writePending(label: String, incoming: Stamp) {
        runCatching {
            val json = JsonObject().apply {
                addProperty("label", label)
                addProperty("commit", incoming.commit)
                addProperty("commitTime", incoming.commitTime)
            }
            Files.createDirectories(workDir)
            Files.writeString(pendingFile, json.toString())
        }
    }

    fun consumePending(): Pending? = runCatching {
        if (!Files.isRegularFile(pendingFile)) return null
        val json = Gson().fromJson(Files.readString(pendingFile), JsonObject::class.java)
        Files.deleteIfExists(pendingFile)
        Pending(
            label = json.get("label")?.asString.orEmpty(),
            commit = json.get("commit")?.asString.orEmpty(),
            commitTime = json.get("commitTime")?.asLong ?: 0L,
        )
    }.getOrNull()

    private val pendingFile: Path get() = workDir.resolve("pending.json")

    private fun extractJar(zip: Path, destination: Path) {
        ZipFile(zip.toFile()).use { archive ->
            val entry = archive.entries().asSequence().firstOrNull { candidate ->
                val name = candidate.name.substringAfterLast('/')
                name.endsWith(".jar", true) && !name.contains("sources", true)
            } ?: error("no mod jar inside the build artifact")

            archive.getInputStream(entry).use { input ->
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        Files.deleteIfExists(zip)
    }

    private fun isLoadableMod(jar: Path): Boolean = runCatching {
        ZipFile(jar.toFile()).use { it.getEntry("fabric.mod.json") != null }
    }.getOrDefault(false)

    private fun arm(updater: Path, log: Path, staged: Path, target: Path, label: String?) {
        val java = Path.of(System.getProperty("java.home"), "bin", if (isWindows()) "java.exe" else "java")
        command = buildList {
            add(java.toAbsolutePath().toString())
            add("-jar"); add(updater.toAbsolutePath().toString())
            add(log.toAbsolutePath().toString())
            add("move"); add(staged.toAbsolutePath().toString()); add(target.toAbsolutePath().toString())
            label?.let { add("announce"); add(it) }
        }
        armed = true

        if (hookRegistered) return
        hookRegistered = true
        Runtime.getRuntime().addShutdownHook(Thread(::launch, "grizzly-update"))
    }

    private fun launch() {
        if (!armed) return
        runCatching {
            ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }

    fun cleanup() {
        val dir = workDir.toFile()
        if (!dir.isDirectory) return
        val cutoff = System.currentTimeMillis() - STALE_AFTER_MS
        dir.listFiles()?.forEach { stale ->
            if (stale.isDirectory && stale.lastModified() < cutoff) runCatching { stale.deleteRecursively() }
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    private const val UPDATER_RESOURCE = "/assets/medved/updater.jar"
    private const val STALE_AFTER_MS = 10 * 60 * 1000L
}
