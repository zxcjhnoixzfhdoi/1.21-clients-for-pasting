package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3

object Stasis : Module(
    name = "Stasis",
    description = "Freezes your movement",
    category = Category.MOVEMENT,
) {
    private var savedMotion: Vec3? = null
    private var savedPlayer: LocalPlayer? = null

    override fun onEnabled() {
        val player = Minecraft.getInstance().player
        savedPlayer = player
        savedMotion = player?.deltaMovement
    }

    override fun onDisabled() {
        val player = Minecraft.getInstance().player
        val motion = savedMotion
        if (motion != null && player != null && player === savedPlayer) {
            player.deltaMovement = motion
        }
        savedMotion = null
        savedPlayer = null
    }

    override fun onTick(client: Minecraft) {
        client.player?.setDeltaMovement(Vec3.ZERO)
    }

    @JvmStatic
    fun shouldFreezeInput(): Boolean = isEnabled() && Minecraft.getInstance().player != null

    @JvmStatic
    fun shouldCancelPacket(packet: Packet<*>): Boolean =
        isEnabled() && packet is ServerboundMovePlayerPacket && packet !is ServerboundMovePlayerPacket.Rot
}
