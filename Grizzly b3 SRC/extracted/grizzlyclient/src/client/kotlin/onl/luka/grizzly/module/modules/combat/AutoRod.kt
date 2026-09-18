package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import onl.luka.grizzly.util.useItemStrict
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.tags.ItemTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object AutoRod : Module(
    "Auto Rod",
    "Automatically throws your fishing rod, retracts it, or throws projectiles",
    Category.COMBAT
) {
    const val AURA_TARGET_RANGE = 8f
    private const val MAX_TRACK_RANGE = AURA_TARGET_RANGE + 2f

    private val retractSpeed = intRange("retract speed (ms)", 200 to 300, 0, 2000)
    private val actionDelay = intRange("action delay (ms)", 50 to 100, 0, 1000)
    private val randomizeDelays = boolean("randomize delays", true)
    private val switchBackToWeapon = boolean("switch to weapon", false)
    private val castFov = float("cast fov", 16f, 2f, 90f)
    private val hitboxExpand = float("hitbox expand", 0.35f, 0f, 1.5f)
    private val predictTicks = float("predict ticks", 1.5f, 0f, 4f)
    private val maxHookTicks = int("max hook ticks", 18, 4, 40)
    private val recastCooldown = intRange("recast cooldown (ms)", 250 to 450, 0, 1500)
    
    private val allowThrowables = boolean("allow throwables", false)
    private val throwSpeed = intRange("throw speed (ms)", 100 to 200, 0, 1000).also {
        it.visibleWhen = { allowThrowables.value }
    }

    // 0 = disabled
    private val notMeleeRange = float("not within melee range", 3.0f, 0.0f, 8.0f)

    private enum class State {
        IDLE, SWITCHING, THROWING, WAITING_HOOK, RETRACTING, SWITCHING_BACK
    }

    private var state = State.IDLE
    private var waitTime = 0L
    private var originalSlot = -1
    private var currentItemType: ItemType = ItemType.NONE
    
    private var retractTimer = 0L
    private var behaviorWasActive = false
    private var auraRotationReady = false
    private var castTargetId = -1
    private var castTick = 0
    private var closestHookDistanceSq = Double.MAX_VALUE
    private var nextCastAt = 0L

    private enum class ItemType { NONE, ROD, PROJECTILE }

    override fun onEnabled() {
        if (!behaviorWasActive) {
            resetState(Minecraft.getInstance(), restoreSlot = false)
        }
    }

    override fun onDisabled() {
        if (!isAuraIntegrationActive()) {
            resetState(Minecraft.getInstance(), restoreSlot = true)
            behaviorWasActive = false
        }
    }

    private fun getDelay(range: Pair<Int, Int>): Long {
        val (lo, hi) = range
        if (lo >= hi || !randomizeDelays.value) return lo.toLong()
        return (lo..hi).random().toLong()
    }

    init {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            val active = isBehaviorActive()
            if (!active) {
                if (behaviorWasActive) {
                    resetState(client, restoreSlot = true)
                }
                behaviorWasActive = false
                return@register
            }

            behaviorWasActive = true
            tickBehavior(client)
        }
    }

    private fun tickBehavior(client: Minecraft) {
        val player = client.player ?: return
        if (client.gui.screen() != null) return
        if (state == State.IDLE && isManualUseActive(client, player)) return
        
        val now = System.currentTimeMillis()
        if (now < waitTime) return

        val auraIntegration = isAuraIntegrationActive()
        val auraTarget = SilentAura.rodTarget
            ?.takeIf {
                auraIntegration &&
                    it.isAlive &&
                    player.distanceTo(it) <= AURA_TARGET_RANGE &&
                    TargetFilter.isValidTarget(player, it)
            }
        val targets = if (auraIntegration) {
            emptyList()
        } else {
            client.level?.entitiesForRendering()?.filterIsInstance<LivingEntity>()
                ?.filter { e ->
                    if (e === player ||
                        !e.isAlive ||
                        player.distanceTo(e) > AURA_TARGET_RANGE ||
                        !TargetFilter.isValidTarget(player, e)
                    ) {
                        return@filter false
                    }
                    val dx = e.x - player.x
                    val dz = e.z - player.z
                    val yaw = Math.toDegrees(kotlin.math.atan2(-dx, dz)).toFloat()
                    val fov = net.minecraft.util.Mth.wrapDegrees(yaw - player.yRot)
                    kotlin.math.abs(fov) < 45f
                } ?: emptyList()
        }
        
        val closest = auraTarget ?: targets.minByOrNull { player.distanceTo(it) }

        val hasValidTarget = closest != null && isUsefulRodTarget(player, closest)

        if (!hasValidTarget) {
            when (state) {
                State.IDLE -> return
                State.WAITING_HOOK -> {
                    state = State.RETRACTING
                    waitTime = now + getDelay(actionDelay.value)
                }
                State.SWITCHING_BACK, State.RETRACTING -> Unit
                else -> {
                    state = State.SWITCHING_BACK
                    waitTime = now + getDelay(actionDelay.value)
                }
            }
        }

        when (state) {
            State.IDLE -> {
                val target = closest ?: return
                if (now < nextCastAt) return
                if (!shouldThrowAt(player, target)) return

                val (slot, type) = findRodOrThrowable(player)
                if (slot == -1) return
                
                originalSlot = player.inventory.selectedSlot
                currentItemType = type
                castTargetId = target.id
                
                if (player.inventory.selectedSlot != slot) {
                    player.inventory.selectedSlot = slot
                    state = State.THROWING
                    waitTime = now + getDelay(actionDelay.value)
                } else {
                    state = State.THROWING
                    waitTime = now
                }
            }
            State.THROWING -> {
                if (isAuraIntegrationActive() && !auraRotationReady) return
                val target = trackedTarget(client) ?: closest
                if (target == null || !shouldThrowAt(player, target)) {
                    state = State.SWITCHING_BACK
                    waitTime = now + getDelay(actionDelay.value)
                    return
                }

                if (useItemStrict(InteractionHand.MAIN_HAND) == null) {
                    state = State.SWITCHING_BACK
                    waitTime = now + getDelay(actionDelay.value)
                    return
                }
                
                if (currentItemType == ItemType.ROD) {
                    state = State.WAITING_HOOK
                    castTargetId = target.id
                    castTick = player.tickCount
                    closestHookDistanceSq = Double.MAX_VALUE
                    val delay = getDelay(actionDelay.value)
                    waitTime = now + delay
                    retractTimer = now + delay + getDelay(retractSpeed.value)
                } else {
                    state = State.SWITCHING_BACK
                    waitTime = now + getDelay(throwSpeed.value)
                    nextCastAt = now + getDelay(recastCooldown.value)
                }
            }
            State.WAITING_HOOK -> {
                val hook = player.fishing
                val target = trackedTarget(client)
                if (hook == null ||
                    hook.onGround() ||
                    hook.hookedIn != null ||
                    target == null ||
                    shouldRetractHook(player, hook, target, now)
                ) {
                    state = State.RETRACTING
                    waitTime = now + getDelay(actionDelay.value)
                }
            }
            State.RETRACTING -> {
                useItemStrict(InteractionHand.MAIN_HAND)
                nextCastAt = now + getDelay(recastCooldown.value)
                state = State.SWITCHING_BACK
                waitTime = now + getDelay(actionDelay.value)
            }
            State.SWITCHING_BACK -> {
                if (switchBackToWeapon.value) {
                    val weaponSlot = findWeapon(player)
                    if (weaponSlot != -1) {
                        player.inventory.selectedSlot = weaponSlot
                    } else if (originalSlot != -1) {
                        player.inventory.selectedSlot = originalSlot
                    }
                } else if (originalSlot != -1) {
                    player.inventory.selectedSlot = originalSlot
                }
                state = State.IDLE
                originalSlot = -1
                currentItemType = ItemType.NONE
                castTargetId = -1
                castTick = 0
                closestHookDistanceSq = Double.MAX_VALUE
            }
            else -> state = State.IDLE
        }
    }

    private fun isAuraIntegrationActive(): Boolean =
        SilentAura.isEnabled() && SilentAura.autoRod.value

    private fun isBehaviorActive(): Boolean =
        isEnabled() || isAuraIntegrationActive()

    fun isControllingHotbar(): Boolean =
        isBehaviorActive() && state != State.IDLE

    fun setAuraRotationReady(ready: Boolean) {
        auraRotationReady = ready
    }

    fun canAcquireAuraTarget(client: Minecraft, player: Player): Boolean =
        isAuraIntegrationActive() &&
            !isManualUseActive(client, player) &&
            findRodOrThrowable(player).first != -1

    fun auraRotation(player: Player, target: LivingEntity): Pair<Float, Float>? {
        if (!isAuraIntegrationActive() || !isUsefulRodTarget(player, target)) return null
        if (findRodOrThrowable(player).first == -1) return null
        return calcRotationTo(player.eyePosition, targetAimPoint(target))
    }

    fun prepareAura(client: Minecraft, rotationReady: Boolean): Boolean {
        if (!isAuraIntegrationActive()) return isControllingHotbar()
        auraRotationReady = rotationReady
        if (state == State.IDLE) {
            tickBehavior(client)
        }
        return isControllingHotbar()
    }

    fun stopAuraIntegration() {
        if (isEnabled()) return
        resetState(Minecraft.getInstance(), restoreSlot = true)
        behaviorWasActive = false
    }

    private fun resetState(client: Minecraft, restoreSlot: Boolean) {
        if (restoreSlot && originalSlot in 0..8) {
            client.player?.inventory?.selectedSlot = originalSlot
        }
        state = State.IDLE
        waitTime = 0L
        originalSlot = -1
        currentItemType = ItemType.NONE
        retractTimer = 0L
        auraRotationReady = false
        castTargetId = -1
        castTick = 0
        closestHookDistanceSq = Double.MAX_VALUE
        nextCastAt = 0L
    }

    private fun isManualUseActive(client: Minecraft, player: Player): Boolean =
        player.isUsingItem || InputUtil.isPhysicalKeyDown(client.options.keyUse)

    private fun findRodOrThrowable(player: Player): Pair<Int, ItemType> {
        for (i in 0..8) {
            val item = player.inventory.getItem(i).item
            if (item is FishingRodItem) {
                return i to ItemType.ROD
            }
        }
        if (allowThrowables.value) {
            for (i in 0..8) {
                val item = player.inventory.getItem(i).item
                if (item is SnowballItem || item is EggItem) {
                    return i to ItemType.PROJECTILE
                }
            }
        }
        return -1 to ItemType.NONE
    }

    private fun findWeapon(player: net.minecraft.world.entity.player.Player): Int {
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            val isWeapon = stack.`is`(ItemTags.SWORDS) ||
                           stack.`is`(ItemTags.AXES) || 
                           stack.item is TridentItem
            if (isWeapon && !stack.isEmpty) return i
        }
        return -1
    }

    private fun isUsefulRodTarget(player: Player, target: LivingEntity): Boolean {
        val distance = player.distanceTo(target)
        if (distance > AURA_TARGET_RANGE) return false
        if (notMeleeRange.value > 0f && distance <= notMeleeRange.value) return false
        if (!TargetFilter.isValidTarget(player, target)) return false
        return true
    }

    private fun shouldThrowAt(player: Player, target: LivingEntity): Boolean {
        if (!isUsefulRodTarget(player, target)) return false
        val aim = targetAimPoint(target)
        val (yaw, pitch) = calcRotationTo(player.eyePosition, aim)
        val yawDiff = abs(Mth.wrapDegrees(yaw - activeYaw(player)))
        val pitchDiff = abs(Mth.wrapDegrees(pitch - activePitch(player)))
        if (sqrt((yawDiff * yawDiff + pitchDiff * pitchDiff).toDouble()) > castFov.value) return false
        return castRayWouldHit(player, target)
    }

    private fun castRayWouldHit(player: Player, target: LivingEntity): Boolean {
        val level = player.level()
        val eye = player.eyePosition
        val end = eye.add(activeLookVector(player).scale(AURA_TARGET_RANGE.toDouble()))
        val box = predictedTargetBox(target).inflate(hitboxExpand.value.toDouble())
        val hit = box.clip(eye, end).orElse(null) ?: return false
        val blockHit = level.clip(
            ClipContext(eye, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        )
        return blockHit.type == HitResult.Type.MISS || blockHit.location.distanceToSqr(eye) > hit.distanceToSqr(eye) - 0.01
    }

    private fun shouldRetractHook(
        player: Player,
        hook: Entity,
        target: LivingEntity,
        now: Long,
    ): Boolean {
        val age = player.tickCount - castTick
        if (age >= maxHookTicks.value) return true
        if (now > retractTimer && age >= 3) return true
        if (!isUsefulRodTarget(player, target) || player.distanceTo(target) > MAX_TRACK_RANGE) return true

        val hookPos = hook.position()
        val targetBox = predictedTargetBox(target).inflate(hitboxExpand.value.toDouble() + 0.2)
        val distanceSq = distanceToBoxSqr(hookPos, targetBox)
        if (distanceSq < 0.18) return true

        val previousClosest = closestHookDistanceSq
        closestHookDistanceSq = minOf(closestHookDistanceSq, distanceSq)
        val passedTarget = age >= 5 && previousClosest < 1.15 && distanceSq > previousClosest + 0.55
        if (passedTarget) return true

        val targetCenter = targetBox.center
        val hookVelocity = hook.deltaMovement
        val toTarget = targetCenter.subtract(hookPos)
        val movingAway = age >= 5 && hookVelocity.lengthSqr() > 0.0001 && hookVelocity.dot(toTarget) < -0.02
        return movingAway && distanceSq > 0.65
    }

    private fun trackedTarget(client: Minecraft): LivingEntity? =
        client.level?.entitiesForRendering()
            ?.filterIsInstance<LivingEntity>()
            ?.firstOrNull { it.id == castTargetId && it.isAlive && !it.isRemoved }

    private fun predictedTargetBox(target: LivingEntity): AABB {
        val velocity = target.deltaMovement
        val ticks = predictTicks.value.toDouble()
        return target.boundingBox.move(
            velocity.x * ticks,
            velocity.y.coerceIn(-0.25, 0.25) * ticks,
            velocity.z * ticks,
        )
    }

    private fun targetAimPoint(target: LivingEntity): Vec3 {
        val box = predictedTargetBox(target)
        return Vec3(
            (box.minX + box.maxX) * 0.5,
            box.minY + (box.maxY - box.minY) * 0.58,
            (box.minZ + box.maxZ) * 0.5,
        )
    }

    private fun calcRotationTo(from: Vec3, to: Vec3): Pair<Float, Float> {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        val horizDist = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizDist))).toFloat()
        return yaw to pitch
    }

    private fun activeYaw(player: Player): Float =
        if (RotationManager.isActive()) RotationManager.getCurrentYaw() else player.yRot

    private fun activePitch(player: Player): Float =
        if (RotationManager.isActive()) RotationManager.getCurrentPitch() else player.xRot

    private fun activeLookVector(player: Player): Vec3 {
        val yaw = Math.toRadians(activeYaw(player).toDouble())
        val pitch = Math.toRadians(activePitch(player).toDouble())
        val cosPitch = kotlin.math.cos(pitch)
        return Vec3(
            -kotlin.math.sin(yaw) * cosPitch,
            -kotlin.math.sin(pitch),
            kotlin.math.cos(yaw) * cosPitch,
        )
    }

    private fun distanceToBoxSqr(point: Vec3, box: AABB): Double {
        val dx = axisDistance(point.x, box.minX, box.maxX)
        val dy = axisDistance(point.y, box.minY, box.maxY)
        val dz = axisDistance(point.z, box.minZ, box.maxZ)
        return dx * dx + dy * dy + dz * dz
    }

    private fun axisDistance(value: Double, min: Double, max: Double): Double =
        when {
            value < min -> min - value
            value > max -> value - max
            else -> 0.0
        }
}
