package onl.luka.grizzly.module.modules.player

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.combat.actualAttackReach
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.useItemStrict
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.TridentItem
import kotlin.math.floor
import kotlin.math.sqrt

object TimerRange : Module(
    name = "Timer Range",
    description = "Skips game time to close the final gap to an opponent",
    category = Category.PLAYER,
) {
    enum class Mode { REGULAR, LEGIT, RICOCHET }

    private enum class Phase { IDLE, BURST, SLOWDOWN, RICOCHET }

    private val mode = enum("mode", Mode.REGULAR)
    private val activationRange = floatRange("activation range", 3.0f to 4.2f, 2.0f, 8.0f, 2)
    private val timerSpeed = float("timer speed", 0.0f, 0.0f, 1.0f)
    private val duration = int("duration (ms)", 100, 25, 500)
    private val reEntryTime = int("re-entry time (ms)", 100, 0, 1_000).also {
        it.visibleWhen = { mode.value == Mode.RICOCHET }
    }
    private val cooldown = int("cooldown (ms)", 750, 0, 5_000)
    private val blockWhileFrozen = boolean("block while frozen", false).also {
        it.visibleWhen = { mode.value == Mode.REGULAR }
    }

    private val requireLeftClick = boolean("left mouse pressed", true)
    private val requireWeapon = boolean("holding weapon", true)
    private val requireNotUsingItem = boolean("not using item", true)

    @Volatile
    private var phase = Phase.IDLE
    private var cycleMode = Mode.REGULAR
    private var cycleDurationMs = 0.0
    private var cycleReEntryMs = 0.0
    private var cycleTimerSpeed = 0.0
    private var phaseRemainingMs = 0.0
    private var burstTimeMs = 0.0
    private var fractionalOutputMs = 0.0
    @Volatile
    private var currentTimerMultiplier = 1.0
    private var lastTeleportMs = Long.MIN_VALUE
    private var ownsBlock = false

    override fun onEnabled() {
        resetCycle(releaseBlock = true)
        lastTeleportMs = Long.MIN_VALUE
    }

    override fun onDisabled() {
        resetCycle(releaseBlock = true)
        lastTeleportMs = Long.MIN_VALUE
    }

    override fun onTick(client: Minecraft) {
        val player = client.player
        val level = client.level
        if (player == null || level == null || client.gui.screen() != null || player.isDeadOrDying) {
            resetCycle(releaseBlock = true)
            return
        }

        if (phase == Phase.SLOWDOWN && cycleMode == Mode.REGULAR && blockWhileFrozen.value) {
            startFreezeBlock(client)
        } else {
            releaseFreezeBlock(client)
        }

        if (phase != Phase.IDLE || !conditionsMet(client)) return

        val now = System.currentTimeMillis()
        if (lastTeleportMs != Long.MIN_VALUE && now - lastTeleportMs < cooldown.value) return

        val (minimumRange, maximumRange) = activationRange.value
        val minimumTriggerRange = maxOf(minimumRange, actualAttackReach(player).toFloat())
        val target = level.players()
            .asSequence()
            .filter { target ->
                target !== player &&
                    !target.isDeadOrDying &&
                    !target.isSpectator &&
                    TargetFilter.isValidTarget(player, target)
            }
            .map { target -> target to player.distanceTo(target) }
            .filter { (_, distance) -> distance in minimumTriggerRange..maximumRange }
            .minByOrNull { (_, distance) -> distance }
            ?.first
            ?: return

        if (!player.hasLineOfSight(target) || !isMovingToward(player.x, player.z, player.deltaMovement.x, player.deltaMovement.z, target.x, target.z)) {
            return
        }

        cycleMode = mode.value
        cycleDurationMs = duration.value.toDouble()
        cycleReEntryMs = reEntryTime.value.toDouble()
        cycleTimerSpeed = timerSpeed.value.toDouble().coerceIn(0.0, 1.0)
        phaseRemainingMs = cycleDurationMs
        burstTimeMs = cycleDurationMs * (1.0 - cycleTimerSpeed)
        phase = when (cycleMode) {
            Mode.REGULAR -> Phase.BURST
            Mode.LEGIT, Mode.RICOCHET -> Phase.SLOWDOWN
        }
    }

    override fun hudInfo(): String {
        val modeName = mode.value.name.lowercase().replaceFirstChar { it.uppercase() }
        val displayedSpeed = if (phase == Phase.IDLE) timerSpeed.value.toDouble() else currentTimerMultiplier
        return "$modeName ${formatTimerSpeed(displayedSpeed)}x ${duration.value}ms"
    }

    /** Called by DeltaTrackerMixin before vanilla converts elapsed time into ticks. */
    fun isControllingTime(): Boolean = isEnabled() && phase != Phase.IDLE

    /**
     * Applies one timer cycle using real elapsed time, so a 0x slowdown can
     * still end while client game ticks are frozen.
     */
    @Synchronized
    fun transformElapsedTime(elapsedMs: Long): Long {
        val elapsed = elapsedMs.coerceAtLeast(0L).toDouble()
        if (!isEnabled()) {
            resetCycle(releaseBlock = false)
            return elapsed.toLong()
        }

        var realRemaining = elapsed
        var adjusted = 0.0
        var transitions = 0

        while (transitions++ < 8) {
            when (phase) {
                Phase.IDLE -> {
                    currentTimerMultiplier = 1.0
                    adjusted += realRemaining
                    realRemaining = 0.0
                    break
                }
                Phase.BURST -> {
                    currentTimerMultiplier = 1.0
                    adjusted += burstTimeMs
                    if (cycleMode == Mode.REGULAR) {
                        phase = Phase.SLOWDOWN
                        phaseRemainingMs = cycleDurationMs
                    } else {
                        finishCycle()
                    }
                }
                Phase.SLOWDOWN -> {
                    if (realRemaining <= 0.0) break
                    currentTimerMultiplier = cycleTimerSpeed
                    val consumed = minOf(realRemaining, phaseRemainingMs)
                    adjusted += consumed * cycleTimerSpeed
                    realRemaining -= consumed
                    phaseRemainingMs -= consumed
                    if (phaseRemainingMs > 0.0) break

                    when (cycleMode) {
                        Mode.REGULAR -> finishCycle()
                        Mode.LEGIT -> phase = Phase.BURST
                        Mode.RICOCHET -> {
                            if (cycleReEntryMs <= 0.0) {
                                finishCycle()
                            } else {
                                phase = Phase.RICOCHET
                                phaseRemainingMs = cycleReEntryMs
                            }
                        }
                    }
                }
                Phase.RICOCHET -> {
                    if (realRemaining <= 0.0) break
                    val consumed = minOf(realRemaining, phaseRemainingMs)
                    val multiplier = 2.0 - cycleTimerSpeed
                    currentTimerMultiplier = multiplier
                    adjusted += consumed * multiplier
                    realRemaining -= consumed
                    phaseRemainingMs -= consumed
                    if (phaseRemainingMs > 0.0) break
                    finishCycle()
                }
            }
        }

        val total = adjusted + fractionalOutputMs
        val whole = floor(total).toLong()
        fractionalOutputMs = total - whole
        return whole
    }

    private fun finishCycle() {
        phase = Phase.IDLE
        phaseRemainingMs = 0.0
        currentTimerMultiplier = 1.0
        lastTeleportMs = System.currentTimeMillis()
    }

    private fun conditionsMet(client: Minecraft): Boolean {
        val player = client.player ?: return false
        if (requireLeftClick.value && !InputUtil.isPhysicalKeyDown(client.options.keyAttack)) return false
        if (requireWeapon.value && !isWeapon(player.mainHandItem)) return false
        if (requireNotUsingItem.value && player.isUsingItem) return false

        return true
    }

    private fun isWeapon(stack: ItemStack): Boolean =
        stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES) || stack.item is TridentItem

    private fun isMovingToward(
        playerX: Double,
        playerZ: Double,
        velocityX: Double,
        velocityZ: Double,
        targetX: Double,
        targetZ: Double,
    ): Boolean {
        val dx = targetX - playerX
        val dz = targetZ - playerZ
        val distance = sqrt(dx * dx + dz * dz)
        if (distance < 1.0e-4) return false
        return (velocityX * dx + velocityZ * dz) / distance > 0.01
    }

    private fun startFreezeBlock(client: Minecraft) {
        if (ownsBlock) return
        val player = client.player ?: return
        if (player.isUsingItem) return

        val hand = InteractionHand.entries.firstOrNull { player.getItemInHand(it).canBlock() } ?: return
        val result = useItemStrict(hand) ?: return
        if (!result.isUseItemSuccess) return

        ownsBlock = true
        client.options.keyUse.setDown(true)
    }

    private fun releaseFreezeBlock(client: Minecraft = Minecraft.getInstance()) {
        if (!ownsBlock) return
        ownsBlock = false
        val player = client.player
        if (player != null) {
            client.gameMode?.releaseUsingItem(player) ?: player.stopUsingItem()
        }
        client.options.keyUse.setDown(InputUtil.isPhysicalKeyDown(client.options.keyUse))
    }

    private fun ItemStack.canBlock(): Boolean =
        has(DataComponents.BLOCKS_ATTACKS) ||
            get(DataComponents.CONSUMABLE)?.animation == ItemUseAnimation.BLOCK

    @Synchronized
    private fun resetCycle(releaseBlock: Boolean) {
        phase = Phase.IDLE
        cycleMode = mode.value
        cycleDurationMs = 0.0
        cycleReEntryMs = 0.0
        cycleTimerSpeed = 0.0
        phaseRemainingMs = 0.0
        burstTimeMs = 0.0
        fractionalOutputMs = 0.0
        currentTimerMultiplier = 1.0
        if (releaseBlock) releaseFreezeBlock()
    }

    private fun formatTimerSpeed(speed: Number): String {
        val value = speed.toDouble()
        return if (value == value.toInt().toDouble()) value.toInt().toString() else "%.2f".format(value).trimEnd('0')
    }
}
