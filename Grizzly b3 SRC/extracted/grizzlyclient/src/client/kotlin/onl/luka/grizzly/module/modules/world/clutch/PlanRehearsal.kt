package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

internal data class RehearsalResult(val landed: Boolean, val failureReason: String?) {
    companion object {
        val SUCCESS = RehearsalResult(true, null)
        fun failed(reason: String) = RehearsalResult(false, reason)
    }
}

internal data class MotionInput(
    val forward: Boolean = false,
    val backward: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val jump: Boolean = false,
)

// Plays a planned clutch forward before committing.
internal object PlanRehearsal {

    // Grounded ticks before the landing is treated as settled
    private const val SETTLE_TICKS = 3

    fun validate(
        level: Level,
        player: LocalPlayer,
        targets: List<BlockPlacement>,
        destination: BlockPos,
        reach: Double,
        maxTicks: Int,
        input: MotionInput,
        movementYaw: Float,
    ): RehearsalResult {
        if (targets.isEmpty()) return RehearsalResult.failed("Nothing to place")

        val simulation = MotionSim(level, player, movementYaw)
        simulation.setInput(input.forward, input.backward, input.left, input.right, input.jump)

        val pending = ArrayDeque(targets)
        var landed = false
        var groundedTicks = 0

        for (tick in 0..maxTicks) {
            placeIfReachable(level, simulation, pending, reach)

            if (simulation.onGround) {
                if (floor(simulation.y).toInt() >= destination.y + 1) landed = true
                if (++groundedTicks >= SETTLE_TICKS) break
            } else {
                groundedTicks = 0
            }

            simulation.tick()

            if (simulation.y < level.minY) {
                return RehearsalResult.failed("Player would be below the world")
            }
            if (simulation.y < destination.y - 1) {
                return RehearsalResult.failed(
                    if (landed) "Player would fall off after landing" else "Player would be too low to land",
                )
            }
        }

        return when {
            landed -> RehearsalResult.SUCCESS
            pending.isNotEmpty() -> RehearsalResult.failed("No time to place ${pending.size} more blocks")
            else -> RehearsalResult.failed("Player would not land on the clutch")
        }
    }

    // Places the next block if the simulated player could reach and see it this tick
    private fun placeIfReachable(
        level: Level,
        simulation: MotionSim,
        pending: ArrayDeque<BlockPlacement>,
        reach: Double,
    ) {
        val target = pending.firstOrNull() ?: return
        val placedBounds = PlacementGeometry.blockBounds(level, target.fills)
        // Vanilla refuses a placement that would intersect the player.
        if (simulation.boundingBox().intersects(placedBounds)) return

        val eye = simulation.eyePosition()
        if (!PlacementGeometry.faceIsVisible(eye, level, target.against, target.facing)) return

        val facing = target.facing
        val faceCenter = if (facing == null) {
            Vec3(target.against.x + 0.5, target.against.y + 0.5, target.against.z + 0.5)
        } else {
            Vec3(
                target.against.x + 0.5 + facing.stepX * 0.5,
                target.against.y + 0.5 + facing.stepY * 0.5,
                target.against.z + 0.5 + facing.stepZ * 0.5,
            )
        }
        if (eye.distanceToSqr(faceCenter) > reach * reach) return

        simulation.addPendingBlock(target.fills)
        pending.removeFirst()
    }
}
