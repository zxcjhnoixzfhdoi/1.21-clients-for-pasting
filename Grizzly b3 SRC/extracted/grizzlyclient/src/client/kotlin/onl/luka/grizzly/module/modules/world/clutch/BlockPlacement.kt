package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

// One block placement: the block we click on, and which of its faces
internal class BlockPlacement(
    val against: BlockPos,
    val facing: Direction?,
    val offsetFromFace: Boolean = true,
) {
    var depth: Int = 0
    var hitPoint: Vec3? = null

    // Set when this placement is the ladder itself rather than a block leading to it
    var isLadder: Boolean = false

    private var cachedPlacedBlock: BlockPos? = null

    val fills: BlockPos
        get() = cachedPlacedBlock ?: run {
            val resolved = if (offsetFromFace && facing != null) against.relative(facing) else against
            cachedPlacedBlock = resolved
            resolved
        }
}

// Decides which blocks a placement search may step through, and how good a result is
internal interface SearchRules {
    val depthLimit: Int

    fun canStepThrough(pos: BlockPos): Boolean = true

    fun canFinishAt(pos: BlockPos): Boolean

    fun cost(path: List<BlockPlacement>): Int = path.size

    fun cost(path: List<BlockPlacement>, depth: Int): Int = cost(path)
}
