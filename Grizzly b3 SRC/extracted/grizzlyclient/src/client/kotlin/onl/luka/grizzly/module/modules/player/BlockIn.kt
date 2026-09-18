package onl.luka.grizzly.module.modules.player

import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.command.KeyNames
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.RotationManager
import onl.luka.grizzly.util.interactBlock
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

object BlockIn : Module(
    name = "Block In",
    description = "Surrounds you with blocks when its separate activation bind is pressed",
    category = Category.PLAYER,
) {
    private const val ROTATION_OWNER = "block_in"
    private const val REACH = 4.5
    private const val MAX_SEARCH_DEPTH = 5

    private val rotationSpeed = float("rotation speed", 10.0f, 1.0f, 30.0f)
    private val randomization = int("randomization", 10, 0, 100)
    private val rotationTolerance = float("rotation tolerance", 25.0f, 1.0f, 100.0f)
    private val activateKey = keybind("activate key", GLFW.GLFW_KEY_UNKNOWN).onPress {
        if (isEnabled()) {
            if (active) stop(Minecraft.getInstance()) else start()
        }
    }
    private val ignoreBlocks = boolean("ignore blocks", false)
    private val ignoredBlocks = itemList(
        "ignored blocks",
        defaultMode = ItemListEntry.Mode.BLACKLIST,
        filter = ItemListEntry.Filter.BLOCKS_ONLY,
    )

    private data class Placement(
        val placePos: BlockPos,
        val support: BlockPos,
        val face: Direction,
        val yaw: Float,
        val pitch: Float,
    )

    private var active = false
    private var previousSlot = -1
    private var filledCount = 0
    private var stalledTicks = 0

    init {
        ignoredBlocks.visibleWhen = { ignoreBlocks.value }
        ClientTickEvents.START_CLIENT_TICK.register(::tick)
    }

    private fun start() {
        active = true
        previousSlot = -1
        filledCount = 0
        stalledTicks = 0
    }

    private fun tick(client: Minecraft) {
        if (!isEnabled() || !active) return

        val player = client.player
        val level = client.level
        if (player == null || level == null || !player.isAlive || player.isSpectator || client.gui.screen() != null) {
            stop(client)
            return
        }

        if (RotationManager.hasExternalRotation(ROTATION_OWNER)) return

        val feet = BlockPos.containing(player.x, player.y, player.z)
        val goals = enclosureGoals(feet)
        val remaining = goals.filter { level.getBlockState(it).canBeReplaced() }
        filledCount = goals.size - remaining.size
        if (remaining.isEmpty()) {
            stop(client)
            return
        }

        val slot = findStrongestBlockSlot(player)
        if (slot == -1) {
            stop(client)
            return
        }
        if (previousSlot == -1) previousSlot = player.inventory.selectedSlot
        player.inventory.setSelectedSlot(slot)

        val placement = findPlacement(player, level, remaining, feet)
        if (placement == null) {
            stalledTicks++
            if (stalledTicks >= 30) stop(client)
            return
        }
        stalledTicks = 0

        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.setTargetRotation(placement.yaw, placement.pitch, ROTATION_OWNER)
        val variance = randomization.value / 100.0f
        val speedScale = 1.0f + Random.nextFloat() * variance - variance * 0.5f
        RotationManager.quickTick((rotationSpeed.value * speedScale).coerceAtLeast(1.0f))
        RotationManager.physicsYawOverride = RotationManager.getCurrentYaw()
        RotationManager.skipPositionSnap = true

        if (!withinTolerance(placement)) return
        val currentHit = rayTraceAt(
            player,
            RotationManager.getCurrentYaw(),
            RotationManager.getCurrentPitch(),
        ) ?: return
        if (currentHit.blockPos != placement.support || currentHit.direction != placement.face) return
        if (currentHit.blockPos.relative(currentHit.direction) != placement.placePos) return
        if (level.getBlockState(placement.placePos).canBeReplaced().not()) return
        if (player.mainHandItem.item !is BlockItem) return

        if (interactBlock(currentHit) is InteractionResult.Success) {
            stalledTicks = 0
        }
    }

    private fun findPlacement(
        player: LocalPlayer,
        level: ClientLevel,
        goals: List<BlockPos>,
        feet: BlockPos,
    ): Placement? {
        findForGoals(player, level, goals, feet)?.let { return it }

        val seen = goals.toMutableSet()
        var frontier = goals
        repeat(MAX_SEARCH_DEPTH) {
            val next = ArrayList<BlockPos>(frontier.size * 4)
            for (goal in frontier) {
                for (direction in Direction.entries) {
                    val candidate = goal.relative(direction)
                    if (!seen.add(candidate) || candidate == feet || candidate == feet.above()) continue
                    if (!level.getBlockState(candidate).canBeReplaced() || intersectsPlayer(player, candidate)) continue
                    next += candidate
                }
            }
            if (next.isEmpty()) return null
            findForGoals(player, level, next, feet)?.let { return it }
            frontier = next
        }
        return null
    }

    private fun findForGoals(
        player: LocalPlayer,
        level: ClientLevel,
        goals: List<BlockPos>,
        feet: BlockPos,
    ): Placement? {
        var best: Placement? = null
        var bestCost = Float.POSITIVE_INFINITY
        for (goal in goals) {
            if (goal == feet || goal == feet.above() || intersectsPlayer(player, goal)) continue
            for (face in Direction.entries) {
                val support = goal.relative(face.opposite)
                val state = level.getBlockState(support)
                if (state.canBeReplaced() || !state.fluidState.isEmpty) continue

                for (point in faceAimPoints(support, face)) {
                    if (player.eyePosition.distanceToSqr(point) > REACH * REACH) continue
                    val (yaw, pitch) = rotationTo(player, point)
                    val hit = rayTraceAt(player, yaw, pitch) ?: continue
                    if (hit.blockPos != support || hit.direction != face) continue
                    if (hit.blockPos.relative(hit.direction) != goal) continue

                    val cost = abs(Mth.wrapDegrees(yaw - RotationManager.getCurrentYaw())) +
                        abs(pitch - RotationManager.getCurrentPitch())
                    if (cost < bestCost) {
                        bestCost = cost
                        best = Placement(goal, support, face, yaw, pitch)
                    }
                }
            }
        }
        return best
    }

    private fun enclosureGoals(feet: BlockPos): List<BlockPos> = buildList {
        add(feet.above(2))
        for (direction in Direction.Plane.HORIZONTAL) {
            add(feet.relative(direction))
            add(feet.above().relative(direction))
        }
    }

    private fun findStrongestBlockSlot(player: LocalPlayer): Int {
        var bestSlot = -1
        var bestStrength = Float.NEGATIVE_INFINITY
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            val item = stack.item as? BlockItem ?: continue
            if (stack.isEmpty || isIgnored(stack)) continue
            val strength = item.block.defaultDestroyTime()
            if (strength > bestStrength) {
                bestStrength = strength
                bestSlot = slot
            }
        }
        return bestSlot
    }

    private fun isIgnored(stack: ItemStack): Boolean {
        if (!ignoreBlocks.value) return false
        val id = BuiltInRegistries.ITEM.getKey(stack.item).path.lowercase()
        return ignoredBlocks.value.any { entry ->
            if (entry.endsWith("_category")) {
                val category = itemCategories.firstOrNull { it.id == entry.removeSuffix("_category") }
                category?.matches(id) == true
            } else {
                id == entry.substringAfter(':').lowercase()
            }
        }
    }

    private fun withinTolerance(placement: Placement): Boolean {
        val yawDifference = abs(Mth.wrapDegrees(placement.yaw - RotationManager.getCurrentYaw()))
        val pitchDifference = abs(placement.pitch - RotationManager.getCurrentPitch())
        return yawDifference <= rotationTolerance.value && pitchDifference <= rotationTolerance.value
    }

    private fun rayTraceAt(player: LocalPlayer, yaw: Float, pitch: Float): BlockHitResult? {
        val savedYaw = player.yRot
        val savedPitch = player.xRot
        player.yRot = yaw
        player.xRot = pitch
        val result = player.pick(if (player.isCreative) 5.0 else REACH, 1.0f, false)
        player.yRot = savedYaw
        player.xRot = savedPitch
        return result as? BlockHitResult
    }

    private fun faceAimPoints(support: BlockPos, face: Direction): List<Vec3> {
        val centerX = support.x + 0.5 + face.stepX * 0.49
        val centerY = support.y + 0.5 + face.stepY * 0.49
        val centerZ = support.z + 0.5 + face.stepZ * 0.49
        val offsets = doubleArrayOf(0.0, -0.32, 0.32, -0.16, 0.16)
        return when (face.axis) {
            Direction.Axis.X -> offsets.flatMap { y -> offsets.map { z -> Vec3(centerX, centerY + y, centerZ + z) } }
            Direction.Axis.Y -> offsets.flatMap { x -> offsets.map { z -> Vec3(centerX + x, centerY, centerZ + z) } }
            Direction.Axis.Z -> offsets.flatMap { x -> offsets.map { y -> Vec3(centerX + x, centerY + y, centerZ) } }
        }
    }

    private fun rotationTo(player: LocalPlayer, point: Vec3): Pair<Float, Float> {
        val deltaX = point.x - player.x
        val deltaY = point.y - (player.y + player.eyeHeight)
        val deltaZ = point.z - player.z
        val horizontal = sqrt(deltaX * deltaX + deltaZ * deltaZ).coerceAtLeast(0.001)
        val yaw = Math.toDegrees(atan2(-deltaX, deltaZ)).toFloat()
        val pitch = Math.toDegrees(atan2(-deltaY, horizontal)).toFloat().coerceIn(-90.0f, 90.0f)
        return yaw to pitch
    }

    private fun intersectsPlayer(player: LocalPlayer, pos: BlockPos): Boolean = player.boundingBox.intersects(
        AABB(
            pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
            pos.x + 1.0, pos.y + 1.0, pos.z + 1.0,
        ),
    )

    private fun stop(client: Minecraft) {
        active = false
        stalledTicks = 0
        filledCount = 0
        client.player?.let { player ->
            if (previousSlot in 0..8) player.inventory.setSelectedSlot(previousSlot)
        }
        previousSlot = -1
        if (RotationManager.ownsRotation(ROTATION_OWNER)) {
            RotationManager.clearRotation(ROTATION_OWNER)
        }
    }

    override fun onDisabled() {
        stop(Minecraft.getInstance())
    }

    override fun hudInfo(): String {
        if (active) return "$filledCount/9"
        if (activateKey.value == GLFW.GLFW_KEY_UNKNOWN) return ""
        return "[${KeyNames.displayName(activateKey.value).uppercase(Locale.ROOT)}]"
    }
}
