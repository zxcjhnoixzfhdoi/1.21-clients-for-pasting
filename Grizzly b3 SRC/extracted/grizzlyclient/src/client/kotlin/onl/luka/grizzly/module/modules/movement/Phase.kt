package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Phase : Module("Phase", "Walks you through blocks", Category.MOVEMENT) {

    enum class Mode { VANILLA, NCP, NORULES }

    @JvmField val mode = enum("mode", Mode.NORULES)

    private val noRulesReach = float("norules offset", 2.256f, 0.5f, 6.0f).also {
        it.visibleWhen = { mode.value == Mode.NORULES }
    }
    private val noRulesDelay = int("norules delay (ms)", 40, 0, 500).also {
        it.visibleWhen = { mode.value == Mode.NORULES }
    }

    private const val SCAN_STEP = 0.03125
    private const val MAX_SCAN = 8.0
    private const val ENTRY_FRACTION = 0.75
    private const val PENDING_TICKS = 10

    private const val NCP_NUDGE = 0.06
    private const val NCP_PUSH = 1.7
    private const val NCP_TICKS = 3

    private var lastShove = 0L
    private var pendingTicks = 0
    private var ncpTicks = 0
    private var ncpAngle = 0.0f

    override fun onEnabled() {
        lastShove = 0L
        pendingTicks = 0
        ncpTicks = 0
        Minecraft.getInstance().player?.noPhysics = false
    }

    override fun onDisabled() {
        pendingTicks = 0
        ncpTicks = 0
        Minecraft.getInstance().player?.noPhysics = false
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        if (pendingTicks > 0) pendingTicks--

        when (mode.value) {
            Mode.VANILLA -> tickVanilla(client, player)
            Mode.NCP -> tickNcp(player)
            Mode.NORULES -> tickNoRules(client, player)
        }
    }

    private fun insideBlock(client: Minecraft, player: LocalPlayer): Boolean {
        val level = client.level ?: return false
        return !level.noCollision(player, player.boundingBox.deflate(1.0E-5))
    }

    private fun tickNoRules(client: Minecraft, player: LocalPlayer) {
        val stuck = insideBlock(client, player)

        if (stuck) {
            player.noPhysics = true
            player.deltaMovement = player.deltaMovement.multiply(1.0, 0.0, 1.0)
        } else {
            player.noPhysics = false
        }

        val now = System.currentTimeMillis()
        val ready = now - lastShove >= noRulesDelay.value
        if (!ready || !player.horizontalCollision) return
        if (stuck && !player.isShiftKeyDown) return

        // One block along the facing yaw, which is what walks you out the far side.
        val yaw = Math.toRadians(player.yRot.toDouble())
        player.setPos(player.x - sin(yaw), player.y, player.z + cos(yaw))
        lastShove = now
    }

    private fun tickNcp(player: LocalPlayer) {
        if (!player.horizontalCollision && ncpTicks == 0) return

        if (ncpTicks == 0) ncpAngle = player.yRot
        ncpTicks++

        if (ncpTicks > NCP_TICKS) {
            ncpTicks = 0
            player.deltaMovement = player.deltaMovement.multiply(0.0, 1.0, 0.0)
            return
        }

        val distance = if (ncpTicks <= 2) NCP_NUDGE else NCP_PUSH
        val yaw = Math.toRadians(ncpAngle.toDouble())
        player.setPos(player.x - sin(yaw) * distance, player.y, player.z + cos(yaw) * distance)
    }

    private fun tickVanilla(client: Minecraft, player: LocalPlayer) {
        if (pendingTicks > 0 || !player.horizontalCollision) return

        val level = client.level ?: return
        val motion = player.deltaMovement
        val speed = sqrt(motion.x * motion.x + motion.z * motion.z)
        if (speed < 1.0E-5) return

        val stepX = motion.x / speed * SCAN_STEP
        val stepZ = motion.z / speed * SCAN_STEP
        val steps = (MAX_SCAN / SCAN_STEP).toInt()

        var boundary: Vec3? = null
        var exit: Vec3? = null

        for (index in 1..steps) {
            val probe = Vec3(player.x + stepX * index, player.y, player.z + stepZ * index)
            val box = player.boundingBox.move(probe.x - player.x, 0.0, probe.z - player.z)
            val blocked = !level.noCollision(player, box.deflate(1.0E-5))

            if (blocked && boundary == null) boundary = probe
            if (!blocked && boundary != null) {
                exit = probe
                break
            }
        }

        val start = boundary ?: return
        val end = exit ?: return
        val connection = player.connection

        val entry = start.add(end.subtract(start).scale(ENTRY_FRACTION))
        connection.send(ServerboundMovePlayerPacket.Pos(start.x, start.y, start.z, false, true))
        connection.send(ServerboundMovePlayerPacket.Pos(entry.x, entry.y, entry.z, false, true))
        connection.send(ServerboundMovePlayerPacket.Pos(end.x, end.y, end.z, false, true))

        player.setPos(end.x, end.y, end.z)
        pendingTicks = PENDING_TICKS
    }

    @JvmStatic
    fun adjustOutgoingMovementPacket(packet: ServerboundMovePlayerPacket): ServerboundMovePlayerPacket {
        if (!isEnabled() || mode.value != Mode.NORULES) return packet

        val player = Minecraft.getInstance().player ?: return packet
        val yaw = Math.toRadians(player.yRot.toDouble())
        val offsetX = sin(yaw) * noRulesReach.value
        val offsetZ = -cos(yaw) * noRulesReach.value

        return when (packet) {
            is ServerboundMovePlayerPacket.PosRot -> ServerboundMovePlayerPacket.PosRot(
                packet.getX(player.x) + offsetX,
                packet.getY(player.y),
                packet.getZ(player.z) + offsetZ,
                packet.getYRot(player.yRot),
                packet.getXRot(player.xRot),
                false,
                packet.horizontalCollision(),
            )
            is ServerboundMovePlayerPacket.Pos -> ServerboundMovePlayerPacket.Pos(
                packet.getX(player.x) + offsetX,
                packet.getY(player.y),
                packet.getZ(player.z) + offsetZ,
                false,
                packet.horizontalCollision(),
            )
            is ServerboundMovePlayerPacket.Rot -> ServerboundMovePlayerPacket.Rot(
                packet.getYRot(player.yRot),
                packet.getXRot(player.xRot),
                false,
                packet.horizontalCollision(),
            )
            else -> packet
        }
    }

    override fun hudInfo(): String = mode.value.name.lowercase()
}
