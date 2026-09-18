package onl.luka.grizzly.module

import com.google.gson.JsonObject
import onl.luka.grizzly.config.Config
import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.EnumEntry
import onl.luka.grizzly.config.entry.KeybindEntry
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.HudDropShadowSettings
import onl.luka.grizzly.util.HudShadowShape
import onl.luka.grizzly.util.NotificationManager
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

/**
 * Base class for all client modules. Extends [Config] so that every module
 * owns its own serialised config file.
 *
 * ### Subclass hooks  (override to react inside the module)
 * ```kotlin
 * override fun onEnabled()  { ... }
 * override fun onDisabled() { ... }
 * override fun onTick(client: Minecraft) { ... }
 * override fun onLevelRender(ctx: LevelRenderContext) { ... }
 * override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) { ... }
 * ```
 *
 * ### External event subscriptions  (for code outside the module)
 * ```kotlin
 * SprintModule.onEnable  { ... }
 * SprintModule.onDisable { ... }
 * SprintModule.onTick    { client -> ... }
 * SprintModule.onLevelRender { ctx -> ... }
 * SprintModule.onHudRender   { extractor, delta -> ... }
 * ```
 *
 * [ModuleManager.register] automatically passes the module to [onl.luka.grizzly.config.ConfigManager]
 * so you only ever need to call `ModuleManager.register(MyModule)`.
 */
enum class HudLayer { UNDERLAY, OVERLAY }

