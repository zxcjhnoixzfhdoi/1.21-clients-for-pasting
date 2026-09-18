package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.ai.attributes.Attributes
import java.lang.reflect.Method

// Works out whether the current server runs 1.8-style combat, for CheatDetector.Protocol.AUTO
internal object ProtocolDetector {

    // 20 / 16 ticks of cooldown is 1.25 ticks, i.e
    private const val LEGACY_ATTACK_SPEED = 16.0

    private var legacy = false
    private var reason = "modern combat"
    private var lastRefreshTick = Long.MIN_VALUE

    fun refresh(client: Minecraft, tick: Long) {
        if (lastRefreshTick != Long.MIN_VALUE && tick - lastRefreshTick < REFRESH_INTERVAL_TICKS) return
        lastRefreshTick = tick

        negotiatedProtocol(client)?.takeIf { it.isLegacy }?.let { protocol ->
            legacy = true
            reason = "negotiated protocol ${protocol.name}"
            return
        }

        val host = client.getCurrentServer()?.ip?.lowercase().orEmpty()
        val matched = KNOWN_LEGACY_HOSTS.firstOrNull { host.contains(it) }
        if (matched != null) {
            legacy = true
            reason = "host matches '$matched'"
            return
        }

        // Keep the previous verdict rather than flapping to modern while the
        // player is null or their attributes have not synced yet.
        val attackSpeed = client.player?.getAttributeBaseValue(Attributes.ATTACK_SPEED) ?: return
        if (attackSpeed >= LEGACY_ATTACK_SPEED) {
            legacy = true
            reason = "attack cooldown removed (speed ${"%.0f".format(attackSpeed)})"
            return
        }
        legacy = false
        reason = "modern combat"
    }

    private fun negotiatedProtocol(client: Minecraft): DetectedProtocol? {
        val connection = client.connection?.connection ?: return null
        val lookup = viaFabricPlusLookup ?: return null
        val version = runCatching { lookup.method.invoke(lookup.instance, connection) }.getOrNull() ?: return null
        val versionClass = version.javaClass
        val id = runCatching {
            (versionClass.getMethod("getVersion").invoke(version) as? Number)?.toInt()
        }.getOrNull()
        val name = runCatching {
            versionClass.getMethod("getName").invoke(version)?.toString()
        }.getOrNull() ?: version.toString()

        return DetectedProtocol(
            name = name,
            isLegacy = (id != null && id in LEGACY_PROTOCOL_IDS) ||
                LEGACY_VERSION_PATTERN.containsMatchIn(name),
        )
    }

    fun isLegacy(): Boolean = legacy

    fun describe(): String = reason

    fun reset() {
        legacy = false
        reason = "modern combat"
        lastRefreshTick = Long.MIN_VALUE
    }

    private data class ViaFabricPlusLookup(val instance: Any, val method: Method)

    private data class DetectedProtocol(val name: String, val isLegacy: Boolean)

    private val viaFabricPlusLookup: ViaFabricPlusLookup? by lazy {
        runCatching {
            val implementationClass = Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl")
            val instance = implementationClass.getField("INSTANCE").get(null)
            val method = implementationClass.methods.first {
                it.name == "getTargetVersion" &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0].isAssignableFrom(net.minecraft.network.Connection::class.java)
            }
            ViaFabricPlusLookup(instance, method)
        }.getOrNull()
    }

    private val KNOWN_LEGACY_HOSTS = listOf("hypixel")
    private val LEGACY_PROTOCOL_IDS = 4..47
    private val LEGACY_VERSION_PATTERN =
        Regex("""(?i)(?:^|[^0-9])1\.(?:7|8)(?:\.(?:\d+|x))?(?:[^0-9]|$)""")
    private const val REFRESH_INTERVAL_TICKS = 20L
}
