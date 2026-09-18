package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object AntiCheatGeometry {
    fun eyePosition(tracked: TrackedPlayer, entity: Player): Vec3 =
        eyePosition(entity, tracked.serverPosition)

    fun serverBox(tracked: TrackedPlayer, entity: Player): AABB =
        serverBox(entity, tracked.serverPosition)

    fun eyePosition(entity: Player, position: Vec3): Vec3 =
        position.add(0.0, entity.eyeHeight.toDouble(), 0.0)

    fun serverBox(entity: Player, position: Vec3): AABB =
        entity.boundingBox.move(position.subtract(entity.position()))

    fun lookVector(yaw: Float, pitch: Float): Vec3 {
        val yawRadians = Math.toRadians(yaw.toDouble())
        val pitchRadians = Math.toRadians(pitch.toDouble())
        val cosPitch = cos(pitchRadians)
        return Vec3(-sin(yawRadians) * cosPitch, -sin(pitchRadians), cos(yawRadians) * cosPitch)
    }

    fun aimsAt(
        attacker: TrackedPlayer,
        attackerEntity: Player,
        victim: TrackedPlayer,
        victimEntity: Player,
        yaw: Float = attacker.yaw,
        pitch: Float = attacker.pitch,
        inflate: Double = 0.12,
    ): Boolean = aimsAt(
        attackerEntity,
        attacker.serverPosition,
        victimEntity,
        victim.serverPosition,
        yaw,
        pitch,
        inflate,
    )

    fun aimsAt(
        attackerEntity: Player,
        attackerPosition: Vec3,
        victimEntity: Player,
        victimPosition: Vec3,
        yaw: Float,
        pitch: Float,
        inflate: Double = 0.12,
    ): Boolean {
        val eye = eyePosition(attackerEntity, attackerPosition)
        val end = eye.add(lookVector(yaw, pitch).scale(7.0))
        return serverBox(victimEntity, victimPosition).inflate(inflate).clip(eye, end).isPresent
    }

    fun rotationError(
        attacker: TrackedPlayer,
        attackerEntity: Player,
        victim: TrackedPlayer,
        victimEntity: Player,
        yaw: Float = attacker.yaw,
        pitch: Float = attacker.pitch,
    ): Double {
        val eye = eyePosition(attacker, attackerEntity)
        val target = serverBox(victim, victimEntity).center
        val dx = target.x - eye.x
        val dy = target.y - eye.y
        val dz = target.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val targetYaw = Math.toDegrees(kotlin.math.atan2(-dx, dz)).toFloat()
        val targetPitch = -Math.toDegrees(kotlin.math.atan2(dy, horizontal)).toFloat()
        val yawError = abs(Mth.wrapDegrees(yaw - targetYaw)).toDouble()
        val pitchError = abs(pitch - targetPitch).toDouble()
        return sqrt(yawError * yawError + pitchError * pitchError)
    }

    fun hasLineOfSight(client: Minecraft, event: AttackEvent): Boolean {
        val level = client.level ?: return true
        val eye = eyePosition(event.attacker, event.attackerEntity)
        val box = serverBox(event.victim, event.victimEntity)
        val points = listOf(
            box.center,
            Vec3(box.center.x, box.minY + box.ysize * 0.75, box.center.z),
            Vec3(box.minX + box.xsize * 0.2, box.center.y, box.center.z),
            Vec3(box.maxX - box.xsize * 0.2, box.center.y, box.center.z),
        )
        return points.any { point ->
            level.clip(
                ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, event.attackerEntity),
            ).type == HitResult.Type.MISS
        }
    }

    fun aimsAtPlacementSupport(
        client: Minecraft,
        event: PlacementEvent,
        yaw: Float? = null,
        pitch: Float? = null,
        position: Vec3? = null,
    ): Boolean {
        val level = client.level ?: return true
        val snapshot = event.player.latest ?: return true
        val look = lookVector(yaw ?: snapshot.yaw, pitch ?: snapshot.pitch)
        val basePosition = position ?: snapshot.position
        return possibleEyeHeights(snapshot, event.entity).any { eyeHeight ->
            val eye = basePosition.add(0.0, eyeHeight, 0.0)
            Direction.entries.any { direction ->
                val supportPos = event.position.relative(direction)
                val supportState = level.getBlockState(supportPos)
                if (supportState.canBeReplaced()) return@any false

                val face = direction.opposite
                val normal = Vec3(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble())
                val denominator = look.dot(normal)
                if (denominator >= -1.0E-6) return@any false

                val faceCenter = Vec3(
                    supportPos.x + 0.5 + face.stepX * 0.5,
                    supportPos.y + 0.5 + face.stepY * 0.5,
                    supportPos.z + 0.5 + face.stepZ * 0.5,
                )
                val distance = faceCenter.subtract(eye).dot(normal) / denominator
                if (distance !in 0.0..PLACEMENT_REACH) return@any false
                val hitPoint = eye.add(look.scale(distance))
                if (!insideBlockFace(hitPoint, supportPos, face)) return@any false

                val obstruction = level.clip(
                    ClipContext(
                        eye,
                        hitPoint.add(look.scale(0.02)),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        event.entity,
                    ),
                )
                obstruction.type == HitResult.Type.MISS ||
                    obstruction.blockPos.let { it == event.position || it == supportPos }
            }
        }
    }

    private fun possibleEyeHeights(snapshot: PlayerSnapshot, entity: Player): Set<Double> {
        val observed = entity.eyeHeight.toDouble()
        val protocolPose = when {
            snapshot.swimming || snapshot.fallFlying -> 0.4
            snapshot.sneaking -> 1.27
            else -> 1.62
        }
        return setOf(observed, protocolPose)
    }

    private fun insideBlockFace(point: Vec3, block: net.minecraft.core.BlockPos, face: Direction): Boolean {
        val epsilon = 1.0E-4
        val xInside = point.x >= block.x - epsilon && point.x <= block.x + 1.0 + epsilon
        val yInside = point.y >= block.y - epsilon && point.y <= block.y + 1.0 + epsilon
        val zInside = point.z >= block.z - epsilon && point.z <= block.z + 1.0 + epsilon
        return when (face.axis) {
            Direction.Axis.X -> yInside && zInside
            Direction.Axis.Y -> xInside && zInside
            Direction.Axis.Z -> xInside && yInside
        }
    }

    private const val PLACEMENT_REACH = 5.25
}
