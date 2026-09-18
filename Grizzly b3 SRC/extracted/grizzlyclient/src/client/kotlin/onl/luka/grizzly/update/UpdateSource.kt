package onl.luka.grizzly.update

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import onl.luka.grizzly.util.HttpUtils
import java.time.OffsetDateTime

class RemoteBuild(
    val label: String,
    val commit: String?,
    val publishedAt: Long,
    val downloadUrl: String,
    val zipped: Boolean,
)

object UpdateSource {

    private val gson = Gson()

    fun latest(channel: Channel, branch: String): RemoteBuild? = when (channel) {
        Channel.RELEASE -> latestRelease()
        Channel.BETA -> latestBeta(branch.trim().ifEmpty { "main" })
    }

    enum class Channel { RELEASE, BETA }

    private fun latestRelease(): RemoteBuild? {
        val release = get("$API/releases/latest")?.asJsonObjectOrNull() ?: return null
        if (release.bool("draft") || release.bool("prerelease")) return null

        val asset = release.getAsJsonArray("assets")?.firstOrNull { element ->
            val name = element.asJsonObjectOrNull()?.str("name").orEmpty()
            name.endsWith(".jar", true) && !name.contains("sources", true)
        }?.asJsonObjectOrNull() ?: return null

        val url = asset.str("browser_download_url").ifEmpty { return null }
        val tag = release.str("tag_name").ifEmpty { "latest" }
        return RemoteBuild(tag, null, epochOf(release.str("published_at")), url, zipped = false)
    }

    // Artifacts carry no branch of their own, so the run that produced one is what places it.
    private fun latestBeta(branch: String): RemoteBuild? {
        val runs = get("$API/actions/runs?event=push&status=success")
            ?.asJsonObjectOrNull()?.getAsJsonArray("workflow_runs") ?: return null

        val run = runs.firstNotNullOfOrNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null
            obj.takeIf { it.str("workflow_id") == BUILD_WORKFLOW && it.str("prettyref") == branch }
        } ?: return null

        val runId = run.get("id")?.asLong ?: return null
        val artifact = get("$API/actions/runs/$runId/artifacts")
            ?.let { it as? JsonArray }
            ?.firstNotNullOfOrNull { element ->
                element.asJsonObjectOrNull()?.takeIf { !it.bool("expired") }
            } ?: return null

        val url = artifact.str("archive_download_url").ifEmpty { return null }
        val commit = run.str("commit_sha")
        return RemoteBuild(
            label = "$branch@${commit.take(7)}",
            commit = commit.ifEmpty { null },
            publishedAt = epochOf(artifact.str("created_at")),
            downloadUrl = url,
            zipped = true,
        )
    }

    private fun get(url: String) = runCatching { gson.fromJson(HttpUtils.httpGet(url), com.google.gson.JsonElement::class.java) }.getOrNull()

    private fun epochOf(text: String): Long =
        runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrDefault(0L)

    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): JsonObject? =
        takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.str(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()

    private fun JsonObject.bool(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false

    private const val API = "https://git.luka.onl/api/v1/repos/luka/GrizzlyClient"
    private const val BUILD_WORKFLOW = "build.yml"
}
