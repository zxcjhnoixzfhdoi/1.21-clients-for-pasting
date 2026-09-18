package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

// Placement geometry: where on a face to aim, and whether a face can be used at all.
internal object PlacementGeometry {

    private const val FACE_CONTRACT = 0.002
    private const val EPSILON = 1.0E-4

    fun isSolid(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        return !state.canBeReplaced() && state.isCollisionShapeFullBlock(level, pos)
    }

    fun isReplaceable(level: Level, pos: BlockPos): Boolean = level.getBlockState(pos).canBeReplaced()

    // Outline bounds of the block, in world space
    fun blockBounds(level: Level, pos: BlockPos): AABB {
        val state = level.getBlockState(pos)
        val shape = state.getShape(level, pos)
        return if (shape.isEmpty) {
            AABB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
        } else {
            shape.bounds().move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        }
    }

    // Blocks that swallow a right click instead of letting the placement through.
    fun isInteractive(state: BlockState): Boolean {
        if (state.hasBlockEntity()) return true
        return state.`is`(net.minecraft.tags.BlockTags.DOORS) ||
            state.`is`(net.minecraft.tags.BlockTags.TRAPDOORS) ||
            state.`is`(net.minecraft.tags.BlockTags.FENCE_GATES) ||
            state.`is`(net.minecraft.tags.BlockTags.BUTTONS) ||
            state.`is`(net.minecraft.tags.BlockTags.BEDS) ||
            state.`is`(net.minecraft.tags.BlockTags.ANVIL) ||
            state.`is`(Blocks.LEVER) ||
            state.`is`(Blocks.CRAFTING_TABLE) ||
            state.`is`(Blocks.ENCHANTING_TABLE) ||
            state.`is`(Blocks.CAKE) ||
            state.`is`(Blocks.REPEATER) ||
            state.`is`(Blocks.COMPARATOR) ||
            state.`is`(Blocks.NOTE_BLOCK) ||
            state.`is`(Blocks.DAYLIGHT_DETECTOR)
    }

    // No entity is standing where the block would go
    fun spaceIsClear(level: Level, source: Entity, pos: BlockPos): Boolean {
        val bounds = blockBounds(level, pos)
        return level.getEntities(source, bounds).none { candidate ->
            !candidate.isSpectator && candidate.isPickable && candidate !== source &&
                candidate.boundingBox.intersects(bounds)
        }
    }

    // The four corners of one face of a box, ordered as the original does
    private fun faceCorners(
        facingIndex: Int,
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
    ): Array<Vec3> = when (facingIndex) {
        0 -> arrayOf(Vec3(minX, minY, minZ), Vec3(minX, minY, maxZ), Vec3(maxX, minY, maxZ), Vec3(maxX, minY, minZ))
        1 -> arrayOf(Vec3(minX, maxY, minZ), Vec3(maxX, maxY, minZ), Vec3(maxX, maxY, maxZ), Vec3(minX, maxY, maxZ))
        2 -> arrayOf(Vec3(minX, minY, minZ), Vec3(maxX, minY, minZ), Vec3(maxX, maxY, minZ), Vec3(minX, maxY, minZ))
        3 -> arrayOf(Vec3(minX, minY, maxZ), Vec3(maxX, minY, maxZ), Vec3(maxX, maxY, maxZ), Vec3(minX, maxY, maxZ))
        4 -> arrayOf(Vec3(minX, minY, minZ), Vec3(minX, minY, maxZ), Vec3(minX, maxY, maxZ), Vec3(minX, maxY, minZ))
        5 -> arrayOf(Vec3(maxX, minY, minZ), Vec3(maxX, minY, maxZ), Vec3(maxX, maxY, maxZ), Vec3(maxX, maxY, minZ))
        else -> emptyArray()
    }

    // Whether the eye is on the outside of the given face, so it could be clicked at all
    fun faceIsVisible(eye: Vec3, level: Level, pos: BlockPos, facing: Direction?): Boolean {
        if (facing == null) return true
        val bounds = blockBounds(level, pos)
        return when (facing.ordinal) {
            0 -> bounds.minY > eye.y
            1 -> eye.y > bounds.maxY
            2 -> bounds.minZ > eye.z
            3 -> eye.z > bounds.maxZ
            4 -> bounds.minX > eye.x
            5 -> eye.x > bounds.maxX
            else -> false
        }
    }

    private fun hitsFace(hit: HitResult?, pos: BlockPos, facingIndex: Int): Boolean {
        val blockHit = hit as? BlockHitResult ?: return false
        if (blockHit.type != HitResult.Type.BLOCK) return false
        if (blockHit.direction.ordinal != facingIndex) return false
        return blockHit.blockPos == pos
    }

