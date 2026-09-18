package onl.luka.grizzly.config

import onl.luka.grizzly.module.Module

object PresetFilter {

    fun includedInSave(config: Config, meta: PresetMetadata): Boolean {
        if (!config.includeInPresets) return false
        val module = config as? Module ?: return true
        return meta.covers(module.category)
    }

    fun shouldApply(config: Config, saved: com.google.gson.JsonObject, meta: PresetMetadata): Boolean {
        if (!config.includeInPresets) return false
        if (meta.overwriteUnbound) return true

        if (config !is Module) return true
        return savedAsEnabled(saved) || savedAsBound(saved)
    }

    private fun savedAsEnabled(saved: com.google.gson.JsonObject): Boolean =
        saved.get("enabled")?.takeIf { it.isJsonPrimitive }?.asBoolean == true

    private fun savedAsBound(saved: com.google.gson.JsonObject): Boolean {
        if (boundKey(saved.get("keybind"))) return true

        val profiles = saved.getAsJsonObject("profiles") ?: return false
        return profiles.entrySet().any { (_, element) ->
            val profile = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
            profile.entrySet().any { (key, value) -> key.startsWith("profile_") && boundKey(value) }
        }
    }

    private fun boundKey(element: com.google.gson.JsonElement?): Boolean {
        val primitive = element?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return false
        if (!primitive.isNumber) return false
        return primitive.asInt != UNBOUND_KEY
    }

    private const val UNBOUND_KEY = -1
}
