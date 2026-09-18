package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

object Misplace : Module(
    "Misplace",
    "Pulls players toward you so you can hit from further away",
    Category.COMBAT,
) {

    enum class Mode {
        NORMAL,
        LAG
    }

    val mode = enum("mode", Mode.NORMAL)

    private val amount = float("amount", 0.1f, 0.01f, 1.0f).also {
        it.visibleWhen = { mode.value == Mode.NORMAL }
    }
    private val maxRange = float("max range", 6.0f, 1.0f, 12.0f).also {
        it.visibleWhen = { mode.value == Mode.NORMAL }
    }
    private val disadvantage = boolean("disadvantage", false).also {
        it.visibleWhen = { mode.value == Mode.NORMAL }
    }

    val lagWindow = intRange("lag window (ms)", 200 to 300, 0, 1000).also {
        it.visibleWhen = { mode.value == Mode.LAG }
    }
    private val minDistance = float("min distance", 3.1f, 0.0f, 8.0f).also {
        it.visibleWhen = { mode.value == Mode.LAG }
    }
    val drainPerTick = int("drain per tick", 2, 1, 10).also {
        it.visibleWhen = { mode.value == Mode.LAG }
    }

    val onlyPlayers = boolean("only players", true)

    private class Shifted(val position: Vec3, val old: Vec3)

    private val shifted = HashMap<Int, Shifted>()

    private class ServerTrack(var previous: Vec3, var latest: Vec3)

    private val serverPositions = ConcurrentHashMap<Int, ServerTrack>()

    private var lagTargetId = -1

    @Volatile private var lastSentPosition: Vec3? = null

    @Volatile var lagActive = false
        private set
    @Volatile var lagUntilMs = 0L
        private set
    @Volatile private var lagDurationMs = 0L
    @Volatile private var lagHitCount = 0

    // Normal

    @JvmStatic
    fun applyForFrame() {
        if (!isEnabled() || mode.value != Mode.NORMAL) return
        // A window that never got closed must not be shifted again, or the offsets compound.
        if (shifted.isNotEmpty()) return
        val client = Minecraft.getInstance()
        val self = client.player ?: return
        val level = client.level ?: return

        for (entity in level.entitiesForRendering()) {
            if (entity === self || entity !is LivingEntity) continue
            if (onlyPlayers.value && entity !is Player) continue

            val offset = misplaceOffset(self, entity) ?: continue
            shifted[entity.id] = Shifted(entity.position(), Vec3(entity.xOld, entity.yOld, entity.zOld))

            entity.setPos(entity.x - offset.x, entity.y, entity.z - offset.z)
            entity.xOld -= offset.x
            entity.zOld -= offset.z
        }
    }

    @JvmStatic
    fun restoreAfterFrame() {
        if (shifted.isEmpty()) return
        val level = Minecraft.getInstance().level

        for ((id, saved) in shifted) {
            val entity = level?.getEntity(id) ?: continue
            entity.setPos(saved.position)
            entity.xOld = saved.old.x
            entity.yOld = saved.old.y
            entity.zOld = saved.old.z
        }
        shifted.clear()
    }

    private fun misplaceOffset(self: LocalPlayer, target: Entity): Vec3? {
        val distance = hypot(self.x - target.x, self.z - target.z)
        if (distance > maxRange.value) return null

        var shift = amount.value.toDouble()
        // Wound back as they close, so it never yanks the hitbox onto or through us.
        val remaining = distance - shift
        if (remaining < 0.5) {
            shift += remaining - 0.5
            if (shift < 0.0) shift = 0.0
        }
        if (shift <= 0.0) return null

        val direction = if (disadvantage.value) -90.0f else 90.0f
        val radians = Math.toRadians((angleTo(self.x, self.z, target.x, target.z) + direction).toDouble())
        return Vec3(cos(radians) * shift, 0.0, sin(radians) * shift)
    }

    private fun angleTo(sourceX: Double, sourceZ: Double, targetX: Double, targetZ: Double): Float {
        val deltaX = targetX - sourceX
        val deltaZ = targetZ - sourceZ
        return when {
            deltaZ < 0.0 && deltaX < 0.0 -> (90.0 + Math.toDegrees(atan(deltaZ / deltaX))).toFloat()
            deltaZ < 0.0 && deltaX > 0.0 -> (-90.0 + Math.toDegrees(atan(deltaZ / deltaX))).toFloat()
            else -> Math.toDegrees(-atan(deltaX / deltaZ)).toFloat()
        }
    }

    // Lag

    @JvmStatic
    fun onOutgoingSend(packet: Packet<*>) {
        if (packet !is ServerboundMovePlayerPacket || !packet.hasPosition()) return
        val player = Minecraft.getInstance().player ?: return
        lastSentPosition = Vec3(packet.getX(player.x), packet.getY(player.y), packet.getZ(player.z))
    }

    @JvmStatic
    fun offsetRenderState(entity: Entity, state: EntityRenderState) {
        if (!isEnabled() || mode.value != Mode.LAG || !lagActive) return
        if (entity !is LivingEntity) return
        if (onlyPlayers.value && entity !is Player) return

        val self = Minecraft.getInstance().player ?: return
        if (entity === self) return

        val sent = lastSentPosition ?: return
        val offset = self.position().subtract(sent)
        if (offset.lengthSqr() < 1.0e-6) return

        state.x += offset.x
        state.y += offset.y
        state.z += offset.z
    }

    @JvmStatic
    fun onServerPosition(entityId: Int, position: Vec3) {
        // The packet handlers run once on the netty thread before they hand off, and a second
        // sample from that pass would overwrite the previous position with the current one.
        if (!Minecraft.getInstance().isSameThread) return

        val track = serverPositions[entityId]
        if (track == null) {
            serverPositions[entityId] = ServerTrack(position, position)
            return
        }
        track.previous = track.latest
        track.latest = position
    }

    @JvmStatic
    fun clearTracking() {
        serverPositions.clear()
        lagTargetId = -1
        lastSentPosition = null
    }

    @JvmStatic
    fun onHit(entityId: Int) {
        if (!isEnabled() || mode.value != Mode.LAG) return
        val client = Minecraft.getInstance()
        val self = client.player ?: return
        val target = client.level?.getEntity(entityId) ?: return
        if (!shouldLagFor(self, target)) return

        lagTargetId = entityId
        triggerLag()
    }

    private fun shouldLagFor(self: LocalPlayer, target: Entity): Boolean {
        if (onlyPlayers.value && target !is Player) return false
        // Inside your own reach there is nothing to gain, and holding packets there just
        // desyncs a fight you were already winning.
        if (self.position().distanceTo(serverPosition(target)) < minDistance.value) return false
        return !isClosing(self, target)
    }

    private fun serverPosition(target: Entity): Vec3 =
        serverPositions[target.id]?.latest ?: target.position()

    private fun isClosing(self: LocalPlayer, target: Entity): Boolean {
        val track = serverPositions[target.id] ?: return false

        val toSelfX = self.x - track.latest.x
        val toSelfZ = self.z - track.latest.z
        val length = sqrt(toSelfX * toSelfX + toSelfZ * toSelfZ)
        if (length < 1.0e-4) return true

        val movedX = track.latest.x - track.previous.x
        val movedZ = track.latest.z - track.previous.z
        return (movedX * toSelfX + movedZ * toSelfZ) / length > CLOSING_SPEED
    }

    private fun triggerLag() {
        val now = System.currentTimeMillis()
        if (!lagActive) lagHitCount = 0
        lagHitCount++

        lagDurationMs = maxOf(lagDurationMs, currentLagWindowMs())
        lagActive = true
        lagUntilMs = maxOf(lagUntilMs, now + lagDurationMs)
    }

    private fun currentLagWindowMs(): Long {
        val (lo, hi) = lagWindow.value
        if (hi <= lo) return lo.toLong()
        val extra = maxOf(0, lagHitCount - 1) * LAG_STEP_MS
        return minOf(hi.toLong(), lo.toLong() + extra)
    }

    override fun onTick(client: Minecraft) {
        if (mode.value != Mode.LAG) return
        val level = client.level
        if (level == null) {
            clearTracking()
            stopLag()
            return
        }
        serverPositions.keys.removeIf { level.getEntity(it) == null }

        if (!lagActive) return
        if (System.currentTimeMillis() >= lagUntilMs) {
            stopLag()
            return
        }

        // Re-checked every tick, so someone who turns and charges cuts the window short
        // instead of riding it out.
        val self = client.player
        val target = level.getEntity(lagTargetId)
        if (self == null || target == null || !shouldLagFor(self, target)) stopLag()
    }

    override fun onDisabled() {
        restoreAfterFrame()
        stopLag()
    }

    private fun stopLag() {
        lagTargetId = -1
        lastSentPosition = null
        lagActive = false
        lagUntilMs = 0L
        lagDurationMs = 0L
        lagHitCount = 0
    }

    override fun hudInfo(): String = when (mode.value) {
        Mode.NORMAL -> "%.2f".format(amount.value)
        Mode.LAG -> {
            val (lo, hi) = lagWindow.value
            if (hi > lo) "lag ${lo}-${hi}ms" else "lag ${lo}ms"
        }
    }

    private const val LAG_STEP_MS = 50L
    private const val CLOSING_SPEED = 0.02
}
