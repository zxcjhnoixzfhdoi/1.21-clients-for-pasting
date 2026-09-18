package onl.luka.grizzly.module.modules.anarchy

import onl.luka.grizzly.mixin.client.ClientLevelAccessor
import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items

object AutoHvHGapple : Module(
    name = "Auto HvH Gapple",
    description = "Eats gapples in a single tick without touching your held slot",
    category = Category.ANARCHY,
) {

    private val triggerHealth = float("health", 14f, 1f, 20f)
    private val enchantedOnly = boolean("enchanted only", true)
    private val silentSwap = boolean("silent swap", true)
    private val packets = int("packets", 32, 1, 32)
    private val delay = int("delay", 250, 0, 2000)

    private var lastEat = 0L

    override fun onEnabled() {
        lastEat = 0L
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        if (client.gui.screen() != null) return
        if (player.health > triggerHealth.value) return

        val now = System.currentTimeMillis()
        if (now - lastEat < delay.value) return

        val slot = findGapple(player)
        if (slot == -1) return

        lastEat = now
        if (silentSwap.value) eatSilently(client, player, slot) else eatWithSwap(client, player, slot)
    }

    /**
     * Held slot never moves. The server is told about the swap, the use and the release inside one
     * tick, so the whole apple lands before anything can see a different item in hand.
     */
    private fun eatSilently(client: Minecraft, player: LocalPlayer, slot: Int) {
        val level = client.level ?: return
        val connection = player.connection
        val heldSlot = player.inventory.selectedSlot

        connection.send(ServerboundSetCarriedItemPacket(slot))
        val predictions = (level as ClientLevelAccessor).`grizzly$blockStatePredictionHandler`()
        predictions.startPredicting().use { handler ->
            connection.send(
                ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND,
                    handler.currentSequence(),
                    player.yRot,
                    player.xRot,
                )
            )
        }
        sendFinish(player)
        connection.send(ServerboundSetCarriedItemPacket(heldSlot))
    }

    private fun eatWithSwap(client: Minecraft, player: LocalPlayer, slot: Int) {
        val heldSlot = player.inventory.selectedSlot
        if (heldSlot != slot) player.inventory.setSelectedSlot(slot)
        client.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
        sendFinish(player)
        if (heldSlot != slot) player.inventory.setSelectedSlot(heldSlot)
    }

    /** The instant trick: burst the use ticks through as status packets, then release. */
    private fun sendFinish(player: LocalPlayer) {
        repeat(packets.value) {
            player.connection.send(
                ServerboundMovePlayerPacket.StatusOnly(
                    player.onGround(),
                    player.horizontalCollision,
                )
            )
        }
        player.connection.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN,
            )
        )
    }

    private fun findGapple(player: LocalPlayer): Int {
        var fallback = -1
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            if (stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)) return slot
            if (!enchantedOnly.value && stack.`is`(Items.GOLDEN_APPLE) && fallback == -1) fallback = slot
        }
        return fallback
    }
}
