package onl.luka.grizzly.module.modules.skyblock

import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.module.Module
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object AutoPickaxeAbility : Module(
    "Auto Pickaxe Ability",
    "Uses a mining ability with an optional rod swap without interrupting Click Lock",
    Category.SKYBLOCK,
) {
    enum class Activation { AS_SOON_AS_READY, INTERVAL, MANUAL }

    private val activation = enum("activation", Activation.AS_SOON_AS_READY)
    private val miningSlot = int("mining tool slot", 1, 1, 9)
    private val rodSwap = boolean("rod swap", true)
    private val rodSlot = int("rod slot", 2, 1, 9)
    private val actionDelay = int("action delay ticks", 2, 1, 10)
    private val intervalSeconds = int("interval seconds", 120, 5, 600)
    private val manualKey = keybind("activate key", GLFW.GLFW_KEY_UNKNOWN)
        .onPress { if (isEnabled() && !paused) requested = true }
    private val pauseKey = keybind("pause key", GLFW.GLFW_KEY_UNKNOWN)
        .onPress { if (isEnabled()) paused = !paused }

    private enum class Step { IDLE, SELECT_ROD, USE_ROD, SELECT_TOOL, USE_ABILITY, RESUME }

    private var step = Step.IDLE
    private var waitTicks = 0
    private var requested = false
    private var paused = false
    private var savedSlot = -1
    private var lastUseAt = 0L
    private var readyConsumed = false

    override fun onDisabled() {
        finish(Minecraft.getInstance())
        paused = false
        requested = false
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        if (client.level == null) return
        MiningAbilityTracker.update(client)

        if (step != Step.IDLE) {
            if (waitTicks-- > 0) return
            when (step) {
                Step.SELECT_ROD -> {
                    player.inventory.selectedSlot = SkyBlockUtils.hotbarIndex(rodSlot.value)
                    step = Step.USE_ROD
                }
                Step.USE_ROD -> {
                    clickUse(client)
                    step = Step.SELECT_TOOL
                }
                Step.SELECT_TOOL -> {
                    player.inventory.selectedSlot = SkyBlockUtils.hotbarIndex(miningSlot.value)
                    step = Step.USE_ABILITY
                }
                Step.USE_ABILITY -> {
                    clickUse(client)
                    lastUseAt = System.currentTimeMillis()
                    readyConsumed = true
                    step = Step.RESUME
                }
                Step.RESUME -> finish(client)
                Step.IDLE -> Unit
            }
            waitTicks = actionDelay.value
            return
        }

        if (MiningAbilityTracker.state != MiningAbilityTracker.State.READY) readyConsumed = false
        if (paused) return

        val now = System.currentTimeMillis()
        val shouldActivate = requested || when (activation.value) {
            Activation.AS_SOON_AS_READY ->
                MiningAbilityTracker.state == MiningAbilityTracker.State.READY && !readyConsumed
            Activation.INTERVAL -> now - lastUseAt >= intervalSeconds.value * 1_000L
            Activation.MANUAL -> false
        }
        if (!shouldActivate) return

        requested = false
        savedSlot = player.inventory.selectedSlot
        ClickLock.suspendForAbility(true)
        client.options.keyAttack.setDown(false)
        step = if (rodSwap.value) Step.SELECT_ROD else Step.SELECT_TOOL
        waitTicks = 0
    }

    override fun hudInfo(): String =
        when {
            paused -> "Paused"
            step != Step.IDLE -> "Using"
            MiningAbilityTracker.state == MiningAbilityTracker.State.COOLDOWN ->
                "${MiningAbilityTracker.cooldownSeconds}s"
            else -> MiningAbilityTracker.state.name.lowercase().replaceFirstChar(Char::uppercase)
        }

    private fun clickUse(client: Minecraft) {
        KeyMapping.click(InputConstants.getKey(client.options.keyUse.saveString()))
    }

    private fun finish(client: Minecraft) {
        if (savedSlot in 0..8) client.player?.inventory?.selectedSlot = savedSlot
        savedSlot = -1
        step = Step.IDLE
        waitTicks = 0
        ClickLock.suspendForAbility(false)
    }
}
