package onl.luka.grizzly.module.modules.world.scaffold

import onl.luka.grizzly.module.modules.movement.InvMove
import onl.luka.grizzly.util.InputUtil.isPhysicalKeyDown
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Telly bridging. Jumps off the ground every tick it can, looks forward through the jump, then
 * turns around and places once it starts falling. The bridge height is pinned to a launch Y so
 * the line stays flat, and a fast fall, a big drop, damage or a blocked head hand over to plain
 * scaffolding so the block still gets placed.
 */
object Telly {

    private const val CLUTCH_FALL_SPEED = -0.65
    private const val CLUTCH_LAUNCH_DISTANCE = 4.0
    private const val HEAD_CHECK_HEIGHT = 0.42
    private const val KEEP_Y_FALL_TICKS = 4
    private const val GROUND_PITCH = 70f
    private const val AIR_PITCH = 80f
    private const val BACKWARDS_PITCH = 85f
    private const val JUMP_YAW_SPEED = 180f
    private const val MATH_PITCH_SPEED_CAP = 18f
    private const val STRAFE_DEADBAND = 0.14

    private const val FACE_SAMPLE_DIVISIONS = 16
    private const val FACE_CENTER_WEIGHT = 0.12
    private const val EDGE_HIT_PENALTY = 4.0
    private const val SAFE_HIT_INSET = 0.0625
    private const val SEARCH_BASE_ROTATION_SAVING_TICKS = 0.30
    private const val SEARCH_DISTANCE_SAVING_TICKS = 0.42
    private const val SEARCH_DISTANCE_SQUARED_SAVING_TICKS = 0.12
    private const val SEARCH_ORIGIN_PRIORITY_TICKS = 0.30
    private const val SEARCH_PATH_WEIGHT_TICKS = 0.24
    private const val SEARCH_PLAYER_DISTANCE_WEIGHT_TICKS = 0.35
    private const val CONSERVATION_DISTANCE_WEIGHT = 3.0
    private const val CONSERVATION_DIRECTION_WEIGHT = 8.0
    private const val FACING_TOLERANCE = 5f

    private var stage = 0
    private var normalJumpIndex = 0
    private var suppressNormalJumpSprint = false
    private var normalJumpedThisTick = false
    private var jumpedThisTick = false
    private var wasKeepYOnGround = false
    private var jumpDownOnLastGround = false
    private var clutchMode = false
    private var launchY: Int? = null
    private var fallTicks = 0
    private var lastPlaced: BlockPos? = null
    private var lineOrigin: Vec3? = null
    private var lineYaw = 0f
    private var lockedTarget: Target? = null

    private val candidates = ArrayList<BlockPos>(64)
    private val candidateSet = HashSet<BlockPos>(64)
    private val ringBuffer = ArrayList<BlockPos>(32)

    private data class Target(
        val pos: BlockPos,
        val neighbour: BlockPos,
        val hit: Vec3,
        val side: Direction,
        val searchDistance: Int,
    )

    private class PlacementHit(val hit: Vec3, val score: Double, val distanceSqr: Double)

    init {
        ClientTickEvents.START_CLIENT_TICK.register(::onTick)
    }

    private fun onTick(client: Minecraft) {
        if (!Scaffold.isEnabled() || Scaffold.bridgeMode.value != Scaffold.BridgeMode.TELLY) return
        if (client.gui.screen() != null && !InvMove.allowsMovementInCurrentScreen(client)) {
            Scaffold.suspendForOpenScreen(client)
            return
        }

        val player = client.player ?: return
        val world = client.level ?: return

        jumpedThisTick = false
        normalJumpedThisTick = false
        updateFallTicks(player)
        updateClutchMode(player)

        if (!hasBlocks(player)) {
            idle(client)
            return
        }

        val updateGroundKeepY = shouldUpdateGroundKeepY(player)
        wasKeepYOnGround = player.onGround()
        if (player.onGround()) jumpDownOnLastGround = isPhysicalJumpDown(client)

        updateStage(player)
        updateNormalJumpIndex(player)
        updateLaunchY(client, player, updateGroundKeepY)

        updateBridgeLine(client, player)

        Scaffold.tellyRestoreMovementKeys(client)
        applyCorrectiveStrafe(client, player)
        if (Scaffold.tellyDown.value) client.options.keyShift.setDown(false)
        if (Scaffold.noSprint.value && !isJumpPriorityActive(player)) player.isSprinting = false

        handleJumpInput(client, player)
        applyJumpKey(client, player)

        val anchor = targetBlock(player)
        val replaceable = isReplaceable(world, anchor)
        val target = if (replaceable) findTarget(client, player, world, anchor) else null

        aim(client, player, target)

        if (isGroundPhase(client, player)) return
        if (target == null) return
        if (!isRotationReady(player, world, target)) return
        place(client, player, world, target)
    }

    // Jump timing

    private fun updateStage(player: LocalPlayer) {
        if (!isMovementActive(player) || !hasBlocks(player) || shouldUseNormalScaffold(player)) {
            stage = 0
            normalJumpIndex = 0
            suppressNormalJumpSprint = false
            normalJumpedThisTick = false
            return
        }
        if (!player.onGround()) return

        if (stage > 0) stage-- else if (stage < 0) stage++
        if (stage == 0) stage = 1
    }

    private fun handleJumpInput(client: Minecraft, player: LocalPlayer) {
        if (shouldUseNormalScaffold(player)) return

        val normalJump = isNormalJumpTiming(client, player)
        val suppressSprint = shouldSuppressNormalJumpSprint(client, player)

        if ((normalJump || suppressSprint) && player.isSprinting) player.isSprinting = false
        if (isMovementActive(player) && !normalJump && !suppressSprint) {
            client.options.keySprint.setDown(true)
        }

        if (!isGroundPhase(client, player)) return
        if (stage <= 0) {
            client.options.keyJump.setDown(false)
            return
        }

        if (normalJump) {
            player.isSprinting = false
            client.options.keySprint.setDown(false)
            client.options.keyJump.setDown(true)
            jumpedThisTick = true
            normalJumpedThisTick = true
            suppressNormalJumpSprint = true
            recordJump(client, player)
            return
        }

        // A sprint jump is only taken when sprinting actually holds; otherwise no jump at all.
        val sprinting = player.isSprinting || isPhysicalKeyDown(client.options.keySprint) || canStartSprinting(player)
        if (!sprinting) {
            client.options.keyJump.setDown(false)
            return
        }

        client.options.keySprint.setDown(true)
        player.isSprinting = true
        client.options.keyJump.setDown(true)
        jumpedThisTick = true
        normalJumpedThisTick = false
        suppressNormalJumpSprint = false
        recordJump(client, player)
    }

