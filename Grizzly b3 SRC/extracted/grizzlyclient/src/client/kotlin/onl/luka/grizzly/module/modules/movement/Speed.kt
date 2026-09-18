package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.effect.MobEffects
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object Speed : Module("Speed", "Increases your movement speed", Category.MOVEMENT) {

    enum class Mode {
        ON_GROUND, BHOP, LOWHOP, NCP
    }

    @JvmField val mode = enum("mode", Mode.ON_GROUND)
    private val speedMult = float("multiplier", 1.2f, 1.0f, 3.0f).also {
        it.visibleWhen = { mode.value != Mode.NCP }
    }

    private val ncpPullDown = boolean("pull down", true).also(::ncpOnly)
    private val ncpPullDownMultiplier = float("pull down multiplier", 1.0f, 0.01f, 10.0f).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpPullDown.value }
    }
    private val ncpPullDownTick = int("pull down tick", 5, 1, 9).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpPullDown.value }
    }
    private val ncpHurtPullDown = boolean("hurt pull down", true).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpPullDown.value }
    }
    private val ncpBoost = boolean("boost", true).also(::ncpOnly)
    private val ncpBoostMultiplier = float("boost multiplier", 1.0f, 0.01f, 10.0f).also {
        it.visibleWhen = { mode.value == Mode.NCP && ncpBoost.value }
    }
    private val ncpTimer = boolean("timer", true).also(::ncpOnly)
    private val ncpDamageBoost = boolean("damage boost", true).also(::ncpOnly)
    private val ncpLowHop = boolean("low hop", true).also(::ncpOnly)
    private val ncpAirStrafe = boolean("air strafe", true).also(::ncpOnly)

    private var ncpTicksInAir = 0

    private fun <T : onl.luka.grizzly.config.entry.ConfigEntry<*>> ncpOnly(entry: T) {
        entry.visibleWhen = { mode.value == Mode.NCP }
    }

    override fun onEnabled() {
        ncpTicksInAir = 0
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val currentMode = mode.value
        val isMoving = player.input.moveVector.x != 0f || player.input.moveVector.y != 0f

        if (currentMode == Mode.NCP) {
            tickNcp(player, isMoving)
            return
        }

        ncpTicksInAir = 0
        if (!isMoving) {
            player.setDeltaMovement(0.0, player.deltaMovement.y, 0.0)
            return
        }

        val baseSpeed = 0.22 * speedMult.value

        when (currentMode) {
            Mode.ON_GROUND -> {
                if (player.onGround()) {
                    setMovementSpeed(player, baseSpeed)
                }
            }
            Mode.BHOP -> {
                if (player.onGround()) {
                    player.jumpFromGround()
                    setMovementSpeed(player, baseSpeed * 1.5)
                } else {
                    setMovementSpeed(player, baseSpeed)
                }
            }
            Mode.LOWHOP -> {
                if (player.onGround()) {
                    val velocity = player.deltaMovement
                    player.setDeltaMovement(velocity.x, 0.3, velocity.z)
                    setMovementSpeed(player, baseSpeed * 1.5)
                } else {
                    setMovementSpeed(player, baseSpeed)
                    if (player.deltaMovement.y < 0.0) {
                        val velocity = player.deltaMovement
                        player.setDeltaMovement(velocity.x, velocity.y * 1.2, velocity.z)
                    }
                }
            }
            Mode.NCP -> Unit
        }
    }

    private fun tickNcp(player: LocalPlayer, isMoving: Boolean) {
        if (player.onGround()) {
            ncpTicksInAir = 0
        } else {
            ncpTicksInAir++
            if (ncpPullDown.value && ncpTicksInAir == ncpPullDownTick.value) {
                setMovementSpeed(player, horizontalSpeed(player))
                val velocity = player.deltaMovement
                player.setDeltaMovement(
                    velocity.x,
                    velocity.y - NCP_PULL_DOWN * ncpPullDownMultiplier.value,
                    velocity.z,
                )
            }
            if (ncpPullDown.value && ncpHurtPullDown.value &&
                player.hurtTime >= 5 && player.deltaMovement.y >= 0.0
            ) {
                val velocity = player.deltaMovement
                player.setDeltaMovement(velocity.x, velocity.y - 0.1, velocity.z)
            }
        }

        if (isMoving && player.onGround()) {
            player.jumpFromGround()
            if (ncpLowHop.value) {
                val velocity = player.deltaMovement
                player.setDeltaMovement(velocity.x, 0.4, velocity.z)
            }
        }

        if (ncpBoost.value && isMoving) {
            val velocity = player.deltaMovement
            val boost = 1.0 + NCP_BOOST * ncpBoostMultiplier.value
            player.setDeltaMovement(velocity.x * boost, velocity.y, velocity.z * boost)
        }

        val speedAmplifier = player.getEffect(MobEffects.SPEED)?.amplifier ?: 0
        if (isMoving) {
            if (player.onGround()) {
                val minimum = NCP_GROUND_SPEED + NCP_POTION_SPEED * speedAmplifier
                setMovementSpeed(player, horizontalSpeed(player).coerceAtLeast(minimum))
            } else if (ncpAirStrafe.value) {
                val minimum = NCP_AIR_SPEED + NCP_POTION_SPEED * speedAmplifier
                setMovementSpeed(player, horizontalSpeed(player).coerceAtLeast(minimum), strength = 0.7)
            }
        }

        if (ncpDamageBoost.value && player.hurtTime >= 1) {
            setMovementSpeed(player, horizontalSpeed(player).coerceAtLeast(0.5))
        }
    }

    private fun horizontalSpeed(player: LocalPlayer): Double =
        hypot(player.deltaMovement.x, player.deltaMovement.z)

    private fun setMovementSpeed(player: LocalPlayer, speed: Double, strength: Double = 1.0) {
        var yaw = player.yRot
        var forward = player.input.moveVector.y
        var strafe = player.input.moveVector.x

        if (forward == 0f && strafe == 0f) {
            if (strength >= 1.0) player.setDeltaMovement(0.0, player.deltaMovement.y, 0.0)
            return
        }

        if (forward != 0f) {
            if (strafe > 0f) {
                yaw += (if (forward > 0f) -45 else 45).toFloat()
            } else if (strafe < 0f) {
                yaw += (if (forward > 0f) 45 else -45).toFloat()
            }
            strafe = 0f
            forward = if (forward > 0f) 1f else -1f
        }

        val radians = Math.toRadians((yaw + 90f).toDouble())
        val desiredX = forward * speed * cos(radians) + strafe * speed * sin(radians)
        val desiredZ = forward * speed * sin(radians) - strafe * speed * cos(radians)
        val retained = 1.0 - strength
        val velocity = player.deltaMovement

        player.setDeltaMovement(
            velocity.x * retained + desiredX * strength,
            velocity.y,
            velocity.z * retained + desiredZ * strength,
        )
    }

    @JvmStatic
    fun timerSpeedMultiplier(): Float =
        if (isEnabled() && mode.value == Mode.NCP && ncpTimer.value) 1.08f else 1.0f

    override fun onDisabled() {
        ncpTicksInAir = 0
        Minecraft.getInstance().player?.let { player ->
            player.setDeltaMovement(0.0, player.deltaMovement.y, 0.0)
        }
    }

    override fun hudInfo(): String = mode.value.name.replace("_", " ")

    private const val NCP_POTION_SPEED = 0.199999999
    private const val NCP_GROUND_SPEED = 0.281
    private const val NCP_AIR_SPEED = 0.2
    private const val NCP_BOOST = 0.00718
    private const val NCP_PULL_DOWN = 0.1523351824467155
}
