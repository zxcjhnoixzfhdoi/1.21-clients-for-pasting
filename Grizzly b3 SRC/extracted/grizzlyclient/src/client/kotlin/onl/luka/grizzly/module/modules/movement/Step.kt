package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.stats.Stats
import net.minecraft.world.phys.Vec3
import kotlin.math.max

object Step : Module("Step", "Allows you to walk up taller blocks", Category.MOVEMENT) {

    enum class Mode {
        VANILLA, NCP, OLD_NCP
    }

    @JvmField val mode = enum("mode", Mode.VANILLA)
    private val height = float("height", 1.0f, 0.6f, 10.0f)
    private val onGroundOnly = boolean("on ground only", true)
    private val delay = int("delay (ms)", 0, 0, 500)

    private var lastStepAt = 0L
    private var oldNcpPacketPending = false

    override fun onEnabled() {
        lastStepAt = 0L
        oldNcpPacketPending = false
    }

    override fun onTick(client: Minecraft) {
        if (mode.value != Mode.OLD_NCP) oldNcpPacketPending = false
    }

    override fun onDisabled() {
        oldNcpPacketPending = false
    }

    @JvmStatic
    fun resolveStepHeight(player: LocalPlayer, vanillaHeight: Float): Float {
        if (!canStep(player)) return vanillaHeight
        return max(vanillaHeight, height.value)
    }

    @JvmStatic
    fun shouldTrackMovement(player: LocalPlayer, movement: Vec3): Boolean =
        canStep(player) && movement.y <= 0.0 && movement.horizontalDistanceSqr() > 1.0e-8

    @JvmStatic
    fun onMovementComplete(player: LocalPlayer, startX: Double, startY: Double, startZ: Double) {
        if (!isEnabled() || player !== Minecraft.getInstance().player) return

        val steppedHeight = player.y - startY
        if (steppedHeight <= FULL_STEP_THRESHOLD || steppedHeight > height.value + STEP_EPSILON) return

        lastStepAt = System.currentTimeMillis()
        when (mode.value) {
            Mode.VANILLA -> Unit
            Mode.NCP -> {
                player.awardStat(Stats.JUMP)
                val horizontalCollision = player.horizontalCollision
                player.connection.send(
                    ServerboundMovePlayerPacket.Pos(
                        startX,
                        startY + NCP_FIRST_OFFSET,
                        startZ,
                        false,
                        horizontalCollision,
                    )
                )
                player.connection.send(
                    ServerboundMovePlayerPacket.Pos(
                        startX,
                        startY + NCP_SECOND_OFFSET,
                        startZ,
                        false,
                        horizontalCollision,
                    )
                )
            }
            Mode.OLD_NCP -> oldNcpPacketPending = true
        }
    }

    @JvmStatic
    fun adjustOutgoingMovementPacket(packet: ServerboundMovePlayerPacket): ServerboundMovePlayerPacket {
        if (!oldNcpPacketPending) return packet
        oldNcpPacketPending = false

        if (!isEnabled() || mode.value != Mode.OLD_NCP || !packet.hasPosition()) return packet
        val player = Minecraft.getInstance().player ?: return packet
        val x = packet.getX(player.x)
        val y = packet.getY(player.y) + OLD_NCP_OFFSET
        val z = packet.getZ(player.z)

        return if (packet.hasRotation()) {
            ServerboundMovePlayerPacket.PosRot(
                x,
                y,
                z,
                packet.getYRot(player.yRot),
                packet.getXRot(player.xRot),
                packet.isOnGround,
                packet.horizontalCollision(),
            )
        } else {
            ServerboundMovePlayerPacket.Pos(
                x,
                y,
                z,
                packet.isOnGround,
                packet.horizontalCollision(),
            )
        }
    }

    private fun canStep(player: LocalPlayer): Boolean =
        isEnabled() &&
            player === Minecraft.getInstance().player &&
            isGrounded(player) &&
            System.currentTimeMillis() - lastStepAt >= delay.value

    /**
     * The step height is asked for partway through move(), so onGround() there is still last
     * tick's value. On its own it stays true for the tick you leave the ground, which is why
     * flight and bhopping could step. Rising or flying rules those out.
     */
    private fun isGrounded(player: LocalPlayer): Boolean {
        if (!player.onGround()) return false
        if (!onGroundOnly.value) return true
        if (player.abilities.flying) return false
        return player.deltaMovement.y <= 0.0
    }

    override fun hudInfo(): String = mode.value.name.replace("_", " ")

    private const val FULL_STEP_THRESHOLD = 0.5
    private const val STEP_EPSILON = 1.0e-3
    private const val NCP_FIRST_OFFSET = 0.41999998688698
    private const val NCP_SECOND_OFFSET = 0.7531999805212
    private const val OLD_NCP_OFFSET = 0.07
}
