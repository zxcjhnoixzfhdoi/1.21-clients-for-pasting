package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque

// Search rules for a bridge: valid endpoints, steppable blocks, and how a route is costed.
internal class BridgeRules(
    private val level: Level,
    private val player: Player,
    // Eye positions the player is predicted to pass through, oldest first
    private val candidatePositions: List<Vec3>,
    private val placedBlocks: Set<BlockPos>,
    override val depthLimit: Int,
) : SearchRules {

    override fun canFinishAt(pos: BlockPos): Boolean = level.getBlockState(pos).canBeReplaced()

    override fun canStepThrough(pos: BlockPos): Boolean =
        PlacementGeometry.spaceIsClear(level, player, pos) && pos !in placedBlocks

    override fun cost(path: List<BlockPlacement>, depth: Int): Int = cost(path)

    // Penalises placements that will be out of view by the time the player reaches them.
    override fun cost(path: List<BlockPlacement>): Int {
        if (path.isEmpty()) return Int.MAX_VALUE

        val remaining = ArrayDeque<BlockPlacement>()
        path.forEach(remaining::push)

        var noTargetReached = true
        var score = path.size * 100.0
        for (candidatePosition in candidatePositions) {
            val target = remaining.peek() ?: break
            if (PlacementGeometry.faceIsVisible(
                    candidatePosition,
                    level,
                    target.against,
                    target.facing,
                )
            ) {
                remaining.pop()
                noTargetReached = false
            } else {
                score += 100_000.0
            }
            val placed = target.fills
            val targetPosition = Vec3(placed.x + 0.5, candidatePosition.y, placed.z + 0.5)
            score += candidatePosition.distanceTo(targetPosition) * 50_000.0
        }
        score += remaining.size * 100_000.0
        if (noTargetReached) return Int.MAX_VALUE
        return score.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
    }
}