    private fun applyJumpKey(client: Minecraft, player: LocalPlayer) {
        if (isGroundPhase(client, player)) {
            if (!jumpedThisTick) client.options.keyJump.setDown(false)
            return
        }
        if (isUpwardJumpInputActive(client, player)) client.options.keyJump.setDown(true)
    }

    private fun recordJump(client: Minecraft, player: LocalPlayer) {
        if (isNormalJumpModeActive(client, player)) normalJumpIndex++ else normalJumpIndex = 0
    }

    private fun updateNormalJumpIndex(player: LocalPlayer) {
        val client = Minecraft.getInstance()
        if (!isNormalJumpModeActive(client, player) || shouldUseNormalScaffold(player)) {
            normalJumpIndex = 0
            suppressNormalJumpSprint = false
            return
        }
        if (suppressNormalJumpSprint && player.onGround()) suppressNormalJumpSprint = false
    }

    private fun isNormalJumpModeActive(client: Minecraft, player: LocalPlayer): Boolean =
        Scaffold.tellyMode.value == Scaffold.TellyMode.NON_UPWARDS &&
            isPhysicalJumpDown(client) &&
            isMovementActive(player) &&
            hasBlocks(player)

    private fun isNormalJumpTiming(client: Minecraft, player: LocalPlayer): Boolean =
        isNormalJumpModeActive(client, player) && isGroundPhase(client, player) && stage > 0

    private fun shouldSuppressNormalJumpSprint(client: Minecraft, player: LocalPlayer): Boolean =
        isNormalJumpModeActive(client, player) &&
            !shouldUseNormalScaffold(player) &&
            suppressNormalJumpSprint

    private fun canStartSprinting(player: LocalPlayer): Boolean =
        !player.isUsingItem && player.foodData.foodLevel > 6 && !player.isFallFlying

    private fun isGroundPhase(client: Minecraft, player: LocalPlayer): Boolean =
        isMovementActive(player) &&
            hasBlocks(player) &&
            player.onGround() &&
            !shouldUseNormalScaffold(player)

    private fun isUpwardJumpInputActive(client: Minecraft, player: LocalPlayer): Boolean {
        if (!isPhysicalJumpDown(client)) return false
        if (player.onGround()) return true
        return jumpDownOnLastGround
    }

    private fun isJumpPriorityActive(player: LocalPlayer): Boolean =
        isGroundPhase(Minecraft.getInstance(), player) || jumpedThisTick

    // Clutch / normal scaffold fallback

    private fun updateClutchMode(player: LocalPlayer) {
        if (player.onGround()) {
            clutchMode = false
            return
        }
        if (isClutchTrigger(player)) clutchMode = true
    }

    private fun isClutchTrigger(player: LocalPlayer): Boolean =
        player.deltaMovement.y <= CLUTCH_FALL_SPEED ||
            isFarFromLaunchY(player) ||
            player.hurtTime != 0

    private fun isFarFromLaunchY(player: LocalPlayer): Boolean {
        val anchor = launchY ?: return false
        return abs(player.y - anchor) >= CLUTCH_LAUNCH_DISTANCE
    }

    private fun shouldUseNormalScaffold(player: LocalPlayer): Boolean =
        (player.onGround() && isHeadBlocked(player)) ||
            (clutchMode && !player.onGround()) ||
            !isAngleSafe(Minecraft.getInstance(), player)

    /**
     * A telly jump lands on a single block, so the direction of travel has to line up with the
     * block grid or the landing misses. Off axis the module hands back to plain scaffolding,
     * which keeps bridging without launching us off the line.
     */
    private fun isAngleSafe(client: Minecraft, player: LocalPlayer): Boolean {
        if (Scaffold.angleTolerance.value >= 45f) return true
        if (!isMovementActive(player)) return true

        val travel = movementDirection(client, player)
        val snapped = Math.round(travel / 45f) * 45f
        return abs(Mth.wrapDegrees(travel - snapped)) <= Scaffold.angleTolerance.value
    }

    // Bridge line

    /**
     * The line we are meant to be walking, anchored on the block last stood on and running
     * along the nearest 45 degrees. Re-anchoring only on the ground keeps a jump from chasing
     * a line that moved under it.
     */
    private fun updateBridgeLine(client: Minecraft, player: LocalPlayer) {
        if (!isMovementActive(player) || shouldUseNormalScaffold(player)) {
            lineOrigin = null
            return
        }

        if (player.onGround() || lineOrigin == null) {
            val pos = player.blockPosition()
            lineOrigin = Vec3(pos.x + 0.5, 0.0, pos.z + 0.5)
            lineYaw = Mth.wrapDegrees(Math.round(movementDirection(client, player) / 45f) * 45f)
        }
    }

    /** Signed distance from the line, positive when we have drifted to its left. */
    private fun lateralOffset(player: LocalPlayer): Double? {
        val origin = lineOrigin ?: return null
        val radians = Math.toRadians(lineYaw.toDouble())
        val dirX = -Math.sin(radians)
        val dirZ = Math.cos(radians)
        return (player.x - origin.x) * dirZ - (player.z - origin.z) * dirX
    }

    /**
     * Steers back onto the line with a strafe key rather than by touching velocity, so the
     * correction goes out through Move Fix as an input a hand could have made. Forward and back
     * are never touched, only the sideways component.
     */
    private fun applyCorrectiveStrafe(client: Minecraft, player: LocalPlayer) {
        if (!Scaffold.strafeCorrect.value) return
        if (player.onGround() || shouldUseNormalScaffold(player)) return
        if (!isMovementActive(player)) return

        val offset = lateralOffset(player) ?: return
        if (abs(offset) < STRAFE_DEADBAND) return

        val correctionYaw = Mth.wrapDegrees(lineYaw + if (offset > 0.0) 90f else -90f)
        val relative = Mth.wrapDegrees(correctionYaw - RotationManager.getClientYaw())
        // Move Fix reads the keys against the camera, so the correction is expressed as
        // whichever strafe points that way from where the player is actually looking.
        client.options.keyRight.setDown(relative > 0f)
        client.options.keyLeft.setDown(relative < 0f)
    }

    private fun isHeadBlocked(player: LocalPlayer): Boolean {
        val world = Minecraft.getInstance().level ?: return false
        return !world.noCollision(player, player.boundingBox.move(0.0, HEAD_CHECK_HEIGHT, 0.0))
    }

    // Bridge height

    private fun updateFallTicks(player: LocalPlayer) {
        fallTicks = if (player.onGround() || player.deltaMovement.y >= 0.0) 0 else fallTicks + 1
    }

    private fun shouldUpdateGroundKeepY(player: LocalPlayer): Boolean =
        Scaffold.keepY.value &&
            player.onGround() &&
            isPhysicalMovementDown(Minecraft.getInstance()) &&
            !shouldUseNormalScaffold(player) &&
            !wasKeepYOnGround

