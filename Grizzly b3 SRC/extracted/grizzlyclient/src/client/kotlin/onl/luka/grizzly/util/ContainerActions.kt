package onl.luka.grizzly.util

import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput

object ContainerActions {
    private var owner: Any? = null
    private var ownerTick = -1

    fun click(
        owner: Any,
        player: Player,
        gameMode: MultiPlayerGameMode,
        slot: Int,
        button: Int,
        input: ContainerInput,
    ): Boolean {
        if (slot < 0) return false
        if (!claim(owner, player)) return false

        gameMode.handleContainerInput(player.containerMenu.containerId, slot, button, input, player)
        return true
    }

    private fun claim(module: Any, player: Player): Boolean {
        if (player.tickCount != ownerTick) {
            ownerTick = player.tickCount
            owner = module
            return true
        }
        return owner === module
    }

    fun reset() {
        owner = null
        ownerTick = -1
    }

    fun playerSlot(menu: AbstractContainerMenu, player: Player, inventorySlot: Int): Int? =
        menu.slots.firstOrNull {
            it.container === player.inventory && it.getContainerSlot() == inventorySlot
        }?.index
}