abstract class Module(
    name: String,
    val description: String,
    val category: Category,
) : Config(name) {

    /** Protected modules are always enabled and cannot be toggled or key-bound. */
    open val isProtected: Boolean = false

    /** When false this module is never listed in the ModulesListHUD overlay. */
    open val showInModulesList: Boolean = true

    /** When false keybind toggles do not create notification toasts. */
    open val showKeybindNotifications: Boolean = true

    /** Where this module's HUD drawing sits relative to the vanilla HUD. */
    open val hudLayer: HudLayer = HudLayer.OVERLAY

    val enabled = boolean("enabled", false)
    val keybind = keybind("keybind", GLFW.GLFW_KEY_UNKNOWN)
        .onPress { if (!isProtected) profilePressed(0) }

    private val _profiles = mutableListOf(ModuleProfile("base", keybind))

    /** Index 0 is the module itself; the rest are alternate configurations of it. */
    val profiles: List<ModuleProfile> get() = _profiles

    var activeProfile = 0
        private set

    private val enableListeners      = mutableListOf<() -> Unit>()
    private val disableListeners     = mutableListOf<() -> Unit>()
    private val tickListeners        = mutableListOf<(Minecraft) -> Unit>()
    private val levelRenderListeners = mutableListOf<(LevelRenderContext) -> Unit>()
    private val hudRenderListeners   = mutableListOf<(GuiGraphicsExtractor, DeltaTracker) -> Unit>()

    /** Subscribe a listener that fires each time this module is enabled. */
    fun onEnable(handler: () -> Unit): Module  { enableListeners  += handler; return this }
    /** Subscribe a listener that fires each time this module is disabled. */
    fun onDisable(handler: () -> Unit): Module { disableListeners += handler; return this }
    /** Subscribe a listener that fires every tick while the module is enabled. */
    fun onTick(handler: (Minecraft) -> Unit): Module { tickListeners += handler; return this }
    /** Subscribe a listener that fires each level-render frame while the module is enabled. */
    fun onLevelRender(handler: (LevelRenderContext) -> Unit): Module { levelRenderListeners += handler; return this }
    /** Subscribe a listener that fires each HUD-render frame while the module is enabled. */
    fun onHudRender(handler: (GuiGraphicsExtractor, DeltaTracker) -> Unit): Module { hudRenderListeners += handler; return this }

    private var fontEntry: EnumEntry<Font.FontOverride>? = null

    protected fun registerFontOverride(): EnumEntry<Font.FontOverride> =
        enum("font", Font.FontOverride.DEFAULT).also { fontEntry = it }

    // A no-op for modules that never registered an override, so it is safe to wrap any render in.
    fun <T> withFont(block: () -> T): T = Font.withOverride(fontEntry?.value?.choice, block)

    protected fun hudDropShadow(
        stepped: Boolean = false,
        availableWhen: () -> Boolean = { true },
    ): HudDropShadowSettings {
        val shadowGroup = group("Drop Shadow")
        val enabled = boolean("drop shadow", false).also {
            it.visibleWhen = availableWhen
        }.inGroup(shadowGroup)
        val controlsVisible = { availableWhen() && enabled.value }
        val shape = if (!stepped) null else enum("drop shadow shape", HudShadowShape.STEPPED).also {
            it.visibleWhen = controlsVisible
        }.inGroup(shadowGroup)

        return HudDropShadowSettings(
            enabled = enabled,
            shape = shape,
            color = color("drop shadow color", Color(0, 0, 0, 150), allowAlpha = true).also {
                it.visibleWhen = controlsVisible
            }.inGroup(shadowGroup),
            softness = int("drop shadow softness", 2, 1, 16).also {
                it.visibleWhen = controlsVisible
            }.inGroup(shadowGroup),
            spread = int("drop shadow spread", 0, 0, 8).also {
                it.visibleWhen = controlsVisible
            }.inGroup(shadowGroup),
            offsetX = int("drop shadow x", 0, -8, 8).also {
                it.visibleWhen = controlsVisible
            }.inGroup(shadowGroup),
            offsetY = int("drop shadow y", 0, -8, 8).also {
                it.visibleWhen = controlsVisible
            }.inGroup(shadowGroup),
        )
    }

    protected open fun onEnabled() {}
    protected open fun onDisabled() {}
    protected open fun onTick(client: Minecraft) {}
    protected open fun onLevelRender(ctx: LevelRenderContext) {}
    protected open fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {}

    open fun isEnabled(): Boolean = enabled.value

    open fun hudInfo(): String = ""

    open fun hudInfoColor(): Int = (255 shl 24) or (170 shl 16) or (170 shl 8) or 170

    fun toggle() { if (enabled.value) disable() else enable() }

    fun enable() {
        if (enabled.value) return
        enabled.value = true
        onEnabled()
        enableListeners.forEach { it() }
    }

    fun disable() {
        if (!enabled.value) return
        enabled.value = false
        onDisabled()
        disableListeners.forEach { it() }
    }

    internal fun dispatchTick(client: Minecraft) {
        if (!isEnabled()) return
        onTick(client)
        tickListeners.forEach { it(client) }
    }

    internal fun dispatchLevelRender(ctx: LevelRenderContext) {
        if (!isEnabled()) return
        onLevelRender(ctx)
        levelRenderListeners.forEach { it(ctx) }
    }

    internal fun dispatchHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        if (!isEnabled()) return
        onHudRender(extractor, delta)
        hudRenderListeners.forEach { it(extractor, delta) }
    }

    /**
     * Adds another configuration of this module, seeded from whatever is currently set, with a
     * key of its own. Its press handler is registered here so the new bind is live immediately.
     */
    fun addProfile(name: String = "profile ${_profiles.size}"): ModuleProfile {
        val bind = KeybindEntry("profile_${_profiles.size}", GLFW.GLFW_KEY_UNKNOWN)
        val profile = ModuleProfile(name, bind, snapshot())

        // Looked up when the key is pressed rather than captured now: deleting an earlier
        // profile shifts every later one down, and a captured index would then fire the wrong
        // configuration or fall off the end of the list entirely.
        bind.onPress {
            if (!isProtected) profilePressed(_profiles.indexOfFirst { it.keybind === bind })
        }

        _profiles += profile
        return profile
    }

    fun removeProfile(index: Int) {
        if (index <= 0 || index >= _profiles.size) return
        _profiles.removeAt(index)
        when {
            // Deleting what we are running means the live entries still hold its values, so the
            // profile we fall back to has to be loaded or the module keeps a configuration that
            // no longer exists anywhere.
            activeProfile == index -> {
                activeProfile = 0
                _profiles[0].values.takeIf { it.size() > 0 }?.let { super.deserialize(it) }
            }
            activeProfile > index -> activeProfile--
        }
    }

    /**
     * The agreed rule: a key whose configuration is already running turns the module off.
     * Anything else switches to that configuration and turns it on, so the binds behave like
     * radio buttons while the module is up and like a toggle when it is the one already active.
     */
    private fun profilePressed(index: Int) {
        if (index !in _profiles.indices) return

        if (enabled.value && activeProfile == index) {
            disable()
            if (showKeybindNotifications) NotificationManager.showModuleToggle(this, false)
            return
        }

        // Getting here while already on means the key belongs to a different profile, so this
        // press is a change of configuration and saying "Enabled" again would tell you nothing.
        val changingProfile = enabled.value

        switchToProfile(index)
        if (!enabled.value) enable()
        if (showKeybindNotifications) {
            if (changingProfile) {
                NotificationManager.showProfileSwitch(this, _profiles[index].name)
            } else {
                NotificationManager.showModuleToggle(this, true)
            }
        }
    }

    /** Banks the live values back into the profile we are leaving before loading the new one. */
    fun switchToProfile(index: Int) {
        if (index !in _profiles.indices || index == activeProfile) return

        _profiles[activeProfile].values = snapshot()
        activeProfile = index
        // Entries only. Going through our own override would let a stored blob rebuild the
        // profile list while we are in the middle of walking it.
        val values = _profiles[index].values
        if (values.size() > 0) super.deserialize(values)
    }

    /**
     * Everything except the enabled flag and the binds, which belong to the profile itself.
     * Deliberately [Config.serialize] and not our own override: ours embeds the profile list,
     * which would nest a copy of every profile inside every profile and, since writing that list
     * needs a snapshot of its own, recurse until the stack ran out.
     */
    private fun snapshot(): JsonObject = super.serialize().also {
        it.remove("enabled")
        it.remove("keybind")
    }

    /** [live] is the running configuration, which belongs to whichever profile is active. */
    private fun profilesToJson(live: JsonObject): JsonObject = JsonObject().also { root ->
        _profiles.forEachIndexed { index, profile ->
            if (index == 0) return@forEachIndexed
            root.add(
                index.toString(),
                JsonObject().also { entry ->
                    entry.addProperty("name", profile.name)
                    entry.addProperty("keybind", profile.keybind.value)
                    entry.add("values", if (index == activeProfile) live else profile.values)
                },
            )
        }
        // The module's own entries always hold whichever profile is running, so the base
        // configuration needs somewhere of its own to live or it is lost the moment you save
        // while standing on another profile.
        root.add(BASE_VALUES_KEY, if (activeProfile == 0) live else _profiles[0].values)
        root.addProperty("active", activeProfile)
    }

    private fun profilesFromJson(root: JsonObject) {
        while (_profiles.size > 1) _profiles.removeAt(_profiles.size - 1)
        activeProfile = 0

        var index = 1
        while (root.has(index.toString())) {
            val entry = root.getAsJsonObject(index.toString())
            val profile = addProfile(entry.get("name")?.asString ?: "profile $index")
            profile.keybind.value = entry.get("keybind")?.asInt ?: GLFW.GLFW_KEY_UNKNOWN
            profile.values = entry.getAsJsonObject("values") ?: JsonObject()
            index++
        }

        root.getAsJsonObject(BASE_VALUES_KEY)?.let { _profiles[0].values = it }

        val active = root.get("active")?.asInt ?: 0
        if (active in _profiles.indices) activeProfile = active
    }

    override fun tickKeybinds() {
        super.tickKeybinds()
        for (index in 1 until _profiles.size) _profiles[index].keybind.tick()
    }

    override fun serialize(): JsonObject = super.serialize().also { json ->
        if (_profiles.size > 1) json.add(PROFILES_KEY, profilesToJson(snapshot()))
    }

    override fun deserialize(json: JsonObject) {
        super.deserialize(json)
        json.getAsJsonObject(PROFILES_KEY)?.let(::profilesFromJson)
    }

    enum class Category {
        COMBAT, ANARCHY, MOVEMENT, RENDER, PLAYER, WORLD, UTILITY, MINIGAMES, SKYBLOCK, HUD, OTHER, EXPLOITS
    }

    private companion object {
        const val PROFILES_KEY = "profiles"
        const val BASE_VALUES_KEY = "base"
    }
}
