package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object Flight : Module("Flight", "Allows you to fly or glide in the air", Category.MOVEMENT) {

    enum class Mode {
        VANILLA, CREATIVE, AIR_HOP, GLIDE, NCP, NORULES
    }

    enum class NcpMode {
        MOSPIXEL
    }

    enum class AntiKickMode {
        LATEST, LEGACY
    }

    @JvmField val mode = enum("mode", Mode.VANILLA)
    private val flightSpeed = float("speed", 1.0f, 0.1f, 5.0f).also {
        it.visibleWhen = { mode.value == Mode.VANILLA }
    }
    private val ncpMode = enum("ncp mode", NcpMode.MOSPIXEL).also {
        it.visibleWhen = { mode.value == Mode.NCP }
    }
    private val ncpSpeed = float("ncp speed", 1.0f, 0.3f, 2.115f).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpMode.value == NcpMode.MOSPIXEL }
    }
    private val ncpDamage = boolean("damage", false).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpMode.value == NcpMode.MOSPIXEL }
    }
    val antiKick = boolean("anti-kick", false)
    private val norulesSpeed = float("norules speed", 0.6f, 0.1f, 1.0f).also {
        it.visibleWhen = { mode.value == Mode.NORULES }
    }
    private val norulesFlyTicks = int("norules fly ticks", 15, 5, 100).also {
        it.visibleWhen = { mode.value == Mode.NORULES }
    }
    private val norulesRestTicks = int("norules rest ticks", 25, 0, 100).also {
        it.visibleWhen = { mode.value == Mode.NORULES }
    }

    val antiKickMode = enum("anti-kick mode", AntiKickMode.LEGACY).also {
        it.visibleWhen = { antiKick.value }
    }
    
    val latestInterval = int("interval", 70, 5, 80).also {
        it.visibleWhen = { antiKick.value && antiKickMode.value == AntiKickMode.LATEST }
    }
    val latestDistance = float("distance", 0.035f, 0.01f, 0.2f).also {
        it.visibleWhen = { antiKick.value && antiKickMode.value == AntiKickMode.LATEST }
    }

    val oldInterval = int("interval", 30, 5, 80).also {
        it.visibleWhen = { antiKick.value && antiKickMode.value == AntiKickMode.LEGACY }
    }

    private var tickCounter = 0
    private var targetY = 0.0
    private var ncpCurrentSpeed = NCP_BASE_SPEED
    private var norulesPhaseTicks = 0

    override fun onEnabled() {
        val player = Minecraft.getInstance().player ?: return
        tickCounter = 0
        targetY = player.y
        norulesPhaseTicks = 0

        if (mode.value == Mode.NCP && ncpMode.value == NcpMode.MOSPIXEL) {
            ncpCurrentSpeed = if (player.onGround()) {
                if (ncpDamage.value) sendNcpDamage(player)
                ncpSpeed.value.toDouble()
            } else {
                NCP_BASE_SPEED
            }
        }
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        
        when (mode.value) {
            Mode.CREATIVE -> {
                player.abilities.mayfly = true
                player.abilities.flying = true
            }
            Mode.VANILLA -> {
                player.abilities.flying = false
                
                var yVelocity = 0.0
                if (client.options.keyJump.isDown) yVelocity += flightSpeed.value * 0.5
                if (client.options.keyShift.isDown) yVelocity -= flightSpeed.value * 0.5

                player.setDeltaMovement(0.0, yVelocity, 0.0)
                setMovementSpeed(player, flightSpeed.value.toDouble() * 0.5)
            }
            Mode.GLIDE -> {
                val vec = player.deltaMovement
                if (vec.y < -0.05) {
                    player.setDeltaMovement(vec.x, -0.05, vec.z)
                }
            }
            Mode.AIR_HOP -> {
                if (client.options.keyShift.isDown) {
                    targetY = player.y
                } else if (client.options.keyJump.isDown) {
                    if (player.deltaMovement.y < 0.0 && !player.onGround()) {
                        player.jumpFromGround()
                    }
                    targetY = player.y
                } else {
                    if (!player.onGround() && player.y <= targetY && player.deltaMovement.y < 0.0) {
                        player.jumpFromGround()
                        targetY = player.y
                    }
                }
            }
            Mode.NCP -> disableVanillaFlight(player)
            Mode.NORULES -> tickNoRules(client, player)
        }

        if (antiKick.value) {
            when (antiKickMode.value) {
                AntiKickMode.LATEST -> doWurstAntiKick(player)
                AntiKickMode.LEGACY -> doOldAntiKick(player, client)
            }
        }
    }

    private fun hasMovementInput(player: LocalPlayer): Boolean {
        val move = player.input.moveVector
        return move.x != 0f || move.y != 0f
    }

    /** Applies Vestige's MoveEvent replacement at the point 26.2 consumes movement. */
    @JvmStatic
    fun modifyMovement(player: LocalPlayer, original: Vec3): Vec3 {
        if (!isEnabled()
            || mode.value != Mode.NCP
            || ncpMode.value != NcpMode.MOSPIXEL
            || player !== Minecraft.getInstance().player
        ) {
            return original
        }

        disableVanillaFlight(player)

        val speed: Double
        val vertical: Double
        if (player.onGround()) {
            player.jumpFromGround()
            speed = 0.58
            vertical = player.deltaMovement.y
        } else {
            if (player.horizontalCollision) {
                ncpCurrentSpeed = NCP_BASE_SPEED
            }
            if (!hasMovementInput(player) || ncpCurrentSpeed < NCP_BASE_SPEED) {
                ncpCurrentSpeed = NCP_BASE_SPEED
            }

            speed = ncpCurrentSpeed
            vertical = if (player.tickCount % 2 == 0) -NCP_VERTICAL_OFFSET else NCP_VERTICAL_OFFSET
            ncpCurrentSpeed -= ncpCurrentSpeed / 159.0
        }

        val horizontal = ncpHorizontalMovement(player, speed)
        return Vec3(horizontal.x, vertical, horizontal.z).also(player::setDeltaMovement)
    }

    private fun ncpHorizontalMovement(player: LocalPlayer, speed: Double): Vec3 {
        val input = player.input.moveVector
        val forward = input.y.compareTo(0f).toFloat()
        val strafe = input.x.compareTo(0f).toFloat()
        if (forward == 0f && strafe == 0f) return Vec3.ZERO

        var direction = player.yRot
        if (forward > 0f) {
            if (strafe > 0f) direction -= 45f
            else if (strafe < 0f) direction += 45f
        } else if (forward < 0f) {
            if (strafe > 0f) direction -= 135f
            else if (strafe < 0f) direction += 135f
            else direction -= 180f
        } else if (strafe > 0f) {
            direction -= 90f
        } else {
            direction += 90f
        }

        val radians = Math.toRadians(direction.toDouble())
        return Vec3(-sin(radians) * speed, 0.0, cos(radians) * speed)
    }

    fun adjustOutgoingMovementPacket(packet: ServerboundMovePlayerPacket): ServerboundMovePlayerPacket {
        if (!isEnabled() || mode.value != Mode.NORULES) return packet
        val player = Minecraft.getInstance().player ?: return packet

        // airborne check.
        val onGround = player.tickCount % 2 == 0
        if (packet.isOnGround == onGround) return packet

        return when (packet) {
            is ServerboundMovePlayerPacket.PosRot -> ServerboundMovePlayerPacket.PosRot(
                packet.getX(player.x),
                packet.getY(player.y),
                packet.getZ(player.z),
                packet.getYRot(player.yRot),
                packet.getXRot(player.xRot),
                onGround,
                packet.horizontalCollision(),
            )
            is ServerboundMovePlayerPacket.Pos -> ServerboundMovePlayerPacket.Pos(
                packet.getX(player.x),
                packet.getY(player.y),
                packet.getZ(player.z),
                onGround,
                packet.horizontalCollision(),
            )
            is ServerboundMovePlayerPacket.Rot -> ServerboundMovePlayerPacket.Rot(
                packet.getYRot(player.yRot),
                packet.getXRot(player.xRot),
                onGround,
                packet.horizontalCollision(),
            )
            is ServerboundMovePlayerPacket.StatusOnly -> ServerboundMovePlayerPacket.StatusOnly(
                onGround,
                packet.horizontalCollision(),
            )
            else -> packet
        }
    }

    @JvmStatic
    fun timerSpeedMultiplier(): Float = 1.0f

    private fun disableVanillaFlight(player: LocalPlayer) {
        if (!player.isCreative && !player.isSpectator) {
            player.abilities.mayfly = false
            player.abilities.flying = false
        }
    }

    @JvmStatic
    fun shouldSpoofGround(): Boolean = false

    private fun sendNcpDamage(player: LocalPlayer) {
        val connection = player.connection
        val x = player.x
        val y = player.y
        val z = player.z
        val horizontalCollision = player.horizontalCollision

        repeat(49) {
            connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, horizontalCollision))
            connection.send(ServerboundMovePlayerPacket.Pos(x, y, z, false, horizontalCollision))
        }
        connection.send(ServerboundMovePlayerPacket.StatusOnly(true, horizontalCollision))
    }

    private fun doWurstAntiKick(player: LocalPlayer) {
        if (tickCounter > latestInterval.value + 1) {
            tickCounter = 0
        }

        val velocity = player.deltaMovement

        when (tickCounter) {
            0 -> {
                if (velocity.y <= -latestDistance.value) {
                    tickCounter = 2
                } else {
                    player.setDeltaMovement(velocity.x, -latestDistance.value.toDouble(), velocity.z)
                }
            }
            1 -> {
                player.setDeltaMovement(velocity.x, latestDistance.value.toDouble(), velocity.z)
            }
        }

        tickCounter++
    }

    private fun doOldAntiKick(player: LocalPlayer, client: Minecraft) {
        if (tickCounter > oldInterval.value) {
            tickCounter = 0
        }

        if (tickCounter == 0) {
            goToGround18(player, client)
        }

        tickCounter++
    }

    private fun goToGround18(player: LocalPlayer, client: Minecraft) {
        var step = 1.0
        val precision = 0.0625
        var flyHeight = 0.0

        val box = player.boundingBox.inflate(precision)

        while (flyHeight < player.y) {
            val nextBox = box.move(0.0, -flyHeight, 0.0)

            if (!client.level!!.noCollision(nextBox)) {
                if (step < precision) break
                flyHeight -= step
                step /= 2.0
            } else {
                flyHeight += step
            }
        }

        if (flyHeight > 300.0) return

        val minY = player.y - flyHeight
        if (minY <= 0.0) return

        var y = player.y
        while (y > minY) {
            y -= 8.0
            if (y < minY) y = minY
            player.connection.send(ServerboundMovePlayerPacket.Pos(Vec3(player.x, y, player.z), true, true))
        }

        y = minY
        while (y < player.y) {
            y += 8.0
            if (y > player.y) y = player.y
            player.connection.send(ServerboundMovePlayerPacket.Pos(Vec3(player.x, y, player.z), true, true))
        }
    }

    private fun tickNoRules(client: Minecraft, player: LocalPlayer) {
        disableVanillaFlight(player)

        val flying = norulesPhaseTicks < norulesFlyTicks.value
        val moving = player.input.moveVector.x != 0f || player.input.moveVector.y != 0f

        if (flying) {
            if (moving) setMovementSpeed(player, norulesSpeed.value.toDouble())
            val motion = player.deltaMovement
            player.setDeltaMovement(motion.x, 0.0, motion.z)
        } else {
            player.setDeltaMovement(0.0, 0.0, 0.0)
        }

        val drift = if (player.tickCount % 2 == 0) -0.001 else 0.001
        player.setPos(player.x, player.y + drift, player.z)

        norulesPhaseTicks++
        if (norulesPhaseTicks >= norulesFlyTicks.value + norulesRestTicks.value) {
            norulesPhaseTicks = 0
        }
    }

    private fun setMovementSpeed(player: LocalPlayer, speed: Double) {
        var yaw = player.yRot
        var forward = player.input.moveVector.y
        var strafe = player.input.moveVector.x

        if (forward == 0f && strafe == 0f) {
            return
        }

        if (forward != 0f) {
            if (strafe > 0f) {
                yaw += (if (forward > 0f) -45 else 45).toFloat()
            } else if (strafe < 0f) {
                yaw += (if (forward > 0f) 45 else -45).toFloat()
            }
            strafe = 0f
            if (forward > 0f) {
                forward = 1f
            } else if (forward < 0f) {
                forward = -1f
            }
        }

        val rad = Math.toRadians((yaw + 90f).toDouble())
        val dx = forward * speed * cos(rad) + strafe * speed * sin(rad)
        val dz = forward * speed * sin(rad) - strafe * speed * cos(rad)

        player.setDeltaMovement(dx, player.deltaMovement.y, dz)
    }

    override fun onDisabled() {
        val player = Minecraft.getInstance().player ?: return

        if (mode.value == Mode.NCP) {
            player.setDeltaMovement(0.0, player.deltaMovement.y, 0.0)
        }
        ncpCurrentSpeed = NCP_BASE_SPEED

        if (!player.isCreative && !player.isSpectator) {
            player.abilities.mayfly = false
            player.abilities.flying = false
        }
    }

    override fun hudInfo(): String = mode.value.name.lowercase().replace("_", " ")

    private const val NCP_BASE_SPEED = 0.28
    private const val NCP_VERTICAL_OFFSET = 1.0E-9
}
