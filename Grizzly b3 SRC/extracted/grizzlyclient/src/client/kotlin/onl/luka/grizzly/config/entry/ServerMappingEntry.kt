package onl.luka.grizzly.config.entry

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import onl.luka.grizzly.config.PresetMetadata

data class ServerMapping(val server: String, val preset: String)

class ServerMappingEntry(
    name: String,
    default: List<ServerMapping> = emptyList(),
) : ConfigEntry<List<ServerMapping>>(name, default) {

    fun add(server: String, preset: String) {
        val host = server.trim()
        val config = preset.trim()
        if (host.isEmpty() || config.isEmpty()) return
        value = value.filterNot { sameServer(it.server, host) } + ServerMapping(host, config)
    }

    fun removeAt(index: Int) {
        if (index !in value.indices) return
        value = value.filterIndexed { position, _ -> position != index }
    }

    fun presetFor(address: String): String? {
        val host = PresetMetadata.normalizeAddress(address)
        if (host.isEmpty()) return null
        return value
            .filter { matches(host, PresetMetadata.normalizeAddress(it.server)) }
            .maxByOrNull { it.server.length }
            ?.preset
    }

    private fun matches(host: String, pattern: String): Boolean =
        pattern.isNotEmpty() && (host == pattern || host.endsWith(".$pattern"))

    private fun sameServer(a: String, b: String): Boolean =
        PresetMetadata.normalizeAddress(a) == PresetMetadata.normalizeAddress(b)

    override fun toJson(): JsonElement = JsonArray().apply {
        value.forEach { mapping ->
            add(JsonObject().apply {
                addProperty("server", mapping.server)
                addProperty("config", mapping.preset)
            })
        }
    }

    override fun fromJson(element: JsonElement) {
        val array = element.arrayOrNull ?: return
        value = array.mapNotNull { item ->
            val obj = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val server = obj.get("server")?.stringOrNull?.trim().orEmpty()
            val preset = obj.get("config")?.stringOrNull?.trim().orEmpty()
            if (server.isEmpty() || preset.isEmpty()) null else ServerMapping(server, preset)
        }
    }
}
