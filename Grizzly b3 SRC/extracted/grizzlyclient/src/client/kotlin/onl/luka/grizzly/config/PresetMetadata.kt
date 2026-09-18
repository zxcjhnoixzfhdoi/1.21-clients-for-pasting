package onl.luka.grizzly.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import onl.luka.grizzly.module.Module

class PresetMetadata(
    var description: String = "",
    var anticheat: String = "",
    var servers: MutableList<String> = mutableListOf(),
    var categories: MutableSet<Module.Category> = Module.Category.entries.toMutableSet(),
    var overwriteUnbound: Boolean = true,
    var disableUnaffected: Boolean = false,
) {
    fun covers(category: Module.Category): Boolean = category in categories

    fun matchesServer(address: String): Boolean {
        val host = normalizeAddress(address)
        if (host.isEmpty()) return false
        return servers.any { pattern ->
            val wanted = normalizeAddress(pattern)
            wanted.isNotEmpty() && (host == wanted || host.endsWith(".$wanted"))
        }
    }

    fun copy(): PresetMetadata = PresetMetadata(
        description,
        anticheat,
        servers.toMutableList(),
        categories.toMutableSet(),
        overwriteUnbound,
        disableUnaffected,
    )

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("description", description)
        addProperty("anticheat", anticheat)
        add("servers", JsonArray().apply { servers.forEach(::add) })
        add("categories", JsonArray().apply { categories.forEach { add(it.name) } })
        addProperty("overwriteUnbound", overwriteUnbound)
        addProperty("disableUnaffected", disableUnaffected)
    }

    companion object {
        const val KEY = "__meta"

        fun normalizeAddress(raw: String): String =
            raw.trim()
                .lowercase()
                .substringBefore('/')
                .substringBefore(':')
                .removePrefix("www.")

        // Presets written before metadata existed simply have none, so the defaults have to be
        // the pre-existing behaviour: everything included, everything overwritten.
        fun fromJson(root: JsonObject?): PresetMetadata {
            val obj = root?.takeIf { it.has(KEY) }?.getAsJsonObject(KEY) ?: return PresetMetadata()
            val meta = PresetMetadata()

            obj.get("description")?.takeIf { it.isJsonPrimitive }?.let { meta.description = it.asString }
            obj.get("anticheat")?.takeIf { it.isJsonPrimitive }?.let { meta.anticheat = it.asString }
            obj.get("overwriteUnbound")?.takeIf { it.isJsonPrimitive }?.let { meta.overwriteUnbound = it.asBoolean }
            obj.get("disableUnaffected")?.takeIf { it.isJsonPrimitive }?.let { meta.disableUnaffected = it.asBoolean }

            obj.getAsJsonArray("servers")?.let { array ->
                meta.servers = array.mapNotNull { it.takeIf(::isText)?.asString?.trim() }
                    .filter(String::isNotEmpty)
                    .distinct()
                    .toMutableList()
            }

            obj.getAsJsonArray("categories")?.let { array ->
                val parsed = array.mapNotNull { element ->
                    val name = element.takeIf(::isText)?.asString ?: return@mapNotNull null
                    Module.Category.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                }
                if (parsed.isNotEmpty()) meta.categories = parsed.toMutableSet()
            }

            return meta
        }

        private fun isText(element: com.google.gson.JsonElement): Boolean =
            element.isJsonPrimitive && element.asJsonPrimitive.isString
    }
}
