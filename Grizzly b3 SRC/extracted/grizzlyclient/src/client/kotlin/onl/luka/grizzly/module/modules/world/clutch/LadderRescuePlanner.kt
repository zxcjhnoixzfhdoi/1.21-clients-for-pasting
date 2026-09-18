package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

internal class LadderRescue(
    val targets: List<BlockPlacement>,
    val ladderBlock: BlockPos,
    val ladderFacing: Direction,
    val catchX: Double,
    val catchZ: Double,
    val score: Double,
) {
    val pendingPlacementCount: Int get() = targets.size
}

// Ladder rescue for a fall that blocks alone cannot catch.
internal class LadderRescuePlanner(
    private val level: Level,
    private val player: LocalPlayer,
    private val reach: Double,
    private val placedBlocks: Set<BlockPos>,
) {
    var failureReason: String? = null
        private set

    fun plan(prediction: FallTrace): LadderRescue? {
        failureReason = null
        val trajectory = prediction.points
        if (trajectory.size < 2) {
            failureReason = "Not enough time for a ladder rescue"
            return null
        }
        val samples = catchWindows(trajectory)
        if (samples.isEmpty()) {
            failureReason = "No ladder catch window in the fall trajectory"
            return null
        }

        val plans = mutableListOf<LadderRescue>()
        for (sample in samples) {
            forEachCell(sample) { ladderBlock, facing, catchX, catchZ, movementError ->
                buildPlan(sample, ladderBlock, facing, catchX, catchZ, movementError, trajectory)
                    ?.let(plans::add)
            }
        }
        if (plans.isEmpty()) {
            failureReason = "Could not place a ladder in time"
            return null
        }
        return plans.minByOrNull { it.score }
    }

    private data class CatchWindow(val point: TracePoint, val latestPlacementTick: Int, val ladderY: Int)

    // Layers the player's body crosses side-on while still falling
    private fun catchWindows(trajectory: List<TracePoint>): List<CatchWindow> {
        val samples = LinkedHashMap<Long, CatchWindow>()
        val minimumLadderY = trajectory.last().let { if (it.onGround) floor(it.y + 1.0E-4).toInt() else level.minY }

        for (index in 1 until trajectory.size) {
            val previous = trajectory[index - 1]
            val current = trajectory[index]
            if (current.y >= previous.y) continue
            if (previous.motionY >= 0.0 && current.motionY >= 0.0) continue

            val highest = floor(previous.y - 1.0 + LADDER_SIDE_ENTRY_DEPTH + 1.0E-4).toInt()
            val lowest = maxOf(
                minimumLadderY,
                floor(current.y - 1.0 + LADDER_SIDE_ENTRY_DEPTH + 1.0E-4).toInt() + 1,
            )
            for (ladderY in highest downTo lowest) {
                val sideEntryY = ladderY + 1.0 - LADDER_SIDE_ENTRY_DEPTH
                if (previous.y < sideEntryY || current.y >= sideEntryY) continue
                val swept = FallTracer.sampleAtY(previous, current, sideEntryY)
                val key = (ladderY.toLong() shl 32) or (previous.tick.toLong() and 0xFFFFFFFFL)
                samples[key] = CatchWindow(swept, previous.tick, ladderY)
            }
        }
        return samples.values.toList()
    }

    private inline fun forEachCell(
        sample: CatchWindow,
        consumer: (BlockPos, Direction, Double, Double, Double) -> Unit,
    ) {
        val baseX = floor(sample.point.x).toInt()
        val baseZ = floor(sample.point.z).toInt()
        for (offsetX in -1..1) {
            for (offsetZ in -1..1) {
                val ladderBlock = BlockPos(baseX + offsetX, sample.ladderY, baseZ + offsetZ)
                val catchX = ladderBlock.x + 0.5
                val catchZ = ladderBlock.z + 0.5
                val movementError = hypot(catchX - sample.point.x, catchZ - sample.point.z)
                if (movementError > CATCH_CANDIDATE_RADIUS) continue
                for (facing in HORIZONTAL_FACINGS) {
                    consumer(ladderBlock, facing, catchX, catchZ, movementError)
                }
            }
        }
    }

    private fun buildPlan(
        sample: CatchWindow,
        ladderBlock: BlockPos,
        facing: Direction,
        catchX: Double,
        catchZ: Double,
        movementError: Double,
        trajectory: List<TracePoint>,
    ): LadderRescue? {
        if (!level.getBlockState(ladderBlock).canBeReplaced()) return null

        val against = ladderBlock.relative(facing.opposite)
        val targets = mutableListOf<BlockPlacement>()
        if (isSupport(against)) {
            targets += BlockPlacement(against, facing).also { it.isLadder = true }
        } else {
            if (!level.getBlockState(against).canBeReplaced()) return null
            val extension = extensionTo(against, trajectory, sample.latestPlacementTick)
                ?: return null
            targets += extension
            targets += BlockPlacement(against, facing).also { it.isLadder = true }
        }

        var tickCursor = 0
        var rotationTotal = 0.0
        var ladderPlacementTick = -1
        for (target in targets) {
            val opportunity = placementWindow(
                target,
                trajectory,
                tickCursor,
                sample.latestPlacementTick,
                placingSolidBlock = !target.isLadder,
            ) ?: return null
            tickCursor = opportunity.first + 1
            rotationTotal += opportunity.second
            ladderPlacementTick = opportunity.first
        }

        val catchTick = catchTick(trajectory, ladderPlacementTick, ladderBlock, against) ?: return null
        val slack = catchTick - ladderPlacementTick
        val centerError = trajectory.firstOrNull { it.tick == catchTick }
            ?.let { hypot(it.x - catchX, it.z - catchZ) } ?: 0.0
        val score = movementError * 1000.0 +
            centerError * 250.0 +
            rotationTotal * 2.0 +
            catchTick * 4.0 -
            slack * 30.0 +
            targets.size * 5.0
        return LadderRescue(targets, ladderBlock, facing, catchX, catchZ, score)
    }

    // Short chain of blocks out to somewhere the ladder can hang off
    private fun extensionTo(
        anchorBlock: BlockPos,
        trajectory: List<TracePoint>,
        latestPlacementTick: Int,
    ): List<BlockPlacement>? {
        val extensionDepth = minOf(maxOf(0, latestPlacementTick - 2), MAX_EXTENSION_BLOCKS)
        if (extensionDepth == 0) return null

        val strategy = object : SearchRules {
            override val depthLimit: Int = extensionDepth
            override fun canFinishAt(pos: BlockPos): Boolean = level.getBlockState(pos).canBeReplaced()
            override fun canStepThrough(pos: BlockPos): Boolean =
                PlacementGeometry.spaceIsClear(level, player, pos) && pos !in placedBlocks
        }

        var best: List<BlockPlacement>? = null
        var bestScore = Double.MAX_VALUE
        for (start in reachableSupports(trajectory)) {
            val path = BridgePlanner(strategy).findPath(start, anchorBlock) ?: continue
            if (path.isEmpty() || path.size > extensionDepth) continue
            if (path.last().fills != anchorBlock) continue
            val score = path.size * 100.0 + abs(start.y - anchorBlock.y) * 3.0
            if (score < bestScore) {
                bestScore = score
                best = path
            }
        }
        return best
    }

    private fun reachableSupports(trajectory: List<TracePoint>): List<BlockPos> {
        val start = trajectory.first()
        val playerX = floor(start.x).toInt()
        val playerY = floor(start.y).toInt()
        val playerZ = floor(start.z).toInt()
        val candidates = mutableListOf<BlockPos>()
        for (offsetY in -1..3) {
            for (offsetX in -4..4) {
                for (offsetZ in -4..4) {
                    val pos = BlockPos(playerX + offsetX, playerY + offsetY, playerZ + offsetZ)
                    if (!isSupport(pos) || pos in placedBlocks) continue
                    candidates += pos
                }
            }
        }
        return candidates
            .sortedBy { hypot(it.x + 0.5 - start.x, it.z + 0.5 - start.z) }
            .take(MAX_REACHABLE_SUPPORTS)
    }

    // Earliest tick the target can be both seen and aimed at, with its rotation cost
    private fun placementWindow(
        target: BlockPlacement,
        trajectory: List<TracePoint>,
        earliestTick: Int,
        latestTick: Int,
        placingSolidBlock: Boolean,
    ): Pair<Int, Double>? {
        if (latestTick < earliestTick) return null
        for (point in trajectory) {
            if (point.tick < earliestTick) continue
            if (point.tick > latestTick) break
            if (placingSolidBlock && point.intersectsUnitBlock(target.fills)) continue
            if (placingSolidBlock &&
                !PlacementGeometry.spaceIsClear(level, player, target.fills)
            ) {
                continue
            }
            val eye = point.eyePosition()
            if (!PlacementGeometry.faceIsVisible(eye, level, target.against, target.facing)) {
                continue
            }
            val hit = PlacementGeometry.aimPointWithinReach(
                player, level, eye, target, point.yaw, point.pitch, reach,
            ) ?: continue
            val rotationDistance = PlacementGeometry
                .angleBetween(eye, hit, point.yaw, point.pitch)
                .toDouble()
            return point.tick to rotationDistance
        }
        return null
    }

    // First tick after the ladder goes up where the player is inside its cell and clear of the support
    private fun catchTick(
        trajectory: List<TracePoint>,
        placementTick: Int,
        ladderBlock: BlockPos,
        against: BlockPos,
    ): Int? {
        for (point in trajectory) {
            if (point.tick < placementTick) continue
            if (point.onGround) return null
            if (point.intersectsUnitBlock(against, SUPPORT_CLEARANCE_MARGIN)) return null
            val insideCell = point.x >= ladderBlock.x + CATCH_CELL_INSET &&
                point.x <= ladderBlock.x + 1.0 - CATCH_CELL_INSET &&
                point.z >= ladderBlock.z + CATCH_CELL_INSET &&
                point.z <= ladderBlock.z + 1.0 - CATCH_CELL_INSET
            val verticallyOverlaps = point.bounds.maxY > ladderBlock.y && point.bounds.minY < ladderBlock.y + 1.0
            if (insideCell && verticallyOverlaps &&
                !point.horizontallyIntersectsUnitBlock(against, SUPPORT_CLEARANCE_MARGIN)
            ) {
                return point.tick
            }
            if (point.y < ladderBlock.y - 0.05) return null
        }
        return null
    }

    private fun isSupport(pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (state.canBeReplaced()) return false
        if (PlacementGeometry.isInteractive(state)) return false
        return state.isCollisionShapeFullBlock(level, pos)
    }

    private companion object {
        const val LADDER_SIDE_ENTRY_DEPTH = 0.28
        const val CATCH_CELL_INSET = 0.02
        const val SUPPORT_CLEARANCE_MARGIN = 0.04
        const val CATCH_CANDIDATE_RADIUS = 0.67
        const val MAX_EXTENSION_BLOCKS = 6
        const val MAX_REACHABLE_SUPPORTS = 12
        val HORIZONTAL_FACINGS: Array<Direction> = Array(4) { Direction.from2DDataValue(it) }
    }
}
