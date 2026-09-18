package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.AABB
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object KnockbackDisplacement : Module(
    "Hit Flick",
    "Silently flicks rotation on each attack to displace knockback sideways",
    Category.COMBAT
) {
    private const val ROTATION_OWNER = "knockback_displacement"
    private const val VOID_YAW_STEP = 2f
    private const val VOID_YAW_STEPS = 180
    private const val VOID_PATH_STEP = 0.15
    private const val MIN_VOID_RUN = 0.45

    enum class FlickMode { LEFT, RIGHT, RANDOM, STRAFE, STRAFE_INVERTED, VOID }

    private val flickMode = enum("flick mode", FlickMode.RIGHT)
    private val flickAngle = floatRange("flick angle", 85f to 95f, 10f, 180f).also {
        it.visibleWhen = { flickMode.value != FlickMode.VOID }
    }
    private val voidCheckDistance = float("void check distance", 2f, 0.5f, 5f).also {
        it.visibleWhen = { flickMode.value == FlickMode.VOID }
    }
    private val voidDepth = int("void depth", 6, 2, 32).also {
        it.visibleWhen = { flickMode.value == FlickMode.VOID }
    }
    private val requireSprint = boolean("require sprint", true)
    private val cooldownMs = int("cooldown (ms)", 250, 0, 2000)
    private val autoKb = boolean("auto kb", false)

    @JvmField
    var skipIntercept = false

    var rotationHeld = false
        private set

    private var autoKbAttack = false
    private var pendingAttack: PendingAttack? = null
    private var suppressNextSwing = false
    private var suppressSwingTick = Int.MIN_VALUE
    private var lastFlickTime = 0L

    private data class PendingAttack(
        val player: LocalPlayer,
        val target: LivingEntity,
        val gameMode: MultiPlayerGameMode,
        val scheduledTick: Int,
    )

    private data class VoidTrajectory(
        val yaw: Float,
        val edgeDistance: Double,
        val voidRun: Double,
        val averageDepth: Double,
        val facingDeviation: Float,
    ) {
        val score: Double
            get() =
                4.0 / (edgeDistance + 0.25) +
                    voidRun * 2.0 +
                    averageDepth * 0.05 -
                    facingDeviation * 0.001
    }

    override fun onEnabled() {
        rotationHeld = false
        skipIntercept = false
        autoKbAttack = false
        pendingAttack = null
        suppressNextSwing = false
        suppressSwingTick = Int.MIN_VALUE
        lastFlickTime = 0L
    }

    override fun onDisabled() {
        clearHeldRotation()
        skipIntercept = false
        autoKbAttack = false
        pendingAttack = null
        suppressNextSwing = false
        suppressSwingTick = Int.MIN_VALUE
    }

    init {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            if (!isEnabled()) return@register

            val pending = pendingAttack
            if (pending != null) {
                if (pending.player.tickCount <= pending.scheduledTick) {
                    return@register
                }
                pendingAttack = null
                if (isStillValid(client, pending)) {
                    if (!AutoBlock.prepareHitFlick(client)) {
                        pendingAttack = pending.copy(scheduledTick = pending.player.tickCount)
                        return@register
                    }
                    RotationManager.flickTick()
                    performPendingAttack(pending)
                }
            }

            clearHeldRotation()
        }
    }

    @JvmStatic
    fun scheduleAttack(
        player: LocalPlayer,
        target: LivingEntity,
        gameMode: MultiPlayerGameMode,
    ): Boolean {
        if (pendingAttack != null) {
            suppressOriginalSwing(player)
            return true
        }
        if (requireSprint.value && !player.isSprinting) return false

        val time = System.currentTimeMillis()
        if (time - lastFlickTime < cooldownMs.value) return false

        val dx = target.x - player.x
        val dz = target.z - player.z
        val baseYaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val options = Minecraft.getInstance().options

        val flickYaw = when (flickMode.value) {
            FlickMode.VOID -> chooseVoidYaw(player, target, baseYaw) ?: return false
            else -> {
                val (lo, hi) = flickAngle.value
                val angle = if (hi > lo) lo + Random.nextFloat() * (hi - lo) else lo
                val side = when (flickMode.value) {
                    FlickMode.LEFT -> -1f
                    FlickMode.RIGHT -> 1f
                    FlickMode.RANDOM -> if (Random.nextBoolean()) -1f else 1f
                    FlickMode.STRAFE -> when {
                        options.keyLeft.isDown && !options.keyRight.isDown -> -1f
                        options.keyRight.isDown && !options.keyLeft.isDown -> 1f
                        else -> return false
                    }
                    FlickMode.STRAFE_INVERTED -> when {
                        options.keyLeft.isDown && !options.keyRight.isDown -> 1f
                        options.keyRight.isDown && !options.keyLeft.isDown -> -1f
                        else -> return false
                    }
                    FlickMode.VOID -> error("Void yaw is resolved separately")
                }
                baseYaw + side * angle
            }
        }

        lastFlickTime = time

        rotationHeld = true
        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.setTargetRotation(flickYaw, player.xRot, ROTATION_OWNER)
        RotationManager.flickTick()

        pendingAttack = PendingAttack(player, target, gameMode, player.tickCount)
        suppressOriginalSwing(player)
        return true
    }

    @JvmStatic
    fun isAutoKbAttack(): Boolean = autoKbAttack

    @JvmStatic
    fun hasPendingAttack(): Boolean = pendingAttack != null

    @JvmStatic
    fun shouldSuppressOriginalSwing(player: LocalPlayer, hand: InteractionHand): Boolean {
        if (!suppressNextSwing || skipIntercept || hand != InteractionHand.MAIN_HAND) return false

        val shouldSuppress = pendingAttack?.player === player && suppressSwingTick == player.tickCount
        suppressNextSwing = false
        suppressSwingTick = Int.MIN_VALUE
        return shouldSuppress
    }

    private fun isStillValid(client: Minecraft, pending: PendingAttack): Boolean =
        client.player === pending.player &&
            client.level != null &&
            pending.target.level() === client.level &&
            pending.target.isAlive &&
            !pending.target.isRemoved &&
            pending.player.distanceTo(pending.target) <= 12f

    private fun performPendingAttack(pending: PendingAttack) {
        val player = pending.player
        suppressNextSwing = false
        suppressSwingTick = Int.MIN_VALUE
        val originalSlot = player.inventory.selectedSlot
        val knockbackSlot = if (autoKb.value) findBestKnockbackSlot(player) else -1

        autoKbAttack = knockbackSlot in 0..8
        if (autoKbAttack && knockbackSlot != originalSlot) {
            player.inventory.selectedSlot = knockbackSlot
        }

        skipIntercept = true
        try {
            pending.gameMode.attack(player, pending.target)
            player.swing(InteractionHand.MAIN_HAND)
        } finally {
            skipIntercept = false
            if (autoKbAttack && player.inventory.selectedSlot != originalSlot) {
                player.inventory.selectedSlot = originalSlot
                player.connection.send(ServerboundSetCarriedItemPacket(originalSlot))
            }
            autoKbAttack = false
        }
    }

    private fun suppressOriginalSwing(player: LocalPlayer) {
        suppressNextSwing = true
        suppressSwingTick = player.tickCount
    }

    private fun chooseVoidYaw(
        player: LocalPlayer,
        target: LivingEntity,
        baseYaw: Float,
    ): Float? {
        var best: VoidTrajectory? = null

        for (step in 0 until VOID_YAW_STEPS) {
            val candidateYaw = -180f + step * VOID_YAW_STEP
            val trajectory = scoreVoidTrajectory(player, target, candidateYaw, baseYaw) ?: continue
            if (best == null || trajectory.score > best.score) {
                best = trajectory
            }
        }

        return best?.yaw
    }

    private fun scoreVoidTrajectory(
        player: LocalPlayer,
        target: LivingEntity,
        yaw: Float,
        baseYaw: Float,
    ): VoidTrajectory? {
        val level = player.level()
        val radians = Math.toRadians(yaw.toDouble())
        val directionX = -sin(radians)
        val directionZ = cos(radians)
        val maxDistance = voidCheckDistance.value.toDouble()
        val probeDepth = (voidDepth.value + 12).coerceAtMost(64)

        var firstVoidDistance = Double.NaN
        var voidSamples = 0
        var totalDepth = 0.0
        var distance = VOID_PATH_STEP

        while (distance <= maxDistance + 1.0e-6) {
            val offsetX = directionX * distance
            val offsetZ = directionZ * distance
            val projectedBox = target.boundingBox.move(offsetX, 0.0, offsetZ)

            if (!level.noCollision(projectedBox.deflate(0.03))) break

            val drop = projectedDrop(player, projectedBox, probeDepth)
            if (drop >= voidDepth.value) {
                if (firstVoidDistance.isNaN()) firstVoidDistance = distance
                voidSamples++
                totalDepth += drop
            } else if (!firstVoidDistance.isNaN()) {
                break
            }

            distance += VOID_PATH_STEP
        }

        if (firstVoidDistance.isNaN()) return null
        val voidRun = voidSamples * VOID_PATH_STEP
        if (voidRun < MIN_VOID_RUN) return null

        return VoidTrajectory(
            yaw = yaw,
            edgeDistance = firstVoidDistance,
            voidRun = voidRun,
            averageDepth = totalDepth / voidSamples,
            facingDeviation = abs(Mth.wrapDegrees(yaw - baseYaw)),
        )
    }

    private fun projectedDrop(
        player: LocalPlayer,
        projectedBox: AABB,
        maxDepth: Int,
    ): Int {
        val level = player.level()
        val inset = minOf(0.08, projectedBox.xsize * 0.15, projectedBox.zsize * 0.15)
        val supportProbe = AABB(
            projectedBox.minX + inset,
            projectedBox.minY - 0.08,
            projectedBox.minZ + inset,
            projectedBox.maxX - inset,
            projectedBox.minY + 0.01,
            projectedBox.maxZ - inset,
        )

        for (depth in 0 until maxDepth) {
            if (!level.noCollision(supportProbe.move(0.0, -depth.toDouble(), 0.0))) return depth
        }
        return maxDepth
    }

    private fun findBestKnockbackSlot(player: LocalPlayer): Int {
        var bestSlot = -1
        var bestLevel = 0
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            if (stack.isEmpty) continue
            val level = enchantLevel(stack, Enchantments.KNOCKBACK)
            if (level > bestLevel) {
                bestLevel = level
                bestSlot = slot
            }
        }
        return bestSlot
    }

    private fun enchantLevel(
        stack: ItemStack,
        enchantmentKey: ResourceKey<Enchantment>,
    ): Int {
        val level = Minecraft.getInstance().level ?: return 0
        return try {
            val enchantment = level.registryAccess().getOrThrow(enchantmentKey)
            stack.enchantments.getLevel(enchantment)
        } catch (_: Exception) {
            0
        }
    }

    private fun clearHeldRotation() {
        if (!rotationHeld) return
        RotationManager.clearRotation(ROTATION_OWNER)
        rotationHeld = false
    }

    override fun hudInfo(): String {
        if (flickMode.value == FlickMode.VOID) return "void"
        val (lo, hi) = flickAngle.value
        var text = "%.1f-%.1f°".format(lo, hi)
        if (hi - lo < 15 && hi > 90 && lo < 90) text = "90°"
        if (hi - lo < 15 && hi >= 179) text = "180°"
        return text
    }
}
