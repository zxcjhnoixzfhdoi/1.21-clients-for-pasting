package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque

object AntiVoid : Module(
    name = "Anti Void",
    description = "Prevents you from falling into the void",
    category = Category.MOVEMENT,
) {
    enum class Mode {
        SETBACK, BOUNCE
    }

    val mode = enum("mode", Mode.SETBACK)
    private val triggerDistance = float("fall distance", 3.0f, 0.5f, 20.0f)
    private val rewindTicks = int("rewind ticks", 3, 0, 20).also {
        it.visibleWhen = { mode.value == Mode.SETBACK }
    }
    private val bounceMotion = float("bounce motion", 0.8f, 0.1f, 2.0f).also {
        it.visibleWhen = { mode.value == Mode.BOUNCE }
    }
    private val cooldown = int("cooldown", 10, 1, 40)
    private val disableAfter = boolean("disable after", false)

    private data class SafePosition(val position: Vec3)

    private val safePositions = ArrayDeque<SafePosition>()
    private var trackedLevel: ClientLevel? = null
    private var lastObservedPosition: Vec3? = null
    private var cooldownTicks = 0

    override fun onEnabled() {
        resetTracking(Minecraft.getInstance().level)
    }

    override fun onTick(client: Minecraft) {
        val player = client.player
        val level = client.level
        if (player == null || level == null) {
            resetTracking(null)
            return
        }

        if (level !== trackedLevel) {
            resetTracking(level)
        }

        val currentPosition = player.position()
        val previousPosition = lastObservedPosition
        lastObservedPosition = currentPosition
        if (previousPosition != null && previousPosition.distanceToSqr(currentPosition) > TELEPORT_RESET_DISTANCE_SQR) {
            safePositions.clear()
        }

        val playerBlock = BlockPos.containing(currentPosition)
        if (!canProtect(player) || !level.chunkSource.hasChunk(playerBlock.x shr 4, playerBlock.z shr 4)) {
            safePositions.clear()
            cooldownTicks = 0
            return
        }

        if (player.onGround()) {
            if (hasCollisionBelow(level, player)) rememberSafePosition(currentPosition)
            cooldownTicks = 0
            return
        }

        if (cooldownTicks > 0) {
            cooldownTicks--
            return
        }

        if (player.deltaMovement.y >= 0.0
            || player.fallDistance < triggerDistance.value
            || hasCollisionBelow(level, player)
        ) {
            return
        }

        val recovered = when (mode.value) {
            Mode.SETBACK -> setback(level, player) || bounce(player, FALLBACK_BOUNCE_MOTION)
            Mode.BOUNCE -> bounce(player, bounceMotion.value.toDouble())
        }

        if (!recovered) return
        cooldownTicks = cooldown.value
        if (disableAfter.value) disable()
    }

    private fun canProtect(player: LocalPlayer): Boolean =
        player.isAlive
            && !player.isCreative
            && !player.isSpectator
            && !player.abilities.flying
            && !player.isFallFlying
            && !player.isPassenger

    private fun rememberSafePosition(position: Vec3) {
        safePositions.addLast(SafePosition(position))
        while (safePositions.size > SAFE_HISTORY_SIZE) safePositions.removeFirst()
    }

    private fun setback(level: ClientLevel, player: LocalPlayer): Boolean {
        if (safePositions.isEmpty()) return false

        val positions = safePositions.toList()
        val index = (positions.lastIndex - rewindTicks.value).coerceAtLeast(0)
        val safe = positions[index].position
        val offset = safe.subtract(player.position())
        if (!level.noCollision(player.boundingBox.move(offset))) return false

        player.setPos(safe.x, safe.y, safe.z)
        player.setDeltaMovement(Vec3.ZERO)
        player.fallDistance = 0.0
        player.setOnGround(true)
        player.connection.send(
            ServerboundMovePlayerPacket.Pos(
                safe.x,
                safe.y,
                safe.z,
                true,
                player.horizontalCollision,
            ),
        )
        lastObservedPosition = safe
        return true
    }

    private fun bounce(player: LocalPlayer, motion: Double): Boolean {
        val velocity = player.deltaMovement
        player.setDeltaMovement(velocity.x, motion, velocity.z)
        player.fallDistance = 0.0
        return true
    }

    private fun hasCollisionBelow(level: ClientLevel, player: LocalPlayer): Boolean {
        val box = player.boundingBox
        val bottom = box.minY - COLLISION_EPSILON
        val worldBottom = level.minY.toDouble()
        if (bottom <= worldBottom) return false

        val searchBox = AABB(
            box.minX + COLLISION_EPSILON,
            worldBottom,
            box.minZ + COLLISION_EPSILON,
            box.maxX - COLLISION_EPSILON,
            bottom,
            box.maxZ - COLLISION_EPSILON,
        )
        return level.getBlockCollisions(null, searchBox).iterator().hasNext()
    }

    private fun resetTracking(level: ClientLevel?) {
        trackedLevel = level
        safePositions.clear()
        lastObservedPosition = null
        cooldownTicks = 0
    }

    override fun onDisabled() {
        resetTracking(null)
    }

    override fun hudInfo(): String = mode.value.name.lowercase()

    private const val SAFE_HISTORY_SIZE = 24
    private const val TELEPORT_RESET_DISTANCE_SQR = 256.0
    private const val COLLISION_EPSILON = 1.0E-4
    private const val FALLBACK_BOUNCE_MOTION = 0.8
}
