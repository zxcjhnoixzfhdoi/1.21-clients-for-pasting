package onl.luka.grizzly.module.modules.movement

import net.minecraft.world.entity.player.Input
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.RotationManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MoveFix : Module(
    name = "Move Fix",
    description = "Keeps movement aligned with your camera during silent rotations",
    category = Category.MOVEMENT,
) {
    @JvmStatic
    fun correctInput(input: Input): Input {
        if (!shouldCorrect()) return input

        var forward = 0f
        var strafe = 0f
        if (input.forward()) forward += 1f
        if (input.backward()) forward -= 1f
        if (input.left()) strafe += 1f
        if (input.right()) strafe -= 1f
        if (forward == 0f && strafe == 0f) return input

        val length = sqrt(forward * forward + strafe * strafe)
        val cameraYaw = Math.toRadians(RotationManager.getClientYaw().toDouble())
        val worldX = strafe / length * cos(cameraYaw) - forward / length * sin(cameraYaw)
        val worldZ = forward / length * cos(cameraYaw) + strafe / length * sin(cameraYaw)
        val corrected = closestInput(worldX, worldZ, RotationManager.getCurrentYaw())

        return Input(
            corrected.forward > 0f,
            corrected.forward < 0f,
            corrected.strafe > 0f,
            corrected.strafe < 0f,
            input.jump(),
            input.shift(),
            input.sprint(),
        )
    }

    @JvmStatic
    fun preparePhysicsYaw() {
        if (shouldCorrect()) {
            RotationManager.physicsYawOverride = RotationManager.getCurrentYaw()
        }
    }

    private fun shouldCorrect(): Boolean =
        isEnabled() &&
            RotationManager.isActive() &&
            RotationManager.movementMode == RotationManager.MovementMode.CLIENT &&
            !RotationManager.rotationOwnerHandlesMovementCorrection()

    private fun closestInput(worldX: Double, worldZ: Double, yaw: Float): CorrectedInput {
        val diagonal = (1.0 / sqrt(2.0)).toFloat()
        val candidates = arrayOf(
            CorrectedInput(1f, 0f),
            CorrectedInput(-1f, 0f),
            CorrectedInput(0f, 1f),
            CorrectedInput(0f, -1f),
            CorrectedInput(diagonal, diagonal),
            CorrectedInput(-diagonal, diagonal),
            CorrectedInput(diagonal, -diagonal),
            CorrectedInput(-diagonal, -diagonal),
        )
        val serverYaw = Math.toRadians(yaw.toDouble())
        val serverSin = sin(serverYaw)
        val serverCos = cos(serverYaw)

        return candidates.maxBy { candidate ->
            val candidateX = candidate.strafe * serverCos - candidate.forward * serverSin
            val candidateZ = candidate.forward * serverCos + candidate.strafe * serverSin
            worldX * candidateX + worldZ * candidateZ
        }
    }

    private data class CorrectedInput(val strafe: Float, val forward: Float)
}
