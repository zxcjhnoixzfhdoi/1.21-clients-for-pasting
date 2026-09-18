package onl.luka.grizzly.module.modules.minigames.bedwars

import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player

internal object BedwarsContext {
    private const val BEDWARS_SERVER_TYPE = "BEDWARS"

    @Volatile
    private var inBedwarsGame = false
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        val api = HypixelModAPI.getInstance()
        api.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        api.createHandler(ClientboundLocationPacket::class.java) { packet ->
            inBedwarsGame = packet.isBedwarsGame()
        }
    }

    fun isActive(client: Minecraft, requireBedwars: Boolean): Boolean {
        if (client.player == null || client.level == null) return false
        return !requireBedwars || inBedwarsGame
    }

    fun isEnemy(local: Player, other: Player, ignoreTeams: Boolean): Boolean {
        if (other === local || !other.isAlive || other.isSpectator) return false
        if (!ignoreTeams) return true

        val localTeam = local.team
        val otherTeam = other.team
        if (localTeam != null && otherTeam != null && localTeam.isAlliedTo(otherTeam)) return false

        val localHelmet = local.getItemBySlot(EquipmentSlot.HEAD)
        val otherHelmet = other.getItemBySlot(EquipmentSlot.HEAD)
        val localColor = localHelmet.get(DataComponents.DYED_COLOR)?.rgb()
        val otherColor = otherHelmet.get(DataComponents.DYED_COLOR)?.rgb()
        if (localColor != null && localColor == otherColor) {
            return false
        }
        return true
    }

    fun reset() {
        inBedwarsGame = false
    }

    private fun ClientboundLocationPacket.isBedwarsGame(): Boolean {
        val type = serverType.orElse(null)?.name() ?: return false
        return type.equals(BEDWARS_SERVER_TYPE, ignoreCase = true) &&
            lobbyName.isEmpty
    }
}
