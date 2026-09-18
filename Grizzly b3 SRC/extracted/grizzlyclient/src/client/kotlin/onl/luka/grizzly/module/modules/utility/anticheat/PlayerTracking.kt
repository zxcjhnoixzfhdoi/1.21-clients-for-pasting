package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.command.TpsTracker
import onl.luka.grizzly.util.LagManager
import java.util.UUID
import kotlin.math.hypot

internal data class PlayerSnapshot(
    val tick: Long,
    val position: Vec3,
    val delta: Vec3,
    val movementTickDelta: Int,
    val positionUpdateCount: Int,
    val positionSynchronized: Boolean,
    val moved: Boolean,
    val rotated: Boolean,
    val yaw: Float,
    val pitch: Float,
    val yawDelta: Float,
    val pitchDelta: Float,
    val onGround: Boolean,
    val sprinting: Boolean,
    val sneaking: Boolean,
    val usingItem: Boolean,
    val useAnimation: ItemUseAnimation,
    val blocking: Boolean,
    val holdingBlock: Boolean,
    val passenger: Boolean,
    val fallFlying: Boolean,
    val swimming: Boolean,
    val inWater: Boolean,
    val inLava: Boolean,
    val climbing: Boolean,
    val spectator: Boolean,
    val creativeFlying: Boolean,
    val speedAmplifier: Int,
    val slowFalling: Boolean,
    val levitating: Boolean,
    val hurtTime: Int,
    val verticalCollision: Boolean,
    val horizontalCollision: Boolean,
    val surfaceIce: Boolean,
    // Friction of the block the player is standing on: 0.6 normally, up to 0.989 on blue ice
    val slipperiness: Double,
    val supportBelow: Boolean,
    val verticalObstruction: Boolean,
    val insideMovementModifier: Boolean,
    val movementSpeedAttribute: Double,
    val gravityAttribute: Double,
) {
    val horizontalSpeed: Double get() = hypot(delta.x, delta.z) / movementTickDelta.coerceAtLeast(1)

    // Remote sprint metadata and effects can arrive after movement. Keep a conservative
    // floor so those synchronization gaps do not turn ordinary sprinting into speed.
    val effectiveSpeedAttribute: Double
        get() {
            val effectMultiplier = if (speedAmplifier >= 0) 1.0 + 0.2 * (speedAmplifier + 1) else 1.0
            return movementSpeedAttribute.coerceAtLeast(0.13 * effectMultiplier)
        }

    val groundAcceleration: Double
        get() {
            val friction = slipperiness.coerceIn(0.4, 0.99)
            return effectiveSpeedAttribute * (0.6 / friction).let { it * it * it }
        }

    val groundSpeedCeiling: Double
        get() {
            val friction = slipperiness.coerceIn(0.4, 0.99)
            return groundAcceleration / (1.0 - 0.91 * friction).coerceAtLeast(0.01)
        }
}