    private fun shouldFreezeKeepY(client: Minecraft, player: LocalPlayer): Boolean =
        !shouldUseNormalScaffold(player) &&
            !isUpwardJumpInputActive(client, player) &&
            (isMovementActive(player) || !isPhysicalMovementDown(client))

    private fun updateLaunchY(client: Minecraft, player: LocalPlayer, updateGroundKeepY: Boolean) {
        if (updateGroundKeepY) {
            launchY = player.blockPosition().y
        } else if (!shouldFreezeKeepY(client, player) && fallTicks >= KEEP_Y_FALL_TICKS) {
            launchY = player.blockPosition().y
        }

        if (launchY == null) launchY = player.blockPosition().y
        val anchor = launchY!!
        if (player.distanceToSqr(player.x, anchor.toDouble(), player.z) > 15.0) {
            launchY = player.blockPosition().y
        }
        if (isDownActive(client)) launchY = player.blockPosition().y - 1
    }

    private fun targetBlock(player: LocalPlayer): BlockPos {
        val anchor = (launchY ?: player.blockPosition().y).toDouble()
        val pos = player.blockPosition()
        return BlockPos(pos.x, floor(anchor - 0.75).toInt(), pos.z)
    }

    // Placement search

    /**
     * Every candidate is scored rather than taking the first that traces, and the winner is
     * weighed against both the block already under the crosshair and the one held from last
     * tick. Scoring is what keeps a diagonal bridge on its line instead of stepping sideways
     * onto whichever block happened to come first in the candidate order.
     */
    private fun findTarget(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
    ): Target? {
        // Simple and Backwards never look at the block they place on, so scoring hit points for
        // them is wasted work. They take the first candidate whose face centre is in reach.
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) {
            lockedTarget = null
            for (candidate in buildCandidates(client, player, anchor)) {
                val target = findFirstTargetForCandidate(
                    client, player, world, anchor, candidate, manhattan(anchor, candidate)
                )
                if (target != null) return target
            }
            return null
        }

        val rotationTarget = rotationPriorityTarget(client, player, world, anchor)
        val heldTarget = reusableTarget(client, player, world, anchor)
        var best: Target? = null
        var bestScore = Double.MAX_VALUE

        for (candidate in buildCandidates(client, player, anchor)) {
            val searchDistance = manhattan(anchor, candidate)
            if (best != null && searchDistance > best.searchDistance) break

            val target = findTargetForCandidate(client, player, world, anchor, candidate, searchDistance)
                ?: continue
            val score = placementTargetScore(client, player, world, anchor, target)
            if (score < bestScore) {
                best = target
                bestScore = score
            }
        }

