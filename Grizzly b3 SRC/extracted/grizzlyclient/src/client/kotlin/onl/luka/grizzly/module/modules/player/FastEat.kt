package onl.luka.grizzly.module.modules.player

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object FastEat : Module(
    name = "Fast Eat",
    description = "Consumes food more quickly",
    category = Category.PLAYER,
) {

    enum class Mode {
        NCP, INSTANT, TIMER, MOSPIXEL
    }

    @JvmField val mode = enum("mode", Mode.NCP)
    private val ncpUseTicks = int("use ticks", 14, 1, 31).also {
        it.visibleWhen = { mode.value == Mode.NCP }
    }
    private val ncpPackets = int("ncp packets", 20, 1, 32).also {
        it.visibleWhen = { mode.value == Mode.NCP }
    }
    private val instantPackets = int("instant packets", 32, 1, 32).also {
        it.visibleWhen = { mode.value == Mode.INSTANT }
    }
    private val timerSpeed = float("timer speed", 1.1f, 1.0f, 3.0f).also {
        it.visibleWhen = { mode.value == Mode.TIMER }
    }

    private const val MOSPIXEL_FAST_STAGE = 1
    private const val MOSPIXEL_SLOW_STAGE = 11
    private const val MOSPIXEL_RELEASES = 112
    private const val MOSPIXEL_PACKETS = 22

    private var acceleratedCurrentUse = false
    private var mospixelStage = MOSPIXEL_FAST_STAGE
    private var lastHeldItem: Item? = null
    private var lastHeldSlot = -1

    override fun onTick(client: Minecraft) {
        val player = client.player
        if (player == null) {
            acceleratedCurrentUse = false
            return
        }

        trackHeldItem(player)

        if (!player.isUsingItem || !isFood(player.useItem)) {
            acceleratedCurrentUse = false
            return
        }

        if (mode.value == Mode.MOSPIXEL) {
            tickMospixel(client, player)
            return
        }

        if (mode.value == Mode.TIMER || acceleratedCurrentUse) return

        val packetCount = when (mode.value) {
            Mode.NCP -> {
                if (player.ticksUsingItem < ncpUseTicks.value) return
                ncpPackets.value
            }
            Mode.INSTANT -> instantPackets.value
            Mode.TIMER, Mode.MOSPIXEL -> return
        }

        acceleratedCurrentUse = true
        repeat(packetCount) { sendStatus(player) }
        client.gameMode?.releaseUsingItem(player)
    }

    private fun tickMospixel(client: Minecraft, player: LocalPlayer) {
        if (player.ticksUsingItem <= mospixelStage) return
        val gameMode = client.gameMode ?: return

        if (mospixelStage == MOSPIXEL_FAST_STAGE) {
            repeat(MOSPIXEL_RELEASES) { gameMode.releaseUsingItem(player) }
            mospixelStage = MOSPIXEL_SLOW_STAGE
        } else {
            repeat(MOSPIXEL_PACKETS) { sendStatus(player) }
            gameMode.releaseUsingItem(player)
            mospixelStage = MOSPIXEL_FAST_STAGE
        }
    }

    private fun trackHeldItem(player: LocalPlayer) {
        val slot = player.inventory.selectedSlot
        val item = player.inventory.selectedItem.item
        if (slot == lastHeldSlot && item === lastHeldItem) return

        lastHeldSlot = slot
        lastHeldItem = item
        mospixelStage = MOSPIXEL_FAST_STAGE
    }

    private fun sendStatus(player: LocalPlayer) {
        player.connection.send(
            ServerboundMovePlayerPacket.StatusOnly(
                player.onGround(),
                player.horizontalCollision,
            )
        )
    }

    internal fun finishInstantly(player: LocalPlayer) {
        repeat(instantPackets.value) { sendStatus(player) }
        Minecraft.getInstance().gameMode?.releaseUsingItem(player)
    }

    @JvmStatic
    fun timerSpeedMultiplier(): Float =
        if (isEnabled() && mode.value == Mode.TIMER && isEating()) timerSpeed.value else 1.0f

    private fun isEating(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        return player.isUsingItem && isFood(player.useItem)
    }

    private fun isFood(stack: ItemStack): Boolean =
        !stack.isEmpty &&
            stack.get(DataComponents.FOOD) != null &&
            stack.get(DataComponents.CONSUMABLE) != null

    override fun onEnabled() {
        reset()
    }

    override fun onDisabled() {
        reset()
    }

    private fun reset() {
        acceleratedCurrentUse = false
        mospixelStage = MOSPIXEL_FAST_STAGE
        lastHeldItem = null
        lastHeldSlot = -1
    }

    override fun hudInfo(): String = mode.value.name.lowercase()
}
