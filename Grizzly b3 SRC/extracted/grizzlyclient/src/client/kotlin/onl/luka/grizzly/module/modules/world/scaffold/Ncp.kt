package onl.luka.grizzly.module.modules.world.scaffold

import onl.luka.grizzly.module.modules.movement.InvMove
import onl.luka.grizzly.util.InputUtil.isPhysicalKeyDown
import onl.luka.grizzly.util.NotificationManager
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Ncp {

    private const val YAW_STEP = 45f
    private const val PITCH_JITTER = 5f
    private const val ATTEMPT_INTERVAL_MS = 15L
    private const val SAFETY_FALL_SPEED = -0.7

    private var lastPos: BlockPos? = null
    private var lastFace: Direction? = null
    private var lastAttempt = 0L
    private var lastSend = 0L
    private val queue = ArrayDeque<Placement>(4)

    private class Placement(val neighbour: BlockPos, val face: Direction, val hit: Vec3)

    init {
        ClientTickEvents.START_CLIENT_TICK.register(::onTick)
    }

    private fun onTick(client: Minecraft) {
        if (!Scaffold.isEnabled() || Scaffold.bridgeMode.value != Scaffold.BridgeMode.NCP) return
        if (client.gui.screen() != null && !InvMove.allowsMovementInCurrentScreen(client)) {
            Scaffold.suspendForOpenScreen(client)
            return
        }

        val player = client.player ?: return
        val world = client.level ?: return

        if (Scaffold.ncpSafety.value && player.deltaMovement.y < SAFETY_FALL_SPEED) {
            NotificationManager.showError("Scaffold", "Disabled to avoid a fast fall placement")
            Scaffold.disable()
            return
        }

        val towering = isPhysicalKeyDown(client.options.keyJump) && !isMovementDown(client)
        //if (towering) applyTower(player)

        val anchor = findAnchor(client, player, world)
        if (anchor != null) {
            val placement = findPlacement(world, anchor)
            if (placement != null) {
                lastPos = placement.neighbour
                lastFace = placement.face
                aim(player, placement)
                enqueue(placement)
            }
        }

        flush(client, player, world)
    }

    // Tower

    private fun applyTower(player: LocalPlayer) {
        player.setDeltaMovement(0.0, 0.0, 0.0)
        player.setPos(player.x, floor(player.y), player.z)
    }

    // Target selection

    private fun findAnchor(client: Minecraft, player: LocalPlayer, world: ClientLevel): BlockPos? {
        val drop = if (player.deltaMovement.y > 0.0) 1.005 else 1.0
        val base = BlockPos(
            floor(player.x).toInt(),
            floor(player.y - drop).toInt(),
            floor(player.z).toInt(),
        )
        if (isReplaceable(world, base)) return base

        // Reach ahead along the direction of travel so a gap in front still gets filled.
        val yaw = Math.toRadians(movementDirection(client, player).toDouble())
        val stepX = -sin(yaw)
        val stepZ = cos(yaw)
        for (step in 1..Scaffold.ncpExtend.value) {
            val candidate = BlockPos(
                floor(player.x + stepX * step).toInt(),
                base.y,
                floor(player.z + stepZ * step).toInt(),
            )
            if (isReplaceable(world, candidate)) return candidate
        }
        return null
    }

    private fun findPlacement(world: ClientLevel, anchor: BlockPos): Placement? {
        for (face in Direction.entries) {
            val neighbour = anchor.relative(face)
            val state = world.getBlockState(neighbour)
            if (state.isAir || state.canBeReplaced() || !state.fluidState.isEmpty) continue
            val side = face.opposite
            return Placement(neighbour, side, faceCenter(neighbour, side))
        }
        return null
    }

    // Rotation

    private fun aim(player: LocalPlayer, placement: Placement) {
        val (yaw, pitch) = rotationTo(player, placement.hit)
        val snappedYaw = Mth.wrapDegrees(floor(yaw / YAW_STEP) * YAW_STEP)
        val jittered = (pitch + (Random.nextFloat() * 2f - 1f) * PITCH_JITTER).coerceIn(-90f, 90f)

        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.perspective = true
        RotationManager.setTargetRotation(
            snappedYaw,
            jittered,
            Scaffold.ROTATION_OWNER,
            handlesMovementCorrection = false,
        )
        Scaffold.ownsRotation = true
        RotationManager.flickTick()
    }

    // Placement

    private fun enqueue(placement: Placement) {
        val now = System.currentTimeMillis()
        if (now - lastAttempt < ATTEMPT_INTERVAL_MS) return
        lastAttempt = now
        if (queue.any { it.neighbour == placement.neighbour && it.face == placement.face }) return
        if (queue.size >= 4) queue.removeFirst()
        queue.addLast(placement)
    }

    private fun flush(client: Minecraft, player: LocalPlayer, world: ClientLevel) {
        if (queue.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastSend < Scaffold.ncpPlaceDelay.value) return

        val placement = queue.removeFirst()
        val state = world.getBlockState(placement.neighbour)
        if (state.isAir || state.canBeReplaced()) return

        val slot = Scaffold.scaffoldBlockSlot(player)
        if (slot == -1) return
        val previousSlot = player.inventory.selectedSlot
        if (previousSlot != slot) player.inventory.setSelectedSlot(slot)

        lastSend = now
        val hit = BlockHitResult(placement.hit, placement.face, placement.neighbour, false)
        val result = client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
        if (result?.consumesAction() == true && Scaffold.ncpSwing.value) {
            player.swing(InteractionHand.MAIN_HAND)
        }

        if (Scaffold.ncpRestoreSlot.value && previousSlot != slot) {
            player.inventory.setSelectedSlot(previousSlot)
        }
    }

    // Helpers

    private fun isReplaceable(world: ClientLevel, pos: BlockPos): Boolean =
        world.getBlockState(pos).canBeReplaced()

    private fun faceCenter(neighbour: BlockPos, side: Direction): Vec3 =
        Vec3.atCenterOf(neighbour).add(side.stepX * 0.5, side.stepY * 0.5, side.stepZ * 0.5)

    private fun rotationTo(player: LocalPlayer, point: Vec3): Pair<Float, Float> {
        val diff = point.subtract(player.eyePosition)
        val horizontal = sqrt(diff.x * diff.x + diff.z * diff.z)
        val yaw = Mth.wrapDegrees(Math.toDegrees(atan2(-diff.x, diff.z)).toFloat())
        val pitch = Mth.wrapDegrees(-Math.toDegrees(atan2(diff.y, horizontal)).toFloat())
        return yaw to pitch.coerceIn(-90f, 90f)
    }

    private fun isMovementDown(client: Minecraft): Boolean =
        isPhysicalKeyDown(client.options.keyUp) ||
            isPhysicalKeyDown(client.options.keyDown) ||
            isPhysicalKeyDown(client.options.keyLeft) ||
            isPhysicalKeyDown(client.options.keyRight)

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
        return Mth.wrapDegrees(yaw)
    }

    fun onDisabled() {
        lastPos = null
        lastFace = null
        lastAttempt = 0L
        lastSend = 0L
        queue.clear()
        if (Scaffold.ownsRotation) Scaffold.releaseRotation()
        Scaffold.tellyRestoreMovementKeys(Minecraft.getInstance())
    }
}
