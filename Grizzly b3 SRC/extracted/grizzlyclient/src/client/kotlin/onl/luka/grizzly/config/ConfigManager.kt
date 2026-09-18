package onl.luka.grizzly.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.other.Colour
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import org.slf4j.LoggerFactory
import java.nio.file.Path

object ConfigManager {

    private val logger = LoggerFactory.getLogger("medved/config")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val configs = mutableListOf<Config>()
    private val metadataCache = mutableMapOf<String, PresetMetadata>()
    private lateinit var store: ConfigStore
    private lateinit var presetStore: PresetStore
    private lateinit var remotePresetSource: RemotePresetSource

    /** Save every 6 000 ticks (~5 minutes at 20 TPS). */
    private const val SAVE_INTERVAL_TICKS = 6_000
    private var tickCounter = 0

    /**
     * Must be called once during [net.fabricmc.api.ClientModInitializer.onInitializeClient].
     * Registers tick and lifecycle hooks for periodic / shutdown saves.
     */
    fun init(configDir: Path) {
        store = ConfigStore(configDir, gson, logger)
        presetStore = PresetStore(configDir.resolve("presets"), gson, logger)
        remotePresetSource = RemotePresetSource(gson, logger)
        remotePresetSource.refreshAsync()

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            refreshDynamicColors()
            if (++tickCounter >= SAVE_INTERVAL_TICKS) {
                tickCounter = 0
                saveAll()
            }
        }

        LevelRenderEvents.END_MAIN.register { _ ->
            refreshDynamicColors()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            saveAll()
        }
    }

    /** Poll input once per rendered frame so Timer cannot delay keybind edges. */
    @JvmStatic
    fun pollKeybinds() {
        configs.forEach { it.tickKeybinds() }
    }

    /**
     * Register a config with the manager. The config's values are loaded from
     * disk immediately (or written as defaults if no file exists yet).
     */
    fun <T : Config> register(config: T): T {
        configs += config
        load(config)
        return config
    }

    fun saveAll() = configs.forEach(::save)

    fun loadAll() = configs.forEach(::load)

    private fun load(config: Config) {
        if (!store.load(config)) {
            store.save(config)
        }
        refreshDynamicColors()
    }

    private fun save(config: Config) = store.save(config)

    fun savePreset(name: String, meta: PresetMetadata = metadataFor(name)) {
        presetStore.save(name, configs, meta)
        metadataCache[name] = meta.copy()
    }

    fun loadPreset(name: String, preferOnline: Boolean = false): Boolean {
        val root = presetRoot(name, preferOnline) ?: return false
        applyPresetRoot(root)
        return true
    }

    fun deletePreset(name: String): Boolean {
        metadataCache.remove(name)
        return presetStore.delete(name)
    }

    fun listPresets(): List<String> =
        (presetStore.list() + remotePresetSource.list()).distinct().sorted()

    fun listLocalPresets(): List<String> = presetStore.list()

    fun listOnlinePresets(): List<String> = remotePresetSource.list().sorted()

    fun isOnlinePreset(name: String): Boolean = name in remotePresetSource.list()

    fun refreshOnlinePresets() {
        remotePresetSource.refreshAsync()
    }

    fun metadataFor(name: String): PresetMetadata =
        metadataCache.getOrPut(name) { PresetMetadata.fromJson(presetRoot(name, false)) }.copy()

    private fun metadataIfKnown(name: String): PresetMetadata? {
        metadataCache[name]?.let { return it }
        val root = presetStore.load(name) ?: remotePresetSource.cached(name) ?: return null
        return PresetMetadata.fromJson(root).also { metadataCache[name] = it }
    }

    fun updateMetadata(name: String, meta: PresetMetadata) {
        metadataCache[name] = meta.copy()
    }

    fun forgetMetadata(name: String) {
        metadataCache.remove(name)
    }

    fun presetsForServer(address: String, preferOnline: Boolean): List<String> {
        val local = presetStore.list().filter { metadataIfKnown(it)?.matchesServer(address) == true }
        val online = remotePresetSource.list().sorted()
            .filter { metadataIfKnown(it)?.matchesServer(address) == true }
        return if (preferOnline) online + local else local + online
    }

    fun snapshot(): JsonObject = JsonObject().apply {
        configs.filter { it.includeInPresets }.forEach { add(it.name, it.serialize()) }
    }

    fun restoreSnapshot(root: JsonObject) {
        configs.forEach { config ->
            if (!config.includeInPresets) return@forEach
            root.getAsJsonObject(config.name)?.let(config::deserialize)
        }
        refreshDynamicColors()
    }

    private fun presetRoot(name: String, preferOnline: Boolean): JsonObject? {
        val local = { presetStore.load(name) }
        val online = { remotePresetSource.load(name) }
        return if (preferOnline) online() ?: local() else local() ?: online()
    }

    fun openPresetFolder() {
        presetStore.openFolder()
    }

    fun refreshDynamicColors() {
        refreshDynamicColorsInternal()
    }

    private fun refreshDynamicColorsInternal() {
        val themeColor = Colour.accent.liveColor(Colour.accent.value)
        val timeSeconds = ColorEntry.chromaTimeSeconds()
        val supportsTheme: (ColorEntry) -> Boolean = { entry -> entry !== Colour.accent }
        configs.forEach { it.refreshDynamicColors(themeColor, timeSeconds, supportsTheme) }
    }

    private fun applyPresetRoot(root: JsonObject) {
        val meta = PresetMetadata.fromJson(root)

        configs.forEach { config ->
            val saved = root.getAsJsonObject(config.name)
            val applied = saved != null && PresetFilter.shouldApply(config, saved, meta)

            if (applied) {
                config.deserialize(saved)
                return@forEach
            }

            if (meta.disableUnaffected) disableUntouched(config)
        }
        refreshDynamicColors()
    }

    private fun disableUntouched(config: Config) {
        if (!config.includeInPresets) return
        val module = config as? Module ?: return
        if (module.isProtected || !module.isEnabled()) return
        module.disable()
    }
}
