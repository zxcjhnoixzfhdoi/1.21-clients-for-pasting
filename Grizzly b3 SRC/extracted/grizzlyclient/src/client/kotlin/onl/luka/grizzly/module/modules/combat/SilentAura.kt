package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.world.scaffold.Scaffold
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TridentItem
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

object SilentAura : Module(
    name = "Silent Aura",
    description = "Silently aims and attacks enemies when they enter your range",
    category = Category.COMBAT
) {
    enum class Mode {
        CPS, SEQUENTIAL
    }

    enum class TargetMode {
        SINGLE, SWITCH, MULTI
    }

    enum class AimMode {
        SMOOTH, SNAP
    }

    enum class SortMode {
        ANGLE, DISTANCE, HEALTH, HURT_TIME
    }

    enum class AimPoint {
        NEAREST, CENTER
    }

    private val targetingGroup = group("Targeting")
    private val conditionGroup = group("Conditions")

    private val mode = enum("mode", Mode.CPS)
    private val targetMode = enum("targeting", TargetMode.SWITCH)
    private val aimMode = enum("rotations", AimMode.SMOOTH)

    private val range       = float("hit range", 4.0f, 1.0f, 8.0f)
    private val swingRange  = float("swing range", 4.5f, 1.0f, 8.0f)
    private val aimRange    = float("aim range", 4.5f, 1.0f, 8.0f)
    private val fov         = float("fov", 180.0f, 10.0f, 360.0f)
    private val cps         = floatRange("cps", 10.0f to 13.0f, 1.0f, 20.0f).also {
        it.visibleWhen = { mode.value == Mode.CPS }
    }
    private val extraDelay  = intRange("extra delay", 0 to 2, -5, 5).also {
        it.visibleWhen = { mode.value == Mode.SEQUENTIAL }
    }
    private val smoothSpeed = float("smoothness", 30.0f, 1.0f, 100.0f).also {
        it.visibleWhen = { aimMode.value == AimMode.SMOOTH }
    }

    private val sortMode = enum("sort", SortMode.ANGLE).inGroup(targetingGroup)
    private val aimPoint = enum("aim point", AimPoint.NEAREST).inGroup(targetingGroup)
    private val switchDelay = int("switch delay (ms)", 200, 0, 1000).inGroup(targetingGroup).also {
        it.visibleWhen = { targetMode.value == TargetMode.SWITCH }
    }
    private val maxTargets = int("max targets", 3, 1, 10).inGroup(targetingGroup).also {
        it.visibleWhen = { targetMode.value != TargetMode.SINGLE }
    }
    private val raycast = boolean("raycast", true).inGroup(targetingGroup)
    private val visibilityCheck = boolean("visibility check", true).inGroup(targetingGroup)
    private val playersOnly = boolean("players only", true).inGroup(targetingGroup)

    private val requireMouseDown = boolean("require mouse down", false).inGroup(conditionGroup)
    private val weaponOnly = boolean("weapon only", false).inGroup(conditionGroup)
    private val notWhileUsing = boolean("not while using item", false).inGroup(conditionGroup)
    private val notWhileMining = boolean("not while mining", false).inGroup(conditionGroup)

    val autoBlock   = boolean("auto block", true)
    val autoRod = boolean("auto rod", false)
    private val autoWeapon  = boolean("auto weapon", false)

    var target: LivingEntity? = null
    internal var aimTarget: LivingEntity? = null
        private set
    internal var rodTarget: LivingEntity? = null
        private set
    private var nextClickAt = 0L
    private var seqDelayTicks = 0
    private var ownsRotation = false
    private var lastSwitchAt = 0L
    private val recentTargets = ArrayDeque<Int>()
    @Volatile private var lastAttackIntentMs = Long.MIN_VALUE

    override fun onEnabled() {
        nextClickAt = 0L
        target = null
        aimTarget = null
        rodTarget = null
        seqDelayTicks = 0
        ownsRotation = false
        lastSwitchAt = 0L
        recentTargets.clear()
        lastAttackIntentMs = Long.MIN_VALUE
    }

    override fun onDisabled() {
        AutoRod.stopAuraIntegration()
        clearAura()
    }

    private fun clearAura() {
        if (ownsRotation && !onl.luka.grizzly.module.modules.combat.KnockbackDisplacement.rotationHeld) {
            RotationManager.clearRotation()
        }
        target = null
        aimTarget = null
        rodTarget = null
        ownsRotation = false
        nextClickAt = 0L
        lastAttackIntentMs = Long.MIN_VALUE
    }

    internal fun shouldInterruptChestActions(client: Minecraft): Boolean {
        if (!isEnabled()) return false
        val player = client.player ?: return false
        val level = client.level ?: return false
        val maxRange = maxSearchRange(client, player)
        val halfFov = fov.value / 2f

        return level.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .any { isValidCandidate(player, it, maxRange, halfFov) }
    }

    private fun maxSearchRange(client: Minecraft, player: LocalPlayer): Double {
        val reach = maxOf(range.value, swingRange.value, aimRange.value).toDouble()
        return if (AutoRod.canAcquireAuraTarget(client, player)) {
            max(reach, AutoRod.AURA_TARGET_RANGE.toDouble())
        } else {
            reach
        }
    }

    private fun isValidCandidate(
        player: Player,
        entity: LivingEntity,
        maxRange: Double,
        halfFov: Float,
    ): Boolean {
        if (
            entity === player ||
            entity.isDeadOrDying ||
            (playersOnly.value && entity !is Player) ||
            hitboxDistance(player, entity) > maxRange ||
            !TargetFilter.isValidTarget(player, entity) ||
            (visibilityCheck.value && !player.hasLineOfSight(entity))
        ) {
            return false
        }

        val (yaw, pitch) = calcRotation(player, entity)
        return Mth.abs(Mth.wrapDegrees(yaw - player.yRot)) <= halfFov &&
            Mth.abs(Mth.wrapDegrees(pitch - player.xRot)) <= halfFov
    }

    init {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            if (!isEnabled()) return@register
            AutoRod.setAuraRotationReady(false)
            val player = client.player ?: return@register
            val level = client.level ?: return@register
            if (client.gui.screen() != null) return@register
            if (!conditionsMet(client, player)) {
                clearAura()
                return@register
            }

            val canAcquireRodTarget = AutoRod.canAcquireAuraTarget(client, player)
            val maxRange = maxSearchRange(client, player)
            val halfFov = fov.value / 2f

            val candidates = level.entitiesForRendering()
                .filterIsInstance<LivingEntity>()
                .filter { isValidCandidate(player, it, maxRange, halfFov) }
                .sortedWith(targetComparator(player))

            // Anything already in attack range wins, so the extra aim range only pulls the
            // rotation onto someone early rather than stealing the target from a closer one.
            val attackCandidates = candidates.filter { hitboxDistance(player, it) <= range.value }
            val aimCandidates = candidates.filter { hitboxDistance(player, it) <= aimRange.value }
            val chosen = selectTarget(attackCandidates.ifEmpty { aimCandidates })
            aimTarget = chosen
            target = chosen?.takeIf { hitboxDistance(player, it) <= range.value }

            rodTarget = if (canAcquireRodTarget) {
                chosen ?: candidates.firstOrNull()
            } else {
                null
            }

            val currentRotationTarget = rodTarget ?: chosen
            if (currentRotationTarget == null) {
                clearAura()
                return@register
            }

            if (!canUseAuraRotation()) return@register
            updateAuraRotation(player, currentRotationTarget)

            if (AutoRod.prepareAura(client, RotationManager.hasReachedTarget(2f))) {
                return@register
            }

            val inSwingRange = chosen != null && hitboxDistance(player, chosen) <= swingRange.value

            // Only swap once we are actually going to hit something. Aim range and the rod
            // range reach much further, and swapping out there just flickers the hotbar.
            val swapTarget = (target ?: chosen)?.takeIf { target != null || inSwingRange }
            if (autoWeapon.value && swapTarget != null && canSeeForSwap(player, swapTarget)) {
                val bestSlot = findBestWeapon(player)
                if (bestSlot != -1 && player.inventory.selectedSlot != bestSlot) {
                    player.inventory.selectedSlot = bestSlot
                }
            }

            if (!inSwingRange) return@register

            when (mode.value) {
                Mode.CPS -> {
                    val now = System.currentTimeMillis()
                    if (nextClickAt == 0L || now - nextClickAt > CLICK_CATCHUP_LIMIT_MS) {
                        nextClickAt = now
                    }

                    var clicks = 0
                    while (nextClickAt <= now && clicks < MAX_CLICKS_PER_TICK) {
                        if (!click(client, player, candidates)) {
                            // AutoBlock is mid sequence, so retry next tick rather than
                            // banking up the clicks that were missed.
                            nextClickAt = now
                            break
                        }
                        clicks++
                        nextClickAt += nextClickDelay()
                    }
                }

                Mode.SEQUENTIAL -> {
                    if (target == null) return@register
                    if (seqDelayTicks > 0) {
                        seqDelayTicks--
                        return@register
                    }
                    if (
                        player.getAttackStrengthScale(0.5f) >= 1.0f &&
                        click(client, player, candidates)
                    ) {
                        val (lo, hi) = extraDelay.value
                        seqDelayTicks = (if (hi > lo) (lo..hi).random() else lo).coerceAtLeast(0)
                    }
                }
            }
        }
    }

    /** Returns false only when the click could not be taken and should be retried. */
    private fun click(client: Minecraft, player: LocalPlayer, candidates: List<LivingEntity>): Boolean {
        val currentTarget = target
        if (currentTarget == null || !isAimedAtTarget(player, currentTarget)) {
            player.swing(InteractionHand.MAIN_HAND)
            return true
        }

        if (targetMode.value != TargetMode.MULTI) return performNormalAttack(client, currentTarget)

        val multiTargets = candidates
            .filter { hitboxDistance(player, it) <= range.value }
            .take(maxTargets.value)
        if (multiTargets.isEmpty()) return performNormalAttack(client, currentTarget)

        var attacked = false
        for (entity in multiTargets) {
            if (!performNormalAttack(client, entity, swing = !attacked)) {
                return attacked
            }
            attacked = true
        }
        return attacked
    }

    private fun conditionsMet(client: Minecraft, player: LocalPlayer): Boolean {
        if (requireMouseDown.value && !InputUtil.isPhysicalKeyDown(client.options.keyAttack)) return false
        if (weaponOnly.value && !isWeapon(player.mainHandItem)) return false
        if (notWhileUsing.value && player.isUsingItem) return false
        if (notWhileMining.value && client.gameMode?.isDestroying == true) return false
        return true
    }

    private fun targetComparator(player: Player): Comparator<LivingEntity> = when (sortMode.value) {
        SortMode.ANGLE -> compareBy { angularDistance(player, it) }
        SortMode.DISTANCE -> compareBy { hitboxDistance(player, it) }
        SortMode.HEALTH -> compareBy<LivingEntity> { it.health }.thenBy { hitboxDistance(player, it) }
        SortMode.HURT_TIME -> compareBy<LivingEntity> { it.hurtTime }.thenBy { hitboxDistance(player, it) }
    }

    private fun selectTarget(pool: List<LivingEntity>): LivingEntity? {
        if (pool.isEmpty()) return null
        val held = aimTarget?.takeIf { current -> pool.any { it === current } }

        return when (targetMode.value) {
            TargetMode.SINGLE -> held ?: pool.first()
            TargetMode.MULTI -> pool.first()
            TargetMode.SWITCH -> {
                val now = System.currentTimeMillis()
                if (held != null && now - lastSwitchAt < switchDelay.value) return held

                // Cycle past the ones already hit so a group fight spreads the damage instead
                // of tunnelling on whoever happens to sort first.
                var next = pool.firstOrNull { it.id !in recentTargets }
                if (next == null) {
                    recentTargets.clear()
                    next = pool.first()
                }
                if (next !== held) {
                    lastSwitchAt = now
                    recentTargets.addLast(next.id)
                    while (recentTargets.size >= maxTargets.value) recentTargets.removeFirst()
                }
                next
            }
        }
    }

    private fun nextClickDelay(): Long {
        val (lo, hi) = cps.value
        val picked = if (hi > lo) lo + Random.nextFloat() * (hi - lo) else lo
        val base = (1000f / picked.coerceAtLeast(1f)).toLong()
        return (base + Random.nextInt(-10, 11)).coerceIn(MIN_CLICK_DELAY_MS, MAX_CLICK_DELAY_MS)
    }

    private fun performNormalAttack(
        client: Minecraft,
        entity: LivingEntity,
        swing: Boolean = true,
    ): Boolean {
        val player = client.player ?: return false
        if (autoBlock.value && !AutoBlock.prepareAuraSwing(client)) {
            return false
        }
        val gameMode = client.gameMode ?: return false
        lastAttackIntentMs = System.currentTimeMillis()
        gameMode.attack(player, entity)
        if (swing) player.swing(InteractionHand.MAIN_HAND)
        return true
    }

    fun isActivelyAttacking(): Boolean {
        val attackAt = lastAttackIntentMs
        return isEnabled() && attackAt != Long.MIN_VALUE &&
            System.currentTimeMillis() - attackAt <= ATTACK_INTENT_WINDOW_MS
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        val player = Minecraft.getInstance().player ?: return
        val currentTarget = rodTarget ?: aimTarget ?: return

        if (!canUseAuraRotation()) {
            return
        }

        updateAuraRotation(player, currentTarget)
    }

    private fun updateAuraRotation(player: Player, currentTarget: LivingEntity) {
        val (targetYaw, targetPitch) =
            AutoRod.auraRotation(player, currentTarget) ?: calcRotation(player, currentTarget)
        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode  = RotationManager.RotationMode.CLIENT

        RotationManager.setTargetRotation(targetYaw, targetPitch)
        ownsRotation = true
        when (aimMode.value) {
            AimMode.SMOOTH -> RotationManager.quickTick(smoothSpeed.value)
            AimMode.SNAP -> RotationManager.flickTick()
        }
    }

    private fun canUseAuraRotation(): Boolean =
        !((Scaffold.isEnabled() && RotationManager.isActive()) ||
                onl.luka.grizzly.module.modules.combat.KnockbackDisplacement.rotationHeld ||
                (onl.luka.grizzly.module.modules.world.BedBreaker.isEnabled() && onl.luka.grizzly.module.modules.world.BedBreaker.pendingHitPos != null) ||
                onl.luka.grizzly.module.modules.world.Clutch.isActivelyPlacing)

    private fun isAimedAtTarget(player: Player, currentTarget: LivingEntity): Boolean {
        if (!raycast.value) return true
        val hit = attackRaycast(player) { it === currentTarget } ?: return false
        return hit.entity === currentTarget
    }

    /** Eye to the closest point on the hitbox, which is what the server measures reach against. */
    private fun hitboxDistance(player: Player, t: LivingEntity): Double {
        val eye = player.eyePosition
        val box = t.boundingBox
        val dx = max(max(box.minX - eye.x, 0.0), eye.x - box.maxX)
        val dy = max(max(box.minY - eye.y, 0.0), eye.y - box.maxY)
        val dz = max(max(box.minZ - eye.z, 0.0), eye.z - box.maxZ)
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun angularDistance(player: Player, t: LivingEntity): Float {
        val (yaw, pitch) = calcRotation(player, t)
        val dy = Mth.wrapDegrees(yaw - player.yRot)
        val dp = Mth.wrapDegrees(pitch - player.xRot)
        return sqrt((dy * dy + dp * dp).toDouble()).toFloat()
    }

    /**
     * Nearest aims at the part of the hitbox already closest to the crosshair, pulled a little
     * way inside so the ray lands in the box instead of skimming its face. It keeps the turn
     * small at close range, where centre aiming has to swing through a wide arc.
     */
    private fun aimVec(player: Player, t: LivingEntity): Vec3 {
        if (aimPoint.value == AimPoint.CENTER) {
            return Vec3(t.x, t.y + t.bbHeight * 0.5, t.z)
        }
        val eye = player.eyePosition
        val box = t.boundingBox.deflate(HITBOX_MARGIN)
        val nearest = Vec3(
            eye.x.coerceIn(box.minX, box.maxX),
            eye.y.coerceIn(box.minY, box.maxY),
            eye.z.coerceIn(box.minZ, box.maxZ),
        )
        return nearest.add(box.center.subtract(nearest).scale(AIM_POINT_INSET))
    }

    private fun calcRotation(player: Player, t: LivingEntity): Pair<Float, Float> {
        val eye = player.eyePosition
        val aim = aimVec(player, t)
        val dx = aim.x - eye.x
        val dy = aim.y - eye.y
        val dz = aim.z - eye.z
        val horizDist = sqrt(dx * dx + dz * dz)
        val yaw   = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizDist))).toFloat()
        return yaw to pitch
    }

    private fun isWeapon(stack: ItemStack): Boolean =
        !stack.isEmpty &&
            (stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES) || stack.item is TridentItem)

    private fun canSeeForSwap(player: Player, entity: LivingEntity): Boolean =
        !visibilityCheck.value || player.hasLineOfSight(entity)

    private fun findBestWeapon(player: Player): Int {
        var bestSlot = -1
        var bestRank = -1
        var bestDamage = -1.0

        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (!isWeapon(stack)) continue

            // A diamond axe out damages a diamond sword on paper, so raw damage alone would
            // always pick the axe. Swords win outright and damage only breaks ties.
            val rank = if (stack.`is`(ItemTags.SWORDS)) 1 else 0
            val damage = weaponDamage(stack)
            if (rank > bestRank || (rank == bestRank && damage > bestDamage)) {
                bestRank = rank
                bestDamage = damage
                bestSlot = i
            }
        }
        return bestSlot
    }

    private fun weaponDamage(stack: ItemStack): Double {
        val modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS) ?: return 0.0
        var damage = 0.0
        modifiers.forEach(EquipmentSlot.MAINHAND) { attribute, modifier ->
            if (attribute == Attributes.ATTACK_DAMAGE &&
                modifier.operation() == AttributeModifier.Operation.ADD_VALUE
            ) {
                damage += modifier.amount()
            }
        }
        return damage
    }

    private const val ATTACK_INTENT_WINDOW_MS = 100L
    private const val MAX_CLICKS_PER_TICK = 6
    private const val MIN_CLICK_DELAY_MS = 33L
    private const val MAX_CLICK_DELAY_MS = 1000L
    private const val CLICK_CATCHUP_LIMIT_MS = 200L
    private const val HITBOX_MARGIN = 0.05
    private const val AIM_POINT_INSET = 0.35

    override fun hudInfo(): String {
        return when (mode.value) {
            Mode.CPS -> {
                val (lo, hi) = cps.value
                if (hi > lo) "%.1f-%.1f cps".format(lo, hi) else "%.1f cps".format(lo)
            }
            Mode.SEQUENTIAL -> "sequential"
        }
    }
}
