package onl.luka.grizzly.update

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class Stamp(
    val commit: String,
    val branch: String,
    val commitTime: Long,
    val dirty: Boolean,
    val ci: Boolean,
) {
    val isPresent: Boolean get() = commit.isNotEmpty()

    fun isSameBuildAs(other: Stamp): Boolean =
        commit == other.commit && commitTime == other.commitTime

    fun describe(): String = buildString {
        append(branch.ifEmpty { "unknown" })
        append('@')
        append(commit.take(7).ifEmpty { "unknown" })
        if (dirty) append(" (modified)")
    }
}

object BuildStamp {

    val local: Stamp = read(BuildStamp::class.java.getResourceAsStream(RESOURCE)?.use { it.readBytes() })

    val jar: Path? by lazy {
        runCatching {
            val origin = FabricLoader.getInstance().getModContainer("medved").orElse(null)?.origin
            if (origin?.kind != ModOrigin.Kind.PATH) return@runCatching null
            origin.paths.firstOrNull { Files.isRegularFile(it) && it.toString().endsWith(".jar", true) }
        }.getOrNull()
    }

    val canSelfUpdate: Boolean get() = jar != null && local.isPresent && local.ci

    fun readFrom(jar: Path): Stamp = runCatching {
        ZipFile(jar.toFile()).use { archive ->
            val entry = archive.getEntry(RESOURCE.removePrefix("/")) ?: return@use read(null)
            read(archive.getInputStream(entry).use { it.readBytes() })
        }
    }.getOrDefault(read(null))

    private fun read(bytes: ByteArray?): Stamp {
        val json = runCatching {
            bytes?.let { Gson().fromJson(it.toString(Charsets.UTF_8), JsonObject::class.java) }
        }.getOrNull() ?: JsonObject()

        return Stamp(
            commit = json.get("commit")?.asString.orEmpty(),
            branch = json.get("branch")?.asString.orEmpty(),
            commitTime = json.get("commitTime")?.asLong ?: 0L,
            dirty = json.get("dirty")?.asBoolean ?: false,
            ci = json.get("ci")?.asBoolean ?: false,
        )
    }

    private const val RESOURCE = "/assets/medved/build.json"
}
