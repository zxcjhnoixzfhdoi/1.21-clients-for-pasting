package onl.luka.grizzly.module.modules.world.clutch

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// Player movement run forward a tick at a time, against a copy of the state so it cannot touch the world.
internal class MotionSim(
    private val level: Level,
    player: LocalPlayer,
    // Direction the held keys actually carry the player. While a clutch is running the view is
    // turned onto the block being placed, but movement correction keeps them going the old way.
    private val movementYaw: Float = player.yRot,
) {
    var x: Double = player.x
        private set
    var y: Double = player.y
        private set
    var z: Double = player.z
        private set
    var motionX: Double = player.deltaMovement.x
        private set
    var motionY: Double = player.deltaMovement.y
        private set
    var motionZ: Double = player.deltaMovement.z
        private set
    var onGround: Boolean = player.onGround()
        private set
    var sprinting: Boolean = player.isSprinting
        private set

    val yaw: Float = player.yRot
    val pitch: Float = player.xRot

    private val width = player.bbWidth.toDouble()
    private val height = player.bbHeight.toDouble()
    private val eyeOffset = player.eyeHeight.toDouble()
    private val speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED)
    private val gravity = player.getAttributeValue(Attributes.GRAVITY).coerceIn(0.0, 0.2)
    private val jumpBoost = player.getEffect(MobEffects.JUMP_BOOST)?.amplifier?.plus(1) ?: 0
    private val sneaking = player.isCrouching

    private var jumpTicks = 0

    var forwardInput = 0f
    var strafeInput = 0f
    var jumpInput = false

    // Blocks the simulation should collide with that are not in the world yet
    private val pendingBlocks = HashSet<BlockPos>()

    fun addPendingBlock(pos: BlockPos) {
        pendingBlocks += pos
    }

    fun boundingBox(): AABB {
        val half = width / 2.0
        return AABB(x - half, y, z - half, x + half, y + height, z + half)
    }

    fun eyePosition(): Vec3 = Vec3(x, y + eyeOffset, z)

    fun position(): Vec3 = Vec3(x, y, z)

    // Copies the inputs the player is physically holding
    fun setInput(forward: Boolean, backward: Boolean, left: Boolean, right: Boolean, jump: Boolean) {
        forwardInput = axisInput(forward, backward)
        strafeInput = axisInput(left, right)
        jumpInput = jump
    }

    private fun axisInput(positive: Boolean, negative: Boolean): Float = when {
        positive == negative -> 0f
        positive -> 1f
        else -> -1f
    }

    // One player tick: input, jump, travel, collide
    fun tick() {
        if (jumpTicks > 0) jumpTicks--

        // Vanilla zeroes motion below this before doing anything with it.
        if (abs(motionX) < 0.003) motionX = 0.0
        if (abs(motionY) < 0.003) motionY = 0.0
        if (abs(motionZ) < 0.003) motionZ = 0.0

        var forward = forwardInput
        var strafe = strafeInput
        if (sneaking) {
            forward *= 0.3f
            strafe *= 0.3f
        }

        if (jumpInput && onGround && jumpTicks == 0) {
            jump()
            jumpTicks = 10
        }

        strafe *= 0.98f
        forward *= 0.98f
        travel(strafe.toDouble(), forward.toDouble())
    }

    private fun jump() {
        motionY = 0.42 + jumpBoost * 0.1
        if (sprinting) {
            val yawRadians = Math.toRadians(movementYaw.toDouble())
            motionX -= sin(yawRadians) * 0.2
            motionZ += cos(yawRadians) * 0.2
        }
    }

    private fun travel(strafe: Double, forward: Double) {
        val friction = if (onGround) blockFrictionBelow() * 0.91 else 0.91
        val acceleration = if (onGround) {
            speed * (0.21600002 / (friction * friction * friction))
        } else {
            AIR_ACCELERATION * (if (sprinting) 1.3 else 1.0)
        }

        moveRelative(acceleration, strafe, forward)
        move(motionX, motionY, motionZ)

        motionY -= gravity
        motionY *= 0.98
        motionX *= friction
        motionZ *= friction
    }

    // Vanilla's `getInputVector`: rotate the input into world space and scale it
    private fun moveRelative(amount: Double, strafe: Double, forward: Double) {
        val lengthSquared = strafe * strafe + forward * forward
        if (lengthSquared < 1.0E-7) return
        val scale = if (lengthSquared > 1.0) amount / sqrt(lengthSquared) else amount
        val scaledStrafe = strafe * scale
        val scaledForward = forward * scale
        val yawRadians = Math.toRadians(movementYaw.toDouble())
        val sinYaw = sin(yawRadians)
        val cosYaw = cos(yawRadians)
        motionX += scaledStrafe * cosYaw - scaledForward * sinYaw
        motionZ += scaledForward * cosYaw + scaledStrafe * sinYaw
    }

    private fun blockFrictionBelow(): Double {
        val pos = BlockPos(floor(x).toInt(), floor(y - 0.5).toInt(), floor(z).toInt())
        return level.getBlockState(pos).block.friction.toDouble()
    }

    // Swept collision in vanilla's axis order, using the game's own shape solver.
    private fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
        var box = boundingBox()
        val searchBox = box.expandTowards(deltaX, deltaY, deltaZ).inflate(1.0)
        val shapes = ArrayList<VoxelShape>()
        level.getBlockCollisions(null, searchBox).forEach(shapes::add)
        for (pos in pendingBlocks) {
            val cube = AABB(
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                pos.x + 1.0, pos.y + 1.0, pos.z + 1.0,
            )
            if (cube.intersects(searchBox)) shapes += Shapes.create(cube)
        }

        var movedX = deltaX
        var movedY = deltaY
        var movedZ = deltaZ
        if (shapes.isNotEmpty()) {
            if (movedY != 0.0) {
                movedY = Shapes.collide(Direction.Axis.Y, box, shapes, movedY)
                box = box.move(0.0, movedY, 0.0)
            }
            val xSmaller = abs(movedX) < abs(movedZ)
            if (xSmaller && movedZ != 0.0) {
                movedZ = Shapes.collide(Direction.Axis.Z, box, shapes, movedZ)
                box = box.move(0.0, 0.0, movedZ)
            }
            if (movedX != 0.0) {
                movedX = Shapes.collide(Direction.Axis.X, box, shapes, movedX)
                if (!xSmaller) box = box.move(movedX, 0.0, 0.0)
            }
            if (!xSmaller && movedZ != 0.0) {
                movedZ = Shapes.collide(Direction.Axis.Z, box, shapes, movedZ)
            }
        }

        x += movedX
        y += movedY
        z += movedZ

        onGround = deltaY < 0.0 && movedY != deltaY
        if (movedY != deltaY) motionY = 0.0
        if (movedX != deltaX) motionX = 0.0
        if (movedZ != deltaZ) motionZ = 0.0
    }

    private companion object {
        const val AIR_ACCELERATION = 0.02
    }
}
