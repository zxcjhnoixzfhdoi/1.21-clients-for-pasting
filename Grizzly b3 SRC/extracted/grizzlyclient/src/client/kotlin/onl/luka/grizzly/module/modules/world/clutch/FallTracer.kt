package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.floor

// One tick of the predicted fall
internal data class TracePoint(
    val tick: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val eyeY: Double,
    val motionY: Double,
    val yaw: Float,
    val pitch: Float,
    val bounds: AABB,
    val onGround: Boolean,
) {
    fun eyePosition(): Vec3 = Vec3(x, eyeY, z)

    fun intersectsUnitBlock(block: BlockPos, horizontalMargin: Double = 0.0): Boolean =
        bounds.maxX + horizontalMargin > block.x && bounds.minX - horizontalMargin < block.x + 1.0 &&
            bounds.maxY > block.y && bounds.minY < block.y + 1.0 &&
            bounds.maxZ + horizontalMargin > block.z && bounds.minZ - horizontalMargin < block.z + 1.0

    fun horizontallyIntersectsUnitBlock(block: BlockPos, horizontalMargin: Double): Boolean =
        bounds.maxX + horizontalMargin > block.x && bounds.minX - horizontalMargin < block.x + 1.0 &&
            bounds.maxZ + horizontalMargin > block.z && bounds.minZ - horizontalMargin < block.z + 1.0
}

internal data class FallTrace(
    val points: List<TracePoint>,
    // Block the player would come to rest on, or null if they fall out of the world
    val landingBlock: BlockPos?,
) {
    val eyePositions: List<Vec3> get() = points.map { it.eyePosition() }
}

// Traces where the player ends up, running the simulation forward with the inputs they hold.
internal object FallTracer {

    fun predict(
        player: LocalPlayer,
        level: Level,
        maxTicks: Int,
        forward: Boolean = false,
        backward: Boolean = false,
        left: Boolean = false,
        right: Boolean = false,
        jump: Boolean = false,
        movementYaw: Float = player.yRot,
    ): FallTrace {
        val simulation = MotionSim(level, player, movementYaw)
        simulation.setInput(forward, backward, left, right, jump)

        val points = mutableListOf(capture(simulation, 0))
        var landingBlock: BlockPos? = null
        var groundedTicks = 0

        for (tick in 1..maxTicks) {
            simulation.tick()
            points += capture(simulation, tick)

            if (simulation.onGround) {
                if (landingBlock == null) {
                    landingBlock = BlockPos(
                        floor(simulation.x).toInt(),
                        floor(simulation.y - 0.5).toInt(),
                        floor(simulation.z).toInt(),
                    )
                }
                if (++groundedTicks >= 2) break
            } else {
                groundedTicks = 0
                landingBlock = null
            }
            if (simulation.y < level.minY) return FallTrace(points, null)
        }
        return FallTrace(points, landingBlock)
    }

    // Whether the player would walk back off the ground, so a landing does not end a clutch early.
    fun wouldStepOff(
        player: LocalPlayer,
        level: Level,
        forward: Boolean,
        backward: Boolean,
        left: Boolean,
        right: Boolean,
        jump: Boolean,
        movementYaw: Float = player.yRot,
        maxTicks: Int = 10,
    ): Boolean {
        val simulation = MotionSim(level, player, movementYaw)
        simulation.setInput(forward, backward, left, right, jump)
        var wasOnGround = simulation.onGround

        repeat(maxTicks) {
            simulation.tick()
            if (simulation.onGround) {
                // Come to a stop on solid ground: the clutch is finished.
                if (abs(simulation.motionX) < RESTING_MOTION && abs(simulation.motionZ) < RESTING_MOTION) {
                    return false
                }
            } else if (wasOnGround) {
                return true
            }
            wasOnGround = simulation.onGround
        }
        return false
    }

    private const val RESTING_MOTION = 0.005

    private fun capture(simulation: MotionSim, tick: Int) = TracePoint(
        tick = tick,
        x = simulation.x,
        y = simulation.y,
        z = simulation.z,
        eyeY = simulation.eyePosition().y,
        motionY = simulation.motionY,
        yaw = simulation.yaw,
        pitch = simulation.pitch,
        bounds = simulation.boundingBox(),
        onGround = simulation.onGround,
    )

    // Linear interpolation between two points at a given height, for layer-crossing tests
    fun sampleAtY(start: TracePoint, end: TracePoint, targetY: Double): TracePoint {
        val verticalDelta = end.y - start.y
        val progress = if (abs(verticalDelta) < 1.0E-9) 0.0 else {
            ((targetY - start.y) / verticalDelta).coerceIn(0.0, 1.0)
        }
        fun lerp(from: Double, to: Double) = from + (to - from) * progress
        return TracePoint(
            tick = end.tick,
            x = lerp(start.x, end.x),
            y = targetY,
            z = lerp(start.z, end.z),
            eyeY = lerp(start.eyeY, end.eyeY),
            motionY = lerp(start.motionY, end.motionY),
            yaw = start.yaw,
            pitch = start.pitch,
            bounds = AABB(
                lerp(start.bounds.minX, end.bounds.minX),
                lerp(start.bounds.minY, end.bounds.minY),
                lerp(start.bounds.minZ, end.bounds.minZ),
                lerp(start.bounds.maxX, end.bounds.maxX),
                lerp(start.bounds.maxY, end.bounds.maxY),
                lerp(start.bounds.maxZ, end.bounds.maxZ),
            ),
            onGround = false,
        )
    }
}
