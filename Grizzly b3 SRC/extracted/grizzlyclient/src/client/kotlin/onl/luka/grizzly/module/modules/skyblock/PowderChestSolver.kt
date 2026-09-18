package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.world.Nuker
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

object PowderChestSolver : Module(
    "Powder Chest Solver",
    "Finds powder chests and aims at their lock-picking particle",
    Category.SKYBLOCK,
) {
    private const val ROTATION_OWNER = "powder-chest-solver"
    private const val PARTICLE_CHEST_RADIUS = 2
    private const val MAX_CHEST_DISTANCE = 4.75

    private val legitMode = boolean("legit mode", false)
    private val smoothRotations = boolean("smooth rotations", true)
    private val rotationSpeed = float("rotation speed", 25f, 1f, 180f)
    private val chestColor = color("chest color", Color(250, 150, 20, 190))
    private val particleColor = color("particle color", Color(255, 55, 55, 230))

    private val solved = mutableSetOf<BlockPos>()
    private var closestChest: BlockPos? = null
    private var particle: Vec3? = null
    private var ticks = 0

    override fun onDisabled() {
        clear()
    }

    override fun onTick(client: Minecraft) {
        val level = client.level ?: return clear()
        val player = client.player ?: return clear()

        closestChest?.let { chest ->
            if (player.eyePosition.distanceTo(Vec3.atCenterOf(chest)) > MAX_CHEST_DISTANCE ||
                level.getBlockState(chest).block !is ChestBlock ||
                chest in solved
            ) {
                closestChest = null
                particle = null
                RotationManager.clearRotation(ROTATION_OWNER)
            }
        }

        if (closestChest == null && ++ticks % 4 == 0) {
            closestChest = if (legitMode.value) {
                (client.hitResult as? net.minecraft.world.phys.BlockHitResult)?.blockPos
                    ?.takeIf { level.getBlockState(it).block is ChestBlock && it !in solved }
            } else {
                SkyBlockUtils.nearbyBlocks(player.blockPosition(), 4, 4)
                    .filter { level.getBlockState(it).block is ChestBlock && it !in solved }
                    .minByOrNull { player.eyePosition.distanceToSqr(Vec3.atCenterOf(it)) }
            }
            if (closestChest != null) Nuker.pauseForPowderChest()
        }

        particle?.let { point -> aimAtParticle(client, point) }
            ?: RotationManager.clearRotation(ROTATION_OWNER)
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        val boxes = buildList {
            closestChest?.let { add(SkyBlockBox(AABB(it).deflate(0.004), chestColor.value)) }
            particle?.let {
                add(SkyBlockBox(AABB(it.x - 0.04, it.y - 0.04, it.z - 0.04, it.x + 0.04, it.y + 0.04, it.z + 0.04), particleColor.value))
            }
        }
        renderSkyBlockBoxes(ctx, boxes, 2f)
    }

    @JvmStatic
    fun isSolving(): Boolean = isEnabled() && closestChest != null

    @JvmStatic
    fun onParticle(packet: ClientboundLevelParticlesPacket) {
        if (!isEnabled()) return
        val particleType = packet.particle.type
        val knownLockParticle =
            particleType == ParticleTypes.CRIT || particleType == ParticleTypes.ENCHANTED_HIT
        if (!knownLockParticle) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return
        val point = Vec3(packet.x, packet.y, packet.z)
        val origin = BlockPos.containing(point.x, point.y, point.z)

        val candidates = buildSet {
            closestChest?.let(::add)
            if (knownLockParticle && !legitMode.value) {
                for (x in -PARTICLE_CHEST_RADIUS..PARTICLE_CHEST_RADIUS) {
                    for (y in -PARTICLE_CHEST_RADIUS..PARTICLE_CHEST_RADIUS) {
                        for (z in -PARTICLE_CHEST_RADIUS..PARTICLE_CHEST_RADIUS) {
                            add(origin.offset(x, y, z))
                        }
                    }
                }
            }
        }

        val matchedChest = candidates
            .asSequence()
            .filter { it !in solved }
            .filter { player.eyePosition.distanceTo(Vec3.atCenterOf(it)) <= MAX_CHEST_DISTANCE }
            .mapNotNull { chest -> particleDistanceToChestFront(level, chest, point)?.let { chest to it } }
            .minByOrNull { it.second }
            ?.first
            ?: return

        val lockedChest = closestChest
        if (lockedChest != null && matchedChest != lockedChest) return

        closestChest = matchedChest
        particle = point
        Nuker.pauseForPowderChest()
        aimAtParticle(mc, point)
    }

    @JvmStatic
    fun onSystemChat(message: String) {
        if (!isEnabled() || !message.contains("picked the lock", ignoreCase = true)) return
        closestChest?.let(solved::add)
        closestChest = null
        particle = null
        RotationManager.clearRotation(ROTATION_OWNER)
    }

    private fun particleDistanceToChestFront(
        level: net.minecraft.world.level.Level,
        chest: BlockPos,
        point: Vec3,
    ): Double? {
        val state = level.getBlockState(chest)
        if (state.block !is ChestBlock || !state.hasProperty(ChestBlock.FACING)) return null

        val facing = state.getValue(ChestBlock.FACING)
        val front = Vec3.atCenterOf(chest).add(
            facing.stepX * 0.5,
            0.0,
            facing.stepZ * 0.5,
        )
        val dx = abs(point.x - front.x)
        val dy = abs(point.y - front.y)
        val dz = abs(point.z - front.z)
        val normalDistance = if (facing.axis == net.minecraft.core.Direction.Axis.X) dx else dz
        val lateralDistance = if (facing.axis == net.minecraft.core.Direction.Axis.X) dz else dx

        if (normalDistance > 0.8 || lateralDistance > 0.9 || dy > 0.9) return null
        return normalDistance * 2.0 + lateralDistance + dy
    }

    private fun aimAtParticle(client: Minecraft, point: Vec3) {
        val player = client.player ?: return
        val rotation = SkyBlockUtils.rotationTo(player, point)
        SkyBlockUtils.preparePerspectiveRotation()
        RotationManager.setTargetRotation(rotation.first, rotation.second, ROTATION_OWNER)
        if (smoothRotations.value) RotationManager.quickTick(rotationSpeed.value)
        else RotationManager.snapToTarget()
    }

    private fun clear() {
        solved.clear()
        closestChest = null
        particle = null
        RotationManager.clearRotation(ROTATION_OWNER)
    }
}
