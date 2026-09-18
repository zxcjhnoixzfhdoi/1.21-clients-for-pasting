package onl.luka.grizzly.config.entry

import com.google.gson.JsonArray
import com.google.gson.JsonElement

class ModuleListEntry(
    name: String,
    default: List<String> = emptyList(),
) : ConfigEntry<List<String>>(name, default) {

    fun contains(moduleName: String): Boolean =
        value.any { it.equals(moduleName, ignoreCase = true) }

    fun add(moduleName: String) {
        val trimmed = moduleName.trim()
        if (trimmed.isEmpty() || contains(trimmed)) return
        value = value + trimmed
    }

    fun remove(moduleName: String) {
        value = value.filterNot { it.equals(moduleName, ignoreCase = true) }
    }

    override fun toJson(): JsonElement =
        JsonArray().apply { value.forEach(::add) }

    override fun fromJson(element: JsonElement) {
        element.arrayOrNull?.let { array ->
            value = array.toListSafely { item -> item.stringOrNull }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy { it.lowercase() }
        }
    }
}