    private fun missesOrHits(hit: HitResult?, pos: BlockPos): Boolean {
        if (hit == null || hit.type == HitResult.Type.MISS) return true
        val blockHit = hit as? BlockHitResult ?: return false
        return blockHit.blockPos == pos
    }

    private fun clip(level: Level, from: Vec3, to: Vec3, entity: Entity): BlockHitResult =
        level.clip(ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity))

    // Whether a block standing at sourceBlock would have line of sight to the given face of targetBlock
    fun hasLineToFace(
        sourceBlock: BlockPos,
        player: Player,
        level: Level,
        targetBlock: BlockPos,
        facing: Direction?,
    ): Boolean {
        val bounds = blockBounds(level, targetBlock).deflate(FACE_CONTRACT)
        val start = Vec3(sourceBlock.x + 0.5, player.y + player.eyeHeight, sourceBlock.z + 0.5)
        if (facing == null) {
            val end = closestPointIn(bounds, start)
            return missesOrHits(clip(level, start, end, player), targetBlock)
        }
        return faceCorners(
            facing.ordinal,
            bounds.minX, bounds.minY, bounds.minZ,
            bounds.maxX, bounds.maxY, bounds.maxZ,
        ).any { corner -> missesOrHits(clip(level, start, corner, player), targetBlock) }
    }

    private fun closestPointIn(bounds: AABB, point: Vec3): Vec3 = Vec3(
        point.x.coerceIn(bounds.minX, bounds.maxX),
        point.y.coerceIn(bounds.minY, bounds.maxY),
        point.z.coerceIn(bounds.minZ, bounds.maxZ),
    )

    fun aimPoint(
        player: Player,
        level: Level,
        eye: Vec3,
        target: BlockPlacement,
        yaw: Float,
        pitch: Float,
    ): Vec3? = closestAimPoint(player, level, eye, target, yaw, pitch, Double.POSITIVE_INFINITY)

    fun aimPointWithinReach(
        player: Player,
        level: Level,
        eye: Vec3,
        target: BlockPlacement,
        yaw: Float,
        pitch: Float,
        maximumDistance: Double,
    ): Vec3? = closestAimPoint(player, level, eye, target, yaw, pitch, maximumDistance)

    // Scans the face from its corners inward, keeping the valid point needing the least head movement.
    private fun closestAimPoint(
        player: Player,
        level: Level,
        eye: Vec3,
        target: BlockPlacement,
        yaw: Float,
        pitch: Float,
        maximumDistance: Double,
    ): Vec3? {
        val against = target.against
        val facing = target.facing
        var bounds = blockBounds(level, against)
        if (facing == null) return closestPointIn(bounds, eye)

        bounds = bounds.deflate(FACE_CONTRACT)
        val distanceLimit = maximumDistance + EPSILON
        val dx = max(0.0, max(bounds.minX - eye.x, eye.x - bounds.maxX))
        val dy = max(0.0, max(bounds.minY - eye.y, eye.y - bounds.maxY))
        val dz = max(0.0, max(bounds.minZ - eye.z, eye.z - bounds.maxZ))
        if (dx * dx + dy * dy + dz * dz > distanceLimit * distanceLimit) return null

        val direction = facing.unitVec3i
        val width = bounds.maxX - bounds.minX
        val depth = bounds.maxZ - bounds.minZ
        val height = bounds.maxY - bounds.minY
        val directionX = direction.x.toDouble()
        val directionY = direction.y.toDouble()
        val directionZ = direction.z.toDouble()
        val facingIndex = facing.ordinal

        var bestRotationDistance = Double.MAX_VALUE
        var bestHitPoint: Vec3? = null

        var horizontalInset = 0
        while (horizontalInset <= 70) {
            val horizontalScale = 1.0 - horizontalInset / 100.0
            val insetX = width / 2.0 * horizontalScale
            val insetZ = depth / 2.0 * horizontalScale
            val minX = bounds.minX + (if (directionX > 0.0) width else if (directionX < 0.0) 0.0 else insetX)
            val minZ = bounds.minZ + (if (directionZ > 0.0) depth else if (directionZ < 0.0) 0.0 else insetZ)
            val maxX = bounds.maxX - (if (directionX < 0.0) width else if (directionX > 0.0) 0.0 else insetX)
            val maxZ = bounds.maxZ - (if (directionZ < 0.0) depth else if (directionZ > 0.0) 0.0 else insetZ)

            var verticalInset = 0
            verticalLoop@ while (verticalInset <= 70) {
                val verticalScale = 1.0 - verticalInset / 100.0
                val insetY = height / 2.0 * verticalScale
                val minY = bounds.minY + (if (directionY > 0.0) height else if (directionY < 0.0) 0.0 else insetY)
                val maxY = bounds.maxY - (if (directionY < 0.0) height else if (directionY > 0.0) 0.0 else insetY)

                for (corner in faceCorners(facingIndex, minX, minY, minZ, maxX, maxY, maxZ)) {
                    if (eye.distanceTo(corner) > maximumDistance + EPSILON) continue
                    val rotationDistance = angleBetween(eye, corner, yaw, pitch).toDouble()
                    if (rotationDistance >= bestRotationDistance || rotationDistance <= 0.5) {
                        if (verticalInset == 0) {
                            verticalInset += 10
                            continue@verticalLoop
                        }
                        continue
                    }
                    if (!hitsFace(clip(level, eye, corner, player), against, facingIndex)) continue
                    bestRotationDistance = rotationDistance
                    bestHitPoint = corner
                    if (bestRotationDistance < 1.0) return bestHitPoint
                }
                verticalInset += 10
            }
            horizontalInset += 10
        }
        return bestHitPoint
    }

    // First corner of the face that raytraces cleanly, ignoring how far the head has to move
    fun firstAimPoint(player: Player, level: Level, eye: Vec3, target: BlockPlacement): Vec3? {
        val against = target.against
        val facing = target.facing
        var bounds = blockBounds(level, against)
        if (facing == null) return closestPointIn(bounds, eye)

        bounds = bounds.deflate(FACE_CONTRACT)
        val direction = facing.unitVec3i
        val width = bounds.maxX - bounds.minX
        val depth = bounds.maxZ - bounds.minZ
        val height = bounds.maxY - bounds.minY
        val directionX = direction.x.toDouble()
        val directionY = direction.y.toDouble()
        val directionZ = direction.z.toDouble()
        val facingIndex = facing.ordinal

        var insetPercent = 0
        while (insetPercent <= 100) {
            val scale = 1.0 - insetPercent / 100.0
            val insetX = width / 2.0 * scale
            val insetY = depth / 2.0 * scale
            val insetZ = height / 2.0 * scale
            val minX = bounds.minX + (if (directionX > 0.0) width else if (directionX < 0.0) 0.0 else insetX)
            val minY = bounds.minY + (if (directionY > 0.0) height else if (directionY < 0.0) 0.0 else insetY)
            val minZ = bounds.minZ + (if (directionZ > 0.0) depth else if (directionZ < 0.0) 0.0 else insetZ)
            val maxX = bounds.maxX - (if (directionX < 0.0) width else if (directionX > 0.0) 0.0 else insetX)
            val maxY = bounds.maxY - (if (directionY < 0.0) height else if (directionY > 0.0) 0.0 else insetY)
            val maxZ = bounds.maxZ - (if (directionZ < 0.0) depth else if (directionZ > 0.0) 0.0 else insetZ)

            for (corner in faceCorners(facingIndex, minX, minY, minZ, maxX, maxY, maxZ)) {
                if (hitsFace(clip(level, eye, corner, player), against, facingIndex)) return corner
            }
            insetPercent += 10
        }
        return null
    }

    // Angular distance, in degrees, between the current angles and the angles that face target
    fun angleBetween(source: Vec3, target: Vec3, yaw: Float, pitch: Float): Float {
        val deltaX = source.x - target.x
        val deltaZ = source.z - target.z
        val deltaY = source.y - target.y
        val horizontalDistance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
        val targetYaw = Math.toDegrees(atan2(-(target.x - source.x), target.z - source.z)).toFloat()
        val targetPitch = Mth.wrapDegrees(Math.toDegrees(atan2(deltaY, horizontalDistance)).toFloat())
        val yawDelta = abs(Mth.wrapDegrees(targetYaw - yaw))
        val pitchDelta = abs(Mth.wrapDegrees(targetPitch - pitch))
        return sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta)
    }

    // Angles that look from eye at point
    fun anglesTo(eye: Vec3, point: Vec3): Pair<Float, Float> {
        val dx = point.x - eye.x
        val dy = point.y - eye.y
        val dz = point.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz).coerceAtLeast(1.0E-4)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(atan2(-dy, horizontal)).toFloat().coerceIn(-90f, 90f)
        return yaw to pitch
    }
}
