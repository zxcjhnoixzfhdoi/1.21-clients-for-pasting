package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import kotlin.random.Random

object AutoReel : Module(
    "Auto Reel",
    "Automatically reels in a SkyBlock fishing hook after its catch alert",
    Category.SKYBLOCK,
) {
    private const val ROTATION_OWNER = "auto-reel-hyperion"

    private val reelDelayMs = intRange("reel delay ms", 200 to 285, 0, 1_000)
    private val useHoldMs = intRange("use hold ms", 75 to 115, 1, 300)
    private val hyperionSwap = boolean("hyperion swap", false)
    private val hyperionDelayMs = intRange("hyperion delay ms", 180 to 220, 0, 1_000).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val hyperionUseHoldMs = intRange("hyperion use hold ms", 75 to 115, 1, 300).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val recastDelayMs = intRange("recast delay ms", 400 to 500, 0, 1_000).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val recastUseHoldMs = intRange("recast use hold ms", 75 to 115, 1, 300).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val lookDownPitch = float("look down pitch", 89f, 45f, 90f).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val lookSpeed = float("look speed", 30f, 1f, 180f).also {
        it.visibleWhen = { hyperionSwap.value }
    }
    private val lookTimeoutMs = int("look timeout ms", 2_000, 250, 5_000).also {
        it.visibleWhen = { hyperionSwap.value }
    }

    private enum class ActionState {
        IDLE,
        REEL_HOLD,
        AIMING_DOWN,
        HYPERION_HOLD,
        RECAST_DELAY,
        RECAST_HOLD,
    }

    private var hookId = -1
    private var indicatorId = -1
    private var reelAtMs = 0L
    private var reeledThisCast = false
    private var actionState = ActionState.IDLE
    private var actionAtMs = 0L
    private var aimTimeoutAtMs = 0L
    private var aimYaw = 0f
    private var rodSlot = -1
    private var hyperionSlot = -1
    private var ownsUse = false

    override fun onEnabled() {
        reset(Minecraft.getInstance())
    }

    override fun onDisabled() {
        reset(Minecraft.getInstance())
    }

    override fun hudInfo(): String {
        val (min, max) = reelDelayMs.value
        return if (min == max) "${min}ms" else "$min-${max}ms"
    }

    override fun onTick(client: Minecraft) {
        val now = System.currentTimeMillis()
        if (actionState != ActionState.IDLE) {
            tickAction(client, now)
            return
        }

        val player = client.player ?: return reset(client)
        val level = client.level ?: return reset(client)
        val hook = player.fishing ?: return reset(client)

        if (!isHoldingFishingRod(client)) return reset(client)

        if (hook.id != hookId) {
            hookId = hook.id
            indicatorId = -1
            reelAtMs = 0L
            reeledThisCast = false
        }

        val indicator = level.getEntity(indicatorId) as? ArmorStand
            ?: findIndicator(client, hook.tickCount)?.also { indicatorId = it.id }
            ?: return

        if (!indicator.isAlive) {
            indicatorId = -1
            reelAtMs = 0L
            return
        }

        if (!reeledThisCast && reelAtMs == 0L && indicator.name.string.trim() == CATCH_ALERT) {
            reelAtMs = now + randomMillis(reelDelayMs.value)
        }

        if (reelAtMs != 0L && now >= reelAtMs) {
            reelAtMs = 0L
            if (client.gui.screen() != null || player.fishing?.id != hookId || !isHoldingFishingRod(client)) {
                return
            }

            rodSlot = player.inventory.selectedSlot
                .takeIf { player.mainHandItem.item is FishingRodItem }
                ?: -1
            reeledThisCast = true
            startUse(client, ActionState.REEL_HOLD, now + randomMillis(useHoldMs.value))
        }
    }

    private fun tickAction(client: Minecraft, now: Long) {
        val player = client.player
        if (player == null || client.level == null || client.gui.screen() != null) {
            reset(client)
            return
        }

        when (actionState) {
            ActionState.IDLE -> Unit
            ActionState.REEL_HOLD -> {
                if (now < actionAtMs) {
                    holdUse(client)
                    return
                }

                releaseUse(client)
                hyperionSlot = if (hyperionSwap.value && rodSlot in 0..8) {
                    findHyperionSlot(client)
                } else {
                    -1
                }
                if (hyperionSlot !in 0..8) {
                    finishSequence(client)
                    return
                }

                aimYaw = player.yRot
                actionState = ActionState.AIMING_DOWN
                actionAtMs = now + randomMillis(hyperionDelayMs.value)
                aimTimeoutAtMs = now + lookTimeoutMs.value
                updateDownwardRotation()
            }
            ActionState.AIMING_DOWN -> {
                val currentHyperionSlot = resolveHyperionSlot(client)
                if (currentHyperionSlot !in 0..8) {
                    finishSequence(client)
                    return
                }

                updateDownwardRotation()
                val reachedTarget = RotationManager.hasReachedTarget(2f)
                if (!reachedTarget && now >= aimTimeoutAtMs) {
                    finishSequence(client)
                    return
                }
                if (now < actionAtMs || !reachedTarget) return

                hyperionSlot = currentHyperionSlot
                player.inventory.selectedSlot = hyperionSlot
                startUse(
                    client,
                    ActionState.HYPERION_HOLD,
                    now + randomMillis(hyperionUseHoldMs.value),
                )
            }
            ActionState.HYPERION_HOLD -> {
                if (now < actionAtMs) {
                    holdUse(client)
                    return
                }

                releaseUse(client)
                RotationManager.clearRotation(ROTATION_OWNER)
                actionState = ActionState.RECAST_DELAY
                actionAtMs = now + randomMillis(recastDelayMs.value)
            }
            ActionState.RECAST_DELAY -> {
                if (now < actionAtMs) return
                if (!restoreRodSlot(client)) {
                    finishSequence(client)
                    return
                }

                startUse(
                    client,
                    ActionState.RECAST_HOLD,
                    now + randomMillis(recastUseHoldMs.value),
                )
            }
            ActionState.RECAST_HOLD -> {
                if (now < actionAtMs) {
                    holdUse(client)
                    return
                }

                releaseUse(client)
                finishSequence(client)
            }
        }
    }

    private fun findIndicator(client: Minecraft, hookAge: Int): ArmorStand? {
        val candidates = client.level
            ?.entitiesForRendering()
            ?.filterIsInstance<ArmorStand>()
            ?.filter { stand ->
                stand.isAlive &&
                    stand.tickCount <= hookAge + MAX_INDICATOR_AGE_LEAD &&
                    isHookDisplayName(stand.name.string.trim())
            }
            ?.toList()
            .orEmpty()

        return candidates.singleOrNull()
    }

    private fun isHoldingFishingRod(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return player.mainHandItem.item is FishingRodItem ||
            player.offhandItem.item is FishingRodItem
    }

    private fun findHyperionSlot(client: Minecraft): Int {
        val inventory = client.player?.inventory ?: return -1
        return (0..8).firstOrNull { isHyperion(inventory.getItem(it)) } ?: -1
    }

    private fun resolveHyperionSlot(client: Minecraft): Int {
        val inventory = client.player?.inventory ?: return -1
        if (hyperionSlot in 0..8 && isHyperion(inventory.getItem(hyperionSlot))) {
            return hyperionSlot
        }
        return findHyperionSlot(client)
    }

    private fun isHyperion(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return SkyBlockUtils.skyBlockId(stack).equals(HYPERION_ID, ignoreCase = true) ||
            stack.hoverName.string.contains("Hyperion", ignoreCase = true)
    }

    private fun updateDownwardRotation() {
        SkyBlockUtils.preparePerspectiveRotation()
        RotationManager.setTargetRotation(aimYaw, lookDownPitch.value, ROTATION_OWNER)
        RotationManager.quickTick(lookSpeed.value)
    }

    private fun restoreRodSlot(client: Minecraft): Boolean {
        val inventory = client.player?.inventory ?: return false
        if (rodSlot !in 0..8 || inventory.getItem(rodSlot).item !is FishingRodItem) return false
        inventory.selectedSlot = rodSlot
        return true
    }

    private fun isHookDisplayName(name: String): Boolean =
        name == CATCH_ALERT || COUNTDOWN_PATTERN.matches(name)

    private fun randomMillis(range: Pair<Int, Int>): Long {
        val (rawMin, rawMax) = range
        val min = minOf(rawMin, rawMax)
        val max = maxOf(rawMin, rawMax)
        return if (max > min) Random.nextInt(min, max + 1).toLong() else min.toLong()
    }

    private fun startUse(client: Minecraft, state: ActionState, releaseAtMs: Long) {
        actionState = state
        actionAtMs = releaseAtMs
        ownsUse = true
        client.options.keyUse.setDown(true)
    }

    private fun holdUse(client: Minecraft) {
        if (ownsUse) client.options.keyUse.setDown(true)
    }

    private fun releaseUse(client: Minecraft) {
        if (!ownsUse) return
        client.options.keyUse.setDown(InputUtil.isPhysicalKeyDown(client.options.keyUse))
        ownsUse = false
    }

    private fun finishSequence(client: Minecraft) {
        releaseUse(client)
        restoreRodSlot(client)
        RotationManager.clearRotation(ROTATION_OWNER)
        actionState = ActionState.IDLE
        actionAtMs = 0L
        aimTimeoutAtMs = 0L
        rodSlot = -1
        hyperionSlot = -1
        hookId = -1
        indicatorId = -1
        reelAtMs = 0L
        reeledThisCast = false
    }

    private fun reset(client: Minecraft) {
        releaseUse(client)
        restoreRodSlot(client)
        RotationManager.clearRotation(ROTATION_OWNER)
        actionState = ActionState.IDLE
        actionAtMs = 0L
        aimTimeoutAtMs = 0L
        rodSlot = -1
        hyperionSlot = -1
        hookId = -1
        indicatorId = -1
        reelAtMs = 0L
        reeledThisCast = false
    }

    private val COUNTDOWN_PATTERN = Regex("""\d+(?:\.\d+)?""")
    private const val CATCH_ALERT = "!!!"
    private const val HYPERION_ID = "HYPERION"
    private const val MAX_INDICATOR_AGE_LEAD = 5
}