internal class TrackedPlayer(
    val entityId: Int,
    val uuid: UUID,
    var name: String,
    initialPosition: Vec3,
    initialYaw: Float,
    initialPitch: Float,
    val joinedTick: Long,
) {
    var profileName: String = name
        private set

    var serverPosition: Vec3 = initialPosition
        private set
    var yaw: Float = initialYaw
        private set
    var pitch: Float = initialPitch
        private set
    var onGround: Boolean = false
        private set

    var lastSwingTick: Long = Long.MIN_VALUE
        private set
    var lastDamageTick: Long = Long.MIN_VALUE
        private set
    var lastVelocityTick: Long = Long.MIN_VALUE
        private set
    var lastTeleportTick: Long = joinedTick
        private set
    var lastAttackTick: Long = Long.MIN_VALUE
    var lastPlacementTick: Long = Long.MIN_VALUE
    var lastVictimId: Int = -1
    var sneakTicks: Int = 0
        private set
    var useTicks: Int = 0
        private set
    var airTicks: Int = 0
        private set

    var pendingKnockback: Vec3? = null
        private set

    private var movedThisTick = false
    private var rotatedThisTick = false
    private var positionUpdatesThisTick = 0
    private var positionSynchronizedThisTick = false
    private var lastMovementTick = joinedTick
    private var lastMovementPosition = initialPosition
    private val history = ArrayDeque<PlayerSnapshot>()
    private val swingNanos = ArrayDeque<Long>()

    val latest: PlayerSnapshot? get() = history.lastOrNull()
    val previous: PlayerSnapshot? get() = if (history.size >= 2) history.elementAt(history.size - 2) else null

    fun snapshotAtOrBefore(tick: Long): PlayerSnapshot? =
        history.asReversed().firstOrNull { it.tick <= tick }

    fun snapshotBefore(tick: Long): PlayerSnapshot? =
        history.asReversed().firstOrNull { it.tick < tick }

    fun snapshotsBetween(firstTick: Long, lastTick: Long): List<PlayerSnapshot> =
        history.filter { it.tick in firstTick..lastTick }

    fun relativeMove(position: Vec3?, newYaw: Float?, newPitch: Float?, newOnGround: Boolean) {
        if (position != null) {
            movedThisTick = movedThisTick || position.distanceToSqr(serverPosition) > 1.0E-10
            serverPosition = position
            positionUpdatesThisTick++
        }
        if (newYaw != null && newPitch != null) {
            yaw = newYaw
            pitch = newPitch
            rotatedThisTick = true
        }
        onGround = newOnGround
    }

    fun absoluteMove(position: Vec3, newYaw: Float, newPitch: Float, newOnGround: Boolean, teleport: Boolean, tick: Long) {
        serverPosition = position
        yaw = newYaw
        pitch = newPitch
        onGround = newOnGround
        movedThisTick = true
        positionUpdatesThisTick++
        positionSynchronizedThisTick = true
        rotatedThisTick = true
        if (teleport) lastTeleportTick = tick
    }

    fun markSwing(tick: Long, receivedNanos: Long) {
        lastSwingTick = tick
        swingNanos.addLast(receivedNanos)
        while (swingNanos.size > SWING_HISTORY_SIZE) swingNanos.removeFirst()
    }

    fun markDamage(tick: Long) {
        lastDamageTick = tick
    }

    fun markVelocity(tick: Long, movement: Vec3) {
        lastVelocityTick = tick
        val horizontal = hypot(movement.x, movement.z)
        if (horizontal >= MIN_TRACKED_KNOCKBACK) pendingKnockback = movement
    }

    fun clearKnockback() {
        pendingKnockback = null
    }

    // Arrival times of the most recent arm-swing packets, oldest first, in nanoseconds
    fun swingTimes(): List<Long> = swingNanos.toList()

    fun swungWithin(tick: Long, ticks: Int): Boolean = happenedWithin(lastSwingTick, tick, ticks)

    fun damagedWithin(tick: Long, ticks: Int): Boolean = happenedWithin(lastDamageTick, tick, ticks)

    fun receivedVelocityWithin(tick: Long, ticks: Int): Boolean = happenedWithin(lastVelocityTick, tick, ticks)

    fun capture(client: Minecraft, entity: Player, tick: Long): PlayerSnapshot {
        name = entity.displayName.string
        entity.gameProfile.name?.takeIf { it.isNotBlank() }?.let { profileName = it }
        if (history.isEmpty() && !movedThisTick) {
            serverPosition = entity.position()
            yaw = entity.yRot
            pitch = entity.xRot
            onGround = entity.onGround()
            lastMovementPosition = serverPosition
            lastMovementTick = tick
        }

        val movementTickDelta = maxOf(
            (tick - lastMovementTick).toInt().coerceAtLeast(1),
            positionUpdatesThisTick.coerceAtLeast(1),
        )
        val delta = if (movedThisTick) serverPosition.subtract(lastMovementPosition) else Vec3.ZERO
        val prior = latest
        val level = client.level
        val feet = BlockPos.containing(serverPosition.x, serverPosition.y - 0.2, serverPosition.z)
        val belowState = level?.getBlockState(feet.below())
        val serverBox = entity.boundingBox.move(
            serverPosition.x - entity.x,
            serverPosition.y - entity.y,
            serverPosition.z - entity.z,
        )
        val verticalProbe = serverBox.deflate(0.04, 0.0, 0.04)
        val supportBelow = level?.let { !it.noCollision(verticalProbe.move(0.0, -0.08, 0.0)) } ?: true
        val obstructionAbove = level?.let { !it.noCollision(verticalProbe.move(0.0, 0.08, 0.0)) } ?: true
        val insideMovementModifier = level?.let { world ->
            val minX = Mth.floor(serverBox.minX + 1.0E-4)
            val minY = Mth.floor(serverBox.minY + 1.0E-4)
            val minZ = Mth.floor(serverBox.minZ + 1.0E-4)
            val maxX = Mth.floor(serverBox.maxX - 1.0E-4)
            val maxY = Mth.floor(serverBox.maxY - 1.0E-4)
            val maxZ = Mth.floor(serverBox.maxZ - 1.0E-4)
            var modified = false
            modifierScan@ for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        if (world.getBlockState(BlockPos(x, y, z)).block in MOVEMENT_MODIFIER_BLOCKS) {
                            modified = true
                            break@modifierScan
                        }
                    }
                }
            }
            modified
        } ?: false
        val useStack = entity.useItem.takeUnless { it.isEmpty }
            ?: entity.mainHandItem.takeUnless { it.isEmpty || it.useAnimation == ItemUseAnimation.NONE }
            ?: entity.offhandItem
        val usingItem = entity.isUsingItem

        sneakTicks = if (entity.isCrouching) sneakTicks + 1 else 0
        useTicks = if (usingItem) useTicks + 1 else 0
        airTicks = if (onGround) 0 else airTicks + 1

        val snapshot = PlayerSnapshot(
            tick = tick,
            position = serverPosition,
            delta = delta,
            movementTickDelta = movementTickDelta,
            positionUpdateCount = positionUpdatesThisTick,
            positionSynchronized = positionSynchronizedThisTick,
            moved = movedThisTick,
            rotated = rotatedThisTick,
            yaw = yaw,
            pitch = pitch,
            yawDelta = prior?.let { Mth.wrapDegrees(yaw - it.yaw) } ?: 0f,
            pitchDelta = prior?.let { pitch - it.pitch } ?: 0f,
            onGround = onGround,
            sprinting = entity.isSprinting,
            sneaking = entity.isCrouching,
            usingItem = usingItem,
            useAnimation = if (usingItem) useStack.useAnimation else ItemUseAnimation.NONE,
            blocking = entity.isBlocking,
            holdingBlock = entity.mainHandItem.item is BlockItem || entity.offhandItem.item is BlockItem,
            passenger = entity.isPassenger,
            fallFlying = entity.isFallFlying,
            swimming = entity.isSwimming,
            inWater = entity.isInWater,
            inLava = entity.isInLava,
            climbing = entity.onClimbable(),
            spectator = entity.isSpectator,
            creativeFlying = entity.abilities.flying,
            speedAmplifier = entity.getEffect(MobEffects.SPEED)?.amplifier ?: -1,
            slowFalling = entity.hasEffect(MobEffects.SLOW_FALLING),
            levitating = entity.hasEffect(MobEffects.LEVITATION),
            hurtTime = entity.hurtTime,
            verticalCollision = entity.verticalCollision,
            horizontalCollision = entity.horizontalCollision,
            surfaceIce = belowState?.`is`(BlockTags.ICE) == true,
            slipperiness = when {
                belowState == null -> 0.6
                belowState.`is`(Blocks.BLUE_ICE) -> 0.989
                belowState.`is`(BlockTags.ICE) -> 0.98
                belowState.`is`(Blocks.SLIME_BLOCK) -> 0.8
                else -> 0.6
            },
            supportBelow = supportBelow,
            verticalObstruction = supportBelow || obstructionAbove,
            insideMovementModifier = insideMovementModifier,
            movementSpeedAttribute = entity.getAttributeValue(Attributes.MOVEMENT_SPEED),
            gravityAttribute = entity.getAttributeValue(Attributes.GRAVITY),
        )

        history.addLast(snapshot)
        while (history.size > HISTORY_SIZE) history.removeFirst()
        if (movedThisTick) {
            lastMovementTick = tick
            lastMovementPosition = serverPosition
        }
        movedThisTick = false
        rotatedThisTick = false
        positionUpdatesThisTick = 0
        positionSynchronizedThisTick = false
        return snapshot
    }

    fun movementSamples(limit: Int): List<PlayerSnapshot> =
        history.asReversed().asSequence().filter { it.moved }.take(limit).toList().asReversed()

    fun snapshots(limit: Int): List<PlayerSnapshot> = history.takeLast(limit)

    fun isInGrace(tick: Long, joinGraceTicks: Int): Boolean =
        tick - joinedTick < joinGraceTicks || tick - lastTeleportTick < TELEPORT_GRACE_TICKS

    companion object {
        private const val HISTORY_SIZE = 80
        private const val SWING_HISTORY_SIZE = 40
        private const val TELEPORT_GRACE_TICKS = 12
        private const val MIN_TRACKED_KNOCKBACK = 0.12
        private val MOVEMENT_MODIFIER_BLOCKS = setOf(
            Blocks.COBWEB,
            Blocks.POWDER_SNOW,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.BUBBLE_COLUMN,
            Blocks.SCAFFOLDING,
            Blocks.HONEY_BLOCK,
            Blocks.SOUL_SAND,
        )

        private fun happenedWithin(eventTick: Long, currentTick: Long, ticks: Int): Boolean =
            eventTick != Long.MIN_VALUE && currentTick >= eventTick && currentTick - eventTick <= ticks
    }
}

internal class LagMonitor {
    private var lagUntilTick = Long.MIN_VALUE

    fun update(tick: Long, snapshots: Collection<PlayerSnapshot>) {
        val moving = snapshots.filter { it.moved }
        if (moving.size >= 3) {
            val burstCount = moving.count { it.delta.lengthSqr() > 1.0 }
            if (burstCount * 2 >= moving.size) lagUntilTick = tick + 20
        }
        if (TpsTracker.current() < 18.0) lagUntilTick = maxOf(lagUntilTick, tick + 20)
        if (LagManager.getIncomingQueueSize() > 0 || LagManager.flushingIncoming) {
            lagUntilTick = maxOf(lagUntilTick, tick + 10)
        }
    }

    fun isLagging(tick: Long): Boolean = tick <= lagUntilTick

    fun reset() {
        lagUntilTick = Long.MIN_VALUE
    }
}