        val positionTarget = selectPositionTarget(client, player, world, anchor, heldTarget, best)
        val selected = selectAdaptiveTarget(client, player, anchor, positionTarget, rotationTarget)
        lockedTarget = selected
        return selected
    }

    private fun findTargetForCandidate(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
        pos: BlockPos,
        searchDistance: Int,
    ): Target? {
        if (!isReplaceable(world, pos)) return null
        if (!isPlacementPositionClear(player, pos)) return null

        val eye = player.eyePosition
        val targetCenter = Vec3.atCenterOf(pos)
        var best: Target? = null
        var bestScore = Double.MAX_VALUE

        for (side in orderedPlaceDirections(player, anchor, pos)) {
            val neighbour = pos.relative(side)
            if (!isSupport(world.getBlockState(neighbour))) continue

            val neighbourCenter = Vec3.atCenterOf(neighbour)
            // Clicking a face further away than the block we want puts the new block on the
            // wrong side of its support, so only the down placement is allowed to do it.
            if (!isDownwardsSupport(client, side) &&
                eye.distanceToSqr(targetCenter) >= eye.distanceToSqr(neighbourCenter)
            ) {
                continue
            }

            val sideToClick = side.opposite
            val hit = traceablePlacementHit(player, world, neighbour, sideToClick) ?: continue

            val score = hit.score +
                hit.distanceSqr * 0.01 +
                abs(eye.distanceToSqr(targetCenter) - eye.distanceToSqr(neighbourCenter)) * 0.001 +
                sideScore(sideToClick) +
                bridgeScore(world, anchor, pos, neighbour)
            if (score < bestScore) {
                best = Target(pos, neighbour, hit.hit, sideToClick, searchDistance)
                bestScore = score
            }
        }
        return best
    }

    private fun findFirstTargetForCandidate(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
        pos: BlockPos,
        searchDistance: Int,
    ): Target? {
        if (!isReplaceable(world, pos)) return null
        if (!isPlacementPositionClear(player, pos)) return null

        val eye = player.eyePosition
        val targetCenter = Vec3.atCenterOf(pos)
        val reachSqr = reach(player) * reach(player)

        for (side in orderedPlaceDirections(player, anchor, pos)) {
            val neighbour = pos.relative(side)
            if (!isSupport(world.getBlockState(neighbour))) continue

            val neighbourCenter = Vec3.atCenterOf(neighbour)
            if (!isDownwardsSupport(client, side) &&
                eye.distanceToSqr(targetCenter) >= eye.distanceToSqr(neighbourCenter)
            ) {
                continue
            }

            val sideToClick = side.opposite
            val hit = faceCenter(neighbour, sideToClick)
            if (eye.distanceToSqr(hit) > reachSqr) continue

            return Target(pos, neighbour, hit, sideToClick, searchDistance)
        }
        return null
    }

    /**
     * Tries the rotation we are already on, then the face centre, then a sweep of the face, and
     * keeps the point the raycast actually landed on rather than the point we aimed at. Hits
     * near the rim of a face score badly because a tick of drift makes them miss entirely.
     */
    private fun traceablePlacementHit(
        player: LocalPlayer,
        world: ClientLevel,
        neighbour: BlockPos,
        side: Direction,
    ): PlacementHit? {
        val reach = reach(player)
        val reachSqr = reach * reach
        val eye = player.eyePosition
        val faceCenter = faceCenter(neighbour, side)
        val currentYaw = RotationManager.getCurrentYaw()
        val currentPitch = RotationManager.getCurrentPitch()

        var currentHit: PlacementHit? = null
        var bestHit: Vec3? = null
        var bestDistanceSqr = 0.0
        var bestScore = Double.MAX_VALUE

        val onCrosshair = rayTrace(player, world, currentYaw, currentPitch, reach)
        if (matchesHit(onCrosshair, neighbour, side)) {
            val hit = onCrosshair!!.location
            currentHit = PlacementHit(hit, 0.0, eye.distanceToSqr(hit))
            if (isStableHit(hit, faceCenter, side)) {
                bestHit = hit
                bestDistanceSqr = currentHit.distanceSqr
                bestScore = placementHitScore(
                    hit, currentYaw, currentPitch, currentYaw, currentPitch, eye, faceCenter, side, true
                )
            }
        }

        val centerRotation = rotationTo(player, faceCenter)
        val centerTrace = rayTrace(player, world, centerRotation.first, centerRotation.second, reach)
        if (matchesHit(centerTrace, neighbour, side)) {
            val hit = centerTrace!!.location
            val score = placementHitScore(
                hit, centerRotation.first, centerRotation.second, currentYaw, currentPitch,
                eye, faceCenter, side, currentHit != null
            )
            if (score < bestScore) {
                bestHit = hit
                bestDistanceSqr = eye.distanceToSqr(hit)
                bestScore = score
            }
        }

        val xSteps = sampleCount(side, Direction.EAST, Direction.WEST)
        val ySteps = sampleCount(side, Direction.UP, Direction.DOWN)
        val zSteps = sampleCount(side, Direction.SOUTH, Direction.NORTH)

        for (xi in 0 until xSteps) {
            val dx = faceOffset(side, Direction.EAST, Direction.WEST, xi)
            for (yi in 0 until ySteps) {
                val dy = faceOffset(side, Direction.UP, Direction.DOWN, yi)
                for (zi in 0 until zSteps) {
                    val dz = faceOffset(side, Direction.SOUTH, Direction.NORTH, zi)
                    val point = Vec3(neighbour.x + dx, neighbour.y + dy, neighbour.z + dz)
                    if (eye.distanceToSqr(point) > reachSqr) continue

                    val rotation = rotationTo(player, point)
                    val trace = rayTrace(player, world, rotation.first, rotation.second, reach)
                    if (!matchesHit(trace, neighbour, side)) continue

                    val hit = trace!!.location
                    val score = placementHitScore(
                        hit, rotation.first, rotation.second, currentYaw, currentPitch,
                        eye, faceCenter, side, currentHit != null
                    )
                    if (score < bestScore) {
                        bestScore = score
                        bestDistanceSqr = eye.distanceToSqr(hit)
                        bestHit = hit
                    }
                }
            }
        }

        val hit = bestHit ?: return currentHit
        return PlacementHit(hit, bestScore, bestDistanceSqr)
    }

    private fun placementHitScore(
        hit: Vec3,
        yaw: Float,
        pitch: Float,
        currentYaw: Float,
        currentPitch: Float,
        eye: Vec3,
        faceCenter: Vec3,
        side: Direction,
        currentCanHit: Boolean,
    ): Double {
        val centerScore = normalizedFaceCenterDistanceSqr(hit, faceCenter, side)
        var score = rotationScore(yaw, pitch, currentYaw, currentPitch) +
            eye.distanceToSqr(hit) * 0.001 +
            centerScore * FACE_CENTER_WEIGHT

        if (!isStableHit(hit, faceCenter, side)) score += EDGE_HIT_PENALTY
        if (currentCanHit) score += centerScore * 1.5
        return score
    }

    private fun isStableHit(hit: Vec3, faceCenter: Vec3, side: Direction): Boolean {
        val safeRadius = 0.5 - SAFE_HIT_INSET
        val dx = abs(hit.x - faceCenter.x)
        val dy = abs(hit.y - faceCenter.y)
        val dz = abs(hit.z - faceCenter.z)
        return when (side.axis) {
            Direction.Axis.X -> dy <= safeRadius && dz <= safeRadius
            Direction.Axis.Y -> dx <= safeRadius && dz <= safeRadius
            Direction.Axis.Z -> dx <= safeRadius && dy <= safeRadius
        }
    }

    private fun normalizedFaceCenterDistanceSqr(hit: Vec3, faceCenter: Vec3, side: Direction): Double {
        val x = (hit.x - faceCenter.x) * 2.0
        val y = (hit.y - faceCenter.y) * 2.0
        val z = (hit.z - faceCenter.z) * 2.0
        return when (side.axis) {
            Direction.Axis.X -> y * y + z * z
            Direction.Axis.Y -> x * x + z * z
            Direction.Axis.Z -> x * x + y * y
        }
    }

    private fun isPlacementPositionClear(player: LocalPlayer, pos: BlockPos): Boolean =
        !AABB(pos).intersects(player.boundingBox.deflate(1.0e-7))

    private fun isDownwardsSupport(client: Minecraft, side: Direction): Boolean =
        side == Direction.UP && isDownActive(client)

    private fun sampleCount(side: Direction, positive: Direction, negative: Direction): Int =
        if (side == positive || side == negative) 1 else FACE_SAMPLE_DIVISIONS

    private fun faceOffset(side: Direction, positive: Direction, negative: Direction, index: Int): Double {
        if (side == positive) return 1.0
        if (side == negative) return 0.0
        return (index + 0.5) / FACE_SAMPLE_DIVISIONS
    }

    private fun buildCandidates(client: Minecraft, player: LocalPlayer, anchor: BlockPos): List<BlockPos> {
        candidates.clear()
        candidateSet.clear()
        addCandidate(anchor)

        val range = currentSearchRange(client, player)
        val searchYaw = searchYaw(client, player)
        val sides = orderedHorizontals(searchYaw)

        if (range >= 1) sides.forEach { addCandidate(anchor.relative(it)) }
        if (range >= 2) {
            for (first in sides) {
                for (second in sides) {
                    if (second == first.opposite) continue
                    addCandidate(anchor.relative(first).relative(second))
                }
            }
        }
        for (distance in 3..range) addRing(anchor, distance, searchYaw)
        return candidates
    }

    private fun addRing(anchor: BlockPos, distance: Int, searchYaw: Float) {
        ringBuffer.clear()
        for (x in -distance..distance) {
            for (z in -distance..distance) {
                if (abs(x) + abs(z) != distance) continue
                ringBuffer.add(anchor.offset(x, 0, z))
            }
        }
        ringBuffer.sortWith { first, second ->
            val yawCompare = candidateYawDistance(anchor, first, searchYaw)
                .compareTo(candidateYawDistance(anchor, second, searchYaw))
            if (yawCompare != 0) yawCompare else {
                abs(first.x - anchor.x).compareTo(abs(second.x - anchor.x))
            }
        }
        ringBuffer.forEach(::addCandidate)
    }

    private fun addCandidate(pos: BlockPos) {
        if (candidateSet.add(pos)) candidates.add(pos)
    }

    /**
     * Taken from where the player is actually travelling rather than the keys being held. Mid
     * jump the momentum carries the diagonal even when the input has changed, and the bridge
     * has to be searched along the momentum or the blocks land off the line.
     */
    private fun searchYaw(client: Minecraft, player: LocalPlayer): Float {
        val motion = player.deltaMovement
        val yaw = if (motion.x * motion.x + motion.z * motion.z > 1.0e-4) {
            Math.toDegrees(atan2(-motion.x, motion.z)).toFloat()
        } else {
            movementDirection(client, player)
        }
        return normalizeYaw(yaw + 180f)
    }

    private fun orderedHorizontals(searchYaw: Float): List<Direction> =
        listOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
            .sortedBy { abs(Mth.wrapDegrees(horizontalYaw(it) - searchYaw)) }

    private fun orderedPlaceDirections(player: LocalPlayer, anchor: BlockPos, pos: BlockPos): List<Direction> {
        val client = Minecraft.getInstance()
        val searchYaw = searchYaw(client, player)
        val preferred = bridgeSupportDirection(anchor, pos)
        val horizontals = orderedHorizontals(searchYaw).filter { it != preferred }
        return buildList {
            preferred?.let { add(it) }
            addAll(horizontals)
            add(Direction.DOWN)
            add(Direction.UP)
        }
    }

    private fun bridgeSupportDirection(anchor: BlockPos, candidate: BlockPos): Direction? {
        val placed = lastPlaced ?: return null
        if (placed.y != candidate.y || placed.y != anchor.y) return null
        val dx = placed.x - candidate.x
        val dz = placed.z - candidate.z
        if (abs(dx) + abs(dz) != 1) return null
        return when {
            dx > 0 -> Direction.EAST
            dx < 0 -> Direction.WEST
            dz > 0 -> Direction.SOUTH
            else -> Direction.NORTH
        }
    }

    private fun bridgeScore(world: ClientLevel, anchor: BlockPos, pos: BlockPos, neighbour: BlockPos): Double {
        val placed = lastPlaced ?: return 0.0
        if (placed.y != anchor.y) return 0.0
        if (!isSupport(world.getBlockState(placed))) return 0.0

        val bridgeDistance = manhattan(placed, anchor)
        var score = if (bridgeDistance <= 1) 0.0 else {
            val fromPlaced = manhattan(placed, pos)
            val detour = fromPlaced + manhattan(pos, anchor) - bridgeDistance
            (max(0, fromPlaced - 1) + max(0, detour)) * 0.25
        }
        if (neighbour == placed) score -= 1.0
        return score
    }

    private fun placementTargetScore(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
        target: Target,
    ): Double {
        val (yaw, pitch) = rotationTo(player, target.hit)
        return rotationScore(yaw, pitch, RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch()) +
            target.searchDistance * 4.0 +
            sideScore(target.side) +
            conservationScore(client, player, anchor, target) +
            bridgeScore(world, anchor, target.pos, target.neighbour)
    }

    /**
     * Telly only gets one placement per jump, so wandering off the travel line costs a block we
     * cannot spare. Candidates away from the direction of travel are charged for both the extra
     * distance and how far off the line they sit, which is what holds a diagonal together.
     */
    private fun conservationScore(
        client: Minecraft,
        player: LocalPlayer,
        anchor: BlockPos,
        target: Target,
    ): Double {
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return 0.0
        if (shouldUseNormalScaffold(player) || isDownActive(client)) return 0.0
        if (target.pos == anchor) return 0.0

        val alignment = travelAlignment(client, player, anchor, target.pos)
        val directionPenalty = (1.0 - alignment.coerceIn(-1.0, 1.0)) * CONSERVATION_DIRECTION_WEIGHT
        return target.searchDistance * CONSERVATION_DISTANCE_WEIGHT + directionPenalty
    }

    private fun travelAlignment(
        client: Minecraft,
        player: LocalPlayer,
        anchor: BlockPos,
        candidate: BlockPos,
    ): Double {
        val direction = travelVector(client, player)
        val directionLength = sqrt(direction.x * direction.x + direction.z * direction.z)
        if (directionLength <= 1.0e-6) return -1.0

        val offsetX = (candidate.x - anchor.x).toDouble()
        val offsetZ = (candidate.z - anchor.z).toDouble()
        val offsetLength = sqrt(offsetX * offsetX + offsetZ * offsetZ)
        if (offsetLength <= 1.0e-6) return 1.0

        return (offsetX * direction.x + offsetZ * direction.z) / (offsetLength * directionLength)
    }

    /** Actual velocity while there is any, falling back to the pressed keys when standing still. */
    private fun travelVector(client: Minecraft, player: LocalPlayer): Vec3 {
        val motion = player.deltaMovement
        val horizontal = Vec3(motion.x, 0.0, motion.z)
        if (horizontal.lengthSqr() > 1.0e-4) return horizontal

        val radians = Math.toRadians(movementDirection(client, player).toDouble())
        return Vec3(-Math.sin(radians), 0.0, Math.cos(radians))
    }

    /** The block already under the crosshair, so an in reach placement is never turned away from. */
    private fun rotationPriorityTarget(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
    ): Target? {
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return null

        val reach = reach(player)
        val hit = rayTrace(
            player, world, RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch(), reach
        ) ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null

        val neighbour = hit.blockPos
        val side = hit.direction
        val pos = neighbour.relative(side)
        if (pos.y != anchor.y) return null

        val searchDistance = manhattan(anchor, pos)
        if (searchDistance > currentSearchRange(client, player)) return null
        if (!isReplaceable(world, pos)) return null
        if (!isPlacementPositionClear(player, pos)) return null
        if (!isSupport(world.getBlockState(neighbour))) return null
        if (player.eyePosition.distanceToSqr(hit.location) > reach * reach + 1.0e-4) return null

        return Target(pos, neighbour, hit.location, side, searchDistance)
    }

    /** Last tick's pick, kept alive so the aim is not restarted at a new block every tick. */
    private fun reusableTarget(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
    ): Target? {
        val target = lockedTarget ?: return null
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return null
        if (target.pos.y != anchor.y) return null

        val searchDistance = manhattan(anchor, target.pos)
        if (searchDistance > currentSearchRange(client, player)) return null
        if (!isReplaceable(world, target.pos)) return null
        if (!isSupport(world.getBlockState(target.neighbour))) return null
        if (player.eyePosition.distanceToSqr(target.hit) > reach(player) * reach(player) + 1.0e-4) return null

        val updated = if (target.searchDistance == searchDistance) target else target.copy(searchDistance = searchDistance)
        return if (isIdealHitReachable(player, world, updated)) updated else null
    }

    private fun isIdealHitReachable(player: LocalPlayer, world: ClientLevel, target: Target): Boolean {
        val (yaw, pitch) = rotationTo(player, target.hit)
        return matchesHit(rayTrace(player, world, yaw, pitch, reach(player)), target.neighbour, target.side)
    }

    private fun selectPositionTarget(
        client: Minecraft,
        player: LocalPlayer,
        world: ClientLevel,
        anchor: BlockPos,
        held: Target?,
        best: Target?,
    ): Target? {
        if (held == null) return best
        if (best == null) return held
        if (samePlacement(held, best)) return held

        if (held.searchDistance != best.searchDistance) {
            return if (best.searchDistance < held.searchDistance) best else held
        }

        val heldReady = currentHit(player, world, held) != null
        val bestReady = currentHit(player, world, best) != null
        if (heldReady && !bestReady) return held
        if (!heldReady && bestReady) return best

        val heldScore = placementTargetScore(client, player, world, anchor, held)
        val bestScore = placementTargetScore(client, player, world, anchor, best)
        return if (bestScore + 10.0 < heldScore) best else held
    }

    /**
     * A block further away is only worth taking when the rotation it saves outweighs the extra
     * search distance, measured in ticks of turning at the configured speed.
     */
    private fun selectAdaptiveTarget(
        client: Minecraft,
        player: LocalPlayer,
        anchor: BlockPos,
        positionTarget: Target?,
        rotationTarget: Target?,
    ): Target? {
        if (positionTarget == null) return rotationTarget
        if (rotationTarget == null) return positionTarget
        if (samePlacement(positionTarget, rotationTarget)) return rotationTarget

        val distanceGap = rotationTarget.searchDistance - positionTarget.searchDistance
        if (distanceGap <= 0) return rotationTarget

        if (!isStableHit(rotationTarget.hit, faceCenter(rotationTarget.neighbour, rotationTarget.side), rotationTarget.side)) {
            return positionTarget
        }

        val positionTicks = placementRotationTicks(player, positionTarget)
        val rotationTicks = placementRotationTicks(player, rotationTarget)
        val preRotationTicks = availablePreRotationTicks(client, player, anchor, positionTarget)
        val saving = max(0.0, positionTicks - preRotationTicks) - rotationTicks

        var required = SEARCH_BASE_ROTATION_SAVING_TICKS +
            distanceGap * SEARCH_DISTANCE_SAVING_TICKS +
            distanceGap * distanceGap * SEARCH_DISTANCE_SQUARED_SAVING_TICKS

        if (positionTarget.searchDistance == 0) required += SEARCH_ORIGIN_PRIORITY_TICKS
        required += searchPathPenaltyTicks(client, player, anchor, rotationTarget.pos, distanceGap)

        val distanceLoss = horizontalDistanceToBlock(player, rotationTarget.pos) -
            horizontalDistanceToBlock(player, positionTarget.pos)
        if (distanceLoss > 0.0) required += distanceLoss * SEARCH_PLAYER_DISTANCE_WEIGHT_TICKS

        if (!player.onGround() && player.deltaMovement.y < 0.0) {
            required += distanceGap * SEARCH_PATH_WEIGHT_TICKS
        }

        return if (saving >= required) rotationTarget else positionTarget
    }

    private fun searchPathPenaltyTicks(
        client: Minecraft,
        player: LocalPlayer,
        anchor: BlockPos,
        candidate: BlockPos,
        distanceGap: Int,
    ): Double {
        if (distanceGap <= 0) return 0.0
        val direction = travelVector(client, player)
        val directionLength = sqrt(direction.x * direction.x + direction.z * direction.z)
        if (directionLength <= 1.0e-6) return distanceGap * SEARCH_PATH_WEIGHT_TICKS

        val offsetX = (candidate.x - anchor.x).toDouble()
        val offsetZ = (candidate.z - anchor.z).toDouble()
        val offsetLength = sqrt(offsetX * offsetX + offsetZ * offsetZ)
        if (offsetLength <= 1.0e-6) return 0.0

        val alignment = (offsetX * direction.x + offsetZ * direction.z) / (offsetLength * directionLength)
        return (1.0 - alignment.coerceIn(-1.0, 1.0)) * distanceGap * SEARCH_PATH_WEIGHT_TICKS
    }

    private fun placementRotationTicks(player: LocalPlayer, target: Target): Double {
        val (yaw, pitch) = rotationTo(player, target.hit)
        val yawDiff = abs(Mth.wrapDegrees(yaw - RotationManager.getCurrentYaw()))
        val pitchDiff = abs(pitch - RotationManager.getCurrentPitch())
        return max(rotationTicks(yawDiff, expectedYawSpeed()), rotationTicks(pitchDiff, expectedPitchSpeed()))
    }

    private fun availablePreRotationTicks(
        client: Minecraft,
        player: LocalPlayer,
        anchor: BlockPos,
        target: Target,
    ): Double {
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return 0.0
        if (target.pos == anchor) return 0.0

        val motion = player.deltaMovement
        val speed = sqrt(motion.x * motion.x + motion.z * motion.z)
        if (speed <= 1.0e-4) return 0.0

        val alignment = travelAlignment(client, player, anchor, target.pos)
        if (alignment <= 0.0) return 0.0

        val lead = (max(0.35, speed * 2.0 + 0.15)).coerceIn(0.35, 0.85)
        return (lead / speed).coerceIn(0.0, 8.0) * alignment.coerceIn(0.0, 1.0)
    }

    private fun rotationTicks(angle: Float, degreesPerTick: Double): Double {
        if (angle <= 1.0e-3f) return 0.0
        if (degreesPerTick <= 1.0e-3) return Double.POSITIVE_INFINITY
        return angle / degreesPerTick
    }

    private fun expectedYawSpeed(): Double {
        val low = min(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value).toDouble()
        val high = max(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value).toDouble()
        return (low + high) * 0.5
    }

    private fun expectedPitchSpeed(): Double {
        val low = min(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value).toDouble()
        val high = max(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value).toDouble()
        val safeHigh = min(high, MATH_PITCH_SPEED_CAP.toDouble())
        if (safeHigh <= 0.0) return 0.0
        var safeLow = if (low > MATH_PITCH_SPEED_CAP) min(12.0, safeHigh) else min(low, safeHigh)
        safeLow = max(min(1.0, safeHigh), safeLow)
        return (safeLow + safeHigh) * 0.5
    }

    private fun horizontalDistanceToBlock(player: LocalPlayer, pos: BlockPos): Double {
        val x = max(max(pos.x - player.x, player.x - (pos.x + 1.0)), 0.0)
        val z = max(max(pos.z - player.z, player.z - (pos.z + 1.0)), 0.0)
        return sqrt(x * x + z * z)
    }

    private fun samePlacement(first: Target, second: Target): Boolean =
        first.pos == second.pos && first.neighbour == second.neighbour && first.side == second.side

    private fun sideScore(side: Direction): Double = when {
        side == Direction.UP -> 0.0
        side.axis.isHorizontal -> 0.05
        else -> 0.1
    }

    private fun rotationScore(yaw: Float, pitch: Float, currentYaw: Float, currentPitch: Float): Double =
        abs(Mth.wrapDegrees(yaw - currentYaw)).toDouble() + abs(pitch - currentPitch).toDouble()

    private fun currentSearchRange(client: Minecraft, player: LocalPlayer): Int {
        if (isDownActive(client)) return 1
        val motion = player.deltaMovement
        val stationary = motion.x * motion.x + motion.z * motion.z < 1.0e-6
        if (isPhysicalJumpDown(client) && stationary) return 0
        return Scaffold.searchRange.value
    }

    // Rotation

    private fun aim(client: Minecraft, player: LocalPlayer, target: Target?) {
        if (shouldAvoidAiming(client, player)) {
            aimJump(client, player, target)
            return
        }

        val rotation = when (Scaffold.tellyRotationMode.value) {
            Scaffold.TellyRotationMode.SIMPLE -> simpleRotation(client, player)
            Scaffold.TellyRotationMode.BACKWARDS -> backwardsRotation(client, player, target)
            else -> target?.let { rotationTo(player, it.hit) } ?: simpleRotation(client, player)
        }
        applyRotation(rotation.first, rotation.second, rotationSpeed())
    }

    /** Through the jump the view stays on the direction of travel instead of the placement. */
    private fun aimJump(client: Minecraft, player: LocalPlayer, target: Target?) {
        val speed = JUMP_YAW_SPEED
        if (isNormalJumpRotationTiming(client, player)) {
            // A plain jump keeps whatever the view is already on, but the rotation stays held
            // so the camera is never handed back mid-bridge.
            applyRotation(RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch(), speed)
            return
        }

        val yaw = movementDirection(client, player)
        val pitch = jumpPitch(player, target)
        if (pitch == null) {
            applyRotation(yaw, RotationManager.getCurrentPitch(), speed, speed)
        } else {
            applyRotation(yaw, pitch, speed, rotationSpeed())
        }
    }

    private fun jumpPitch(player: LocalPlayer, target: Target?): Float? {
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return null
        return target?.let { rotationTo(player, it.hit).second } ?: idlePitch(player)
    }

    private fun shouldAvoidAiming(client: Minecraft, player: LocalPlayer): Boolean {
        if (shouldUseNormalScaffold(player)) return false
        if (isGroundPhase(client, player)) return true
        return isMovementActive(player) && hasBlocks(player) && fallTicks <= 0
    }

    private fun isNormalJumpRotationTiming(client: Minecraft, player: LocalPlayer): Boolean =
        isNormalJumpTiming(client, player) ||
            (normalJumpedThisTick && isNormalJumpModeActive(client, player))

    private fun simpleRotation(client: Minecraft, player: LocalPlayer): Pair<Float, Float> {
        val yaw = Mth.wrapDegrees(movementDirection(client, player) - 180f)
        return snapYaw(yaw) to idlePitch(player)
    }

    private fun backwardsRotation(client: Minecraft, player: LocalPlayer, target: Target?): Pair<Float, Float> {
        val yaw = Mth.wrapDegrees(movementDirection(client, player) - 180f)
        val pitch = target?.let { rotationTo(player, it.hit).second } ?: BACKWARDS_PITCH
        return yaw to pitch
    }

    private fun idlePitch(player: LocalPlayer): Float =
        if (player.onGround()) GROUND_PITCH else AIR_PITCH

    private fun snapYaw(yaw: Float): Float = Mth.wrapDegrees(Math.round(yaw / 45f) * 45f)

    private fun applyRotation(yaw: Float, pitch: Float, speed: Float) =
        applyRotation(yaw, pitch, speed, pitchSpeed(speed))

    private fun applyRotation(yaw: Float, pitch: Float, speed: Float, pitchSpeed: Float) {
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.perspective = true
        // Krs leaves movement to its MovementFix module rather than correcting it in the
        // scaffold, so this hands correction to Move Fix instead of claiming it. Overriding the
        // physics yaw here would walk the player wherever the spoofed rotation points.
        RotationManager.setTargetRotation(
            Mth.wrapDegrees(yaw),
            pitch.coerceIn(-90f, 90f),
            Scaffold.ROTATION_OWNER,
            handlesMovementCorrection = false,
        )
        Scaffold.ownsRotation = true
        RotationManager.quickTick(speed, pitchSpeed)
        RotationManager.skipPositionSnap = true
    }

    private fun rotationSpeed(): Float {
        val low = min(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value)
        val high = max(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value)
        return if (high > low) low + Random.nextFloat() * (high - low) else low
    }

    /**
     * Math turns the yaw at the configured speed but creeps the pitch, capped at 18 degrees a
     * tick. Driving both from one number was making the yaw crawl behind the placement.
     */
    private fun pitchSpeed(yawSpeed: Float): Float {
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) return yawSpeed
        if (isDownActive(Minecraft.getInstance())) return yawSpeed

        val low = min(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value)
        val high = max(Scaffold.minRotSpeed.value, Scaffold.rotSpeed.value)
        val safeHigh = min(high, MATH_PITCH_SPEED_CAP)
        if (safeHigh <= 0f) return 0f
        var safeLow = if (low > MATH_PITCH_SPEED_CAP) min(12f, safeHigh) else min(low, safeHigh)
        safeLow = max(min(1f, safeHigh), safeLow)
        return if (safeHigh > safeLow) safeLow + Random.nextFloat() * (safeHigh - safeLow) else safeLow
    }

    private fun isRotationReady(player: LocalPlayer, world: ClientLevel, target: Target): Boolean {
        val client = Minecraft.getInstance()
        if (isDownActive(client)) return true
        if (Scaffold.tellyRotationMode.value != Scaffold.TellyRotationMode.MATH) {
            return isFacingRotationTarget(client, player, target)
        }
        return currentHit(player, world, target) != null
    }

    /** Simple and Backwards gate on the aim reaching their own rotation, not on a traced hit. */
    private fun isFacingRotationTarget(client: Minecraft, player: LocalPlayer, target: Target): Boolean {
        val rotation = when (Scaffold.tellyRotationMode.value) {
            Scaffold.TellyRotationMode.SIMPLE -> simpleRotation(client, player)
            Scaffold.TellyRotationMode.BACKWARDS -> backwardsRotation(client, player, target)
            else -> return true
        }
        val yawDiff = abs(Mth.wrapDegrees(RotationManager.getCurrentYaw() - rotation.first))
        val pitchDiff = abs(RotationManager.getCurrentPitch() - rotation.second)
        return yawDiff <= FACING_TOLERANCE && pitchDiff <= FACING_TOLERANCE
    }

    // Placement

    private fun place(client: Minecraft, player: LocalPlayer, world: ClientLevel, target: Target) {
        val slot = Scaffold.tellyBlockSlot(player)
        if (slot == -1) return
        if (player.inventory.selectedSlot != slot) player.inventory.setSelectedSlot(slot)

        val hit = currentHit(player, world, target)
            ?: BlockHitResult(target.hit, target.side, target.neighbour, false)
        val result = client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit) ?: return
        if (result.consumesAction()) {
            lastPlaced = target.pos
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    private fun currentHit(player: LocalPlayer, world: ClientLevel, target: Target): BlockHitResult? {
        val hit = rayTrace(
            player,
            world,
            RotationManager.getCurrentYaw(),
            RotationManager.getCurrentPitch(),
            reach(player),
        )
        return hit.takeIf { matchesHit(it, target.neighbour, target.side) }
    }

    private fun rayTrace(
        player: LocalPlayer,
        world: ClientLevel,
        yaw: Float,
        pitch: Float,
        reach: Double,
    ): BlockHitResult? {
        val savedYaw = player.yRot
        val savedPitch = player.xRot
        player.yRot = yaw
        player.xRot = pitch
        val hit = player.pick(reach, 1.0f, false) as? BlockHitResult
        player.yRot = savedYaw
        player.xRot = savedPitch
        return hit
    }

    private fun matchesHit(hit: BlockHitResult?, neighbour: BlockPos, side: Direction): Boolean =
        hit != null &&
            hit.type == HitResult.Type.BLOCK &&
            hit.blockPos == neighbour &&
            hit.direction == side

    // Shared helpers

    private fun reach(player: LocalPlayer): Double = player.blockInteractionRange()

    private fun faceCenter(neighbour: BlockPos, side: Direction): Vec3 =
        Vec3.atCenterOf(neighbour).add(side.stepX * 0.5, side.stepY * 0.5, side.stepZ * 0.5)

    private fun rotationTo(player: LocalPlayer, point: Vec3): Pair<Float, Float> {
        val diff = point.subtract(player.eyePosition)
        val horizontal = sqrt(diff.x * diff.x + diff.z * diff.z)
        val yaw = Mth.wrapDegrees(Math.toDegrees(atan2(-diff.x, diff.z)).toFloat())
        val pitch = Mth.wrapDegrees(-Math.toDegrees(atan2(diff.y, horizontal)).toFloat())
        return yaw to pitch.coerceIn(-90f, 90f)
    }

    private fun isReplaceable(world: ClientLevel, pos: BlockPos): Boolean =
        world.getBlockState(pos).canBeReplaced()

    private fun isSupport(state: BlockState): Boolean =
        !state.isAir && !state.canBeReplaced() && state.fluidState.isEmpty

    private fun manhattan(from: BlockPos, to: BlockPos): Int =
        abs(to.x - from.x) + abs(to.z - from.z)

    private fun candidateYawDistance(anchor: BlockPos, candidate: BlockPos, searchYaw: Float): Float {
        val x = candidate.x - anchor.x
        val z = candidate.z - anchor.z
        if (x == 0 && z == 0) return 0f
        val yaw = normalizeYaw(Math.toDegrees(atan2(-x.toDouble(), z.toDouble())).toFloat())
        return abs(Mth.wrapDegrees(yaw - searchYaw))
    }

    private fun horizontalYaw(direction: Direction): Float = when (direction) {
        Direction.SOUTH -> 0f
        Direction.WEST -> 90f
        Direction.NORTH -> 180f
        Direction.EAST -> 270f
        else -> 0f
    }

    private fun normalizeYaw(yaw: Float): Float = (yaw % 360f + 360f) % 360f

    /** Vanilla's movement direction for the pressed keys, in the 0..360 range. */
    private fun movementDirection(client: Minecraft, player: LocalPlayer): Float {
        val forward = isPhysicalKeyDown(client.options.keyUp)
        val backward = isPhysicalKeyDown(client.options.keyDown)
        val left = isPhysicalKeyDown(client.options.keyLeft)
        val right = isPhysicalKeyDown(client.options.keyRight)

        var yaw = RotationManager.getClientYaw()
        val multiplier: Float
        if (backward && !forward) {
            yaw += 180f
            multiplier = -0.5f
        } else if (forward && !backward) {
            multiplier = 0.5f
        } else {
            multiplier = 1f
        }
        if (left && !right) yaw -= 90f * multiplier
        if (right && !left) yaw += 90f * multiplier
        return normalizeYaw(yaw)
    }

    private fun isMovementActive(player: LocalPlayer): Boolean {
        val motion = player.deltaMovement
        return isPhysicalMovementDown(Minecraft.getInstance()) ||
            motion.x * motion.x + motion.z * motion.z > 1.0e-6
    }

    private fun isPhysicalMovementDown(client: Minecraft): Boolean =
        isPhysicalKeyDown(client.options.keyUp) ||
            isPhysicalKeyDown(client.options.keyDown) ||
            isPhysicalKeyDown(client.options.keyLeft) ||
            isPhysicalKeyDown(client.options.keyRight)

    private fun isPhysicalJumpDown(client: Minecraft): Boolean =
        isPhysicalKeyDown(client.options.keyJump)

    private fun isDownActive(client: Minecraft): Boolean =
        Scaffold.tellyDown.value && isPhysicalKeyDown(client.options.keyShift)

    private fun hasBlocks(player: LocalPlayer): Boolean = Scaffold.tellyBlockSlot(player) != -1

    private fun idle(client: Minecraft) {
        reset()
        if (Scaffold.ownsRotation) Scaffold.releaseRotation()
        Scaffold.tellyRestoreMovementKeys(client)
        client.options.keyJump.setDown(isPhysicalKeyDown(client.options.keyJump))
        client.options.keySprint.setDown(isPhysicalKeyDown(client.options.keySprint))
    }

    private fun reset() {
        stage = 0
        normalJumpIndex = 0
        suppressNormalJumpSprint = false
        normalJumpedThisTick = false
        jumpedThisTick = false
        wasKeepYOnGround = false
        jumpDownOnLastGround = false
        clutchMode = false
        fallTicks = 0
        launchY = null
        lastPlaced = null
        lineOrigin = null
        lockedTarget = null
    }

    fun onDisabled() {
        reset()
        val client = Minecraft.getInstance()
        Scaffold.releaseRotation()
        Scaffold.tellyRestoreMovementKeys(client)
        client.options.keyJump.setDown(isPhysicalKeyDown(client.options.keyJump))
        client.options.keySprint.setDown(isPhysicalKeyDown(client.options.keySprint))
    }
}
