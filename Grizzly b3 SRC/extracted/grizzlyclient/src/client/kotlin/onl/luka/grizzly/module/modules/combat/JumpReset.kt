package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.InputUtil
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.round
import kotlin.random.Random

object JumpReset : Module(
    "Jump Reset",
    "Jumps the moment knockback lands so the ground cuts it short",
    Category.COMBAT,
) {

    private val chance = int("chance %", 40, 0, 100)
    private val accuracy = intRange("accuracy %", 40 to 60, 0, 100)
    private val onlyWhenTargeting = boolean("only when targeting", false)
    private val waterCheck = boolean("water check", false)

    private var expectedMotion: Vec3? = null
    private var waitingTicks = 0
    private var shouldJump = false
    private var jumping = false

    init {
        ClientTickEvents.START_CLIENT_TICK.register(::onClientTick)
    }

    @JvmStatic
    fun onVelocity(player: LocalPlayer, motion: Vec3) {
        if (!isEnabled()) return
        // Nothing horizontal to shed, or a downward pull, is not knockback worth resetting.
        if (motion.x == 0.0 && motion.z == 0.0) return
        if (motion.y < 0.0) return
        if (!shouldReset(Minecraft.getInstance(), player)) return

        expectedMotion = motion
        waitingTicks = 0
    }

    private fun onClientTick(client: Minecraft) {
        if (!isEnabled()) return
        val player = client.player ?: run {
            reset(client)
            return
        }

        if (jumping) {
            client.options.keyJump.setDown(InputUtil.isPhysicalKeyDown(client.options.keyJump))
            jumping = false
        }

        awaitKnockback(client, player)

        if (!shouldJump) return
        // A missed roll leaves the jump queued rather than dropping it, so a bad one lands late
        // instead of not at all. That is what makes accuracy read as timing quality.
        if (Random.nextInt(100) < 100 - rolledAccuracy()) return

        client.options.keyJump.setDown(true)
        jumping = true
        shouldJump = false
    }

    private fun awaitKnockback(client: Minecraft, player: LocalPlayer) {
        val expected = expectedMotion ?: return
        if (++waitingTicks > WAIT_TIMEOUT_TICKS) {
            expectedMotion = null
            return
        }
        if (!matches(player.deltaMovement, expected)) return

        expectedMotion = null
        if (!InputUtil.isPhysicalKeyDown(client.options.keyJump)) shouldJump = true
    }

    private fun matches(actual: Vec3, expected: Vec3): Boolean =
        round3(actual.x) == round3(expected.x) &&
            round3(actual.y) == round3(expected.y) &&
            round3(actual.z) == round3(expected.z)

    private fun round3(value: Double): Double = round(value * 1000.0) / 1000.0

    private fun shouldReset(client: Minecraft, player: LocalPlayer): Boolean {
        if (jumping) return false
        if (player.hasEffect(MobEffects.JUMP_BOOST)) return false
        if (waterCheck.value && player.isInWater) return false
        if (onlyWhenTargeting.value && !isFacingOpponent(client, player)) return false
        return chance.value >= 100 || Random.nextInt(100) < chance.value
    }

    private fun rolledAccuracy(): Int {
        val (lo, hi) = accuracy.value
        return if (hi > lo) (lo..hi).random() else lo
    }

    private fun isFacingOpponent(client: Minecraft, player: LocalPlayer): Boolean {
        val level = client.level ?: return false
        var nearest: Player? = null
        var nearestDistance = TARGET_RANGE

        for (entity in level.entitiesForRendering()) {
            if (entity !is Player || entity === player) continue
            val distance = player.distanceTo(entity)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = entity
            }
        }

        val target = nearest ?: return false
        val eye = player.eyePosition
        val toTarget = target.boundingBox.center.subtract(eye)
        val yaw = Math.toDegrees(atan2(-toTarget.x, toTarget.z)).toFloat()
        val pitch = (-Math.toDegrees(atan2(toTarget.y, hypot(toTarget.x, toTarget.z)))).toFloat()

        return abs(Mth.wrapDegrees(yaw - player.yRot)) <= FACING_TOLERANCE &&
            abs(Mth.wrapDegrees(pitch - player.xRot)) <= FACING_TOLERANCE
    }

    override fun onDisabled() {
        reset(Minecraft.getInstance())
    }

    private fun reset(client: Minecraft) {
        expectedMotion = null
        waitingTicks = 0
        shouldJump = false
        if (jumping) {
            client.options?.keyJump?.setDown(InputUtil.isPhysicalKeyDown(client.options.keyJump))
            jumping = false
        }
    }

    override fun hudInfo(): String = "${chance.value}%"

    private const val WAIT_TIMEOUT_TICKS = 10
    private const val TARGET_RANGE = 6.0f
    private const val FACING_TOLERANCE = 60f
}
