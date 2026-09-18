package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

// Directed placement search: steps toward a destination one face at a time, so the block
// being clicked is always one that already exists.
internal class BridgePlanner(private val strategy: SearchRules) {

    private val plannedFrom = HashMap<BlockPos, MutableList<BlockPlacement>?>()

    var visitedNodes: Int = 0
        private set

    fun findPath(start: BlockPos, destination: BlockPos): MutableList<BlockPlacement>? {
        plannedFrom.clear()
        visitedNodes = 0
        return planFrom(start, destination, 0, mutableListOf())
    }

    private fun planFrom(
        current: BlockPos,
        destination: BlockPos,
        depth: Int,
        currentPath: MutableList<BlockPlacement>,
    ): MutableList<BlockPlacement>? {
        visitedNodes++
        if (plannedFrom.containsKey(current)) return plannedFrom[current]
        if (depth > strategy.depthLimit) return null

        val deltaX = destination.x - current.x
        val deltaY = destination.y - current.y
        val deltaZ = destination.z - current.z
        val directionOrder = intArrayOf(
            if (deltaX > 0) 5 else if (deltaX < 0) 4 else -1,
            if (deltaY > 0) 1 else if (deltaY < 0) 0 else -1,
            if (deltaZ > 0) 3 else if (deltaZ < 0) 2 else -1,
        )

        val candidates = mutableListOf<BlockPlacement>()
        for (directionIndex in directionOrder) {
            if (directionIndex == -1) continue
            val direction = FACINGS[directionIndex]
            val adjacent = current.relative(direction)
            if (!strategy.canFinishAt(adjacent)) continue
            val target = BlockPlacement(current, direction)
            target.depth = depth
            candidates += target
        }
        candidates.sortBy { candidate -> stepCost(currentPath, depth, candidate) }

        var bestPath: MutableList<BlockPlacement>? = null
        var bestScore = Int.MAX_VALUE
        for (candidate in candidates) {
            val nextBlock = candidate.fills
            var candidatePath = ArrayList(currentPath).also { it += candidate }
            if (nextBlock == destination) {
                bestPath = candidatePath
                break
            }
            val recursivePath = planFrom(nextBlock, destination, depth + 1, candidatePath)
            if (recursivePath != null && recursivePath.isNotEmpty()) {
                candidatePath = ArrayList(recursivePath)
            }
            if (candidatePath.isEmpty()) break

            val score = strategy.cost(candidatePath, depth)
            if (bestPath == null || score < bestScore) {
                bestPath = candidatePath
                bestScore = score
            }
        }
        plannedFrom[current] = bestPath
        return bestPath
    }

    private fun stepCost(
        currentPath: MutableList<BlockPlacement>,
        depth: Int,
        candidate: BlockPlacement,
    ): Int = strategy.cost(ArrayList(currentPath).also { it += candidate }, depth)

    private companion object {
        val FACINGS: Array<Direction> = Direction.values()
    }
}
