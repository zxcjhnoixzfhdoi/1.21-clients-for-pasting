package onl.luka.grizzly.module.modules.other

import com.google.gson.JsonObject
import onl.luka.grizzly.config.ConfigManager
import onl.luka.grizzly.config.PresetMetadata
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.NotificationManager
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft

object AutoConfig : Module(
    "Auto Config",
    "Loads a saved config automatically based on the server you join",
    Category.OTHER,
) {
    override val includeInPresets = false
    override val showInModulesList = false

    init { enabled.value = true }

    enum class Mode { AUTOMATIC, MANUAL }

    val mode = enum("mode", Mode.AUTOMATIC)
    val preferOnline = boolean("prefer online configs", true).also {
        it.visibleWhen = { mode.value == Mode.AUTOMATIC }
    }
    val rememberPerServer = boolean("remember per server", true)
    val notify = boolean("notify", true)
    val mappings = serverMappings("mappings").also {
        it.visibleWhen = { mode.value == Mode.MANUAL }
    }

    private class SessionState(val values: JsonObject, val preset: String?)

    private var personalState: JsonObject? = null
    private val serverStates = mutableMapOf<String, SessionState>()
    private var currentServer: String? = null
    private var activePreset: String? = null
    private var lastServer: String? = null
    private var leftServerAt = 0L
    private var registered = false

    fun init() {
        if (registered) return
        registered = true
        ClientPlayConnectionEvents.JOIN.register { _, _, client -> onJoin(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> onDisconnect() }
    }

    override fun onEnabled() {
        init()
    }

    private fun onJoin(client: Minecraft) {
        if (!isEnabled()) return
        val address = client.currentServer?.ip?.takeIf { it.isNotBlank() } ?: return
        val key = PresetMetadata.normalizeAddress(address)

        if (isProxyHop(key)) {
            currentServer = key
            return
        }
        currentServer = key

        if (rememberPerServer.value) {
            serverStates[key]?.let { remembered ->
                val changed = !matchesCurrent(remembered.values)
                ConfigManager.restoreSnapshot(remembered.values)
                activePreset = remembered.preset
                announce(changed, "Restored your setup")
                return
            }
        }

        val preset = resolvePreset(address)
        if (preset == null) {
            restorePersonalState()
            return
        }

        if (personalState == null) personalState = ConfigManager.snapshot()

        val alreadyRunning = activePreset == preset
        if (!ConfigManager.loadPreset(preset, preferOnline.value)) {
            if (notify.value) NotificationManager.showError(name, "Could not load $preset")
            return
        }
        activePreset = preset
        announce(!alreadyRunning, "Loaded \"$preset\"")
    }

    private fun announce(changed: Boolean, message: String) {
        if (notify.value && changed) NotificationManager.showConfig(name, message)
    }

    private fun matchesCurrent(values: JsonObject): Boolean = ConfigManager.snapshot() == values

    private fun onDisconnect() {
        val key = currentServer
        currentServer = null
        lastServer = key
        leftServerAt = System.currentTimeMillis()
        if (key == null || !rememberPerServer.value) return
        serverStates[key] = SessionState(ConfigManager.snapshot(), activePreset)
    }

    private fun isProxyHop(key: String): Boolean {
        if (currentServer == key) return true
        if (currentServer != null) return false
        return lastServer == key && System.currentTimeMillis() - leftServerAt <= HOP_GRACE_MS
    }

    private fun restorePersonalState() {
        val state = personalState ?: return
        val changed = !matchesCurrent(state)
        personalState = null
        activePreset = null
        ConfigManager.restoreSnapshot(state)
        announce(changed, "Restored your config")
    }

    private fun resolvePreset(address: String): String? = when (mode.value) {
        Mode.MANUAL -> mappings.presetFor(address)
        Mode.AUTOMATIC -> ConfigManager.presetsForServer(address, preferOnline.value).firstOrNull()
    }

    override fun hudInfo(): String = mode.value.name.lowercase()

    private const val HOP_GRACE_MS = 8_000L
}
