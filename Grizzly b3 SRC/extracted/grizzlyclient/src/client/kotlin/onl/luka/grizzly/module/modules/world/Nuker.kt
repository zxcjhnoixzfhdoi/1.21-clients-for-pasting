package onl.luka.grizzly.module.modules.world

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.skyblock.ClickLock
import onl.luka.grizzly.module.modules.skyblock.PowderChestSolver
import onl.luka.grizzly.module.modules.skyblock.SkyBlockBox
import onl.luka.grizzly.module.modules.skyblock.SkyBlockUtils
import onl.luka.grizzly.module.modules.skyblock.renderSkyBlockBoxes
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil

object Nuker : Module(
    "Nuker",
    "Automatically mines selected blocks using Medved rotations",
    Category.WORLD,
) {
    enum class Rotations { NONE, CLIENT }
    enum class Priority { CLOSEST, LOWEST_ROTATION }
    enum class Shape { SPHERE, FACING_AXIS, AXIS_TUNNELS, TRIGGERBOT }

    private const val ROTATION_OWNER = "skyblock-nuker"

    private val blocks = itemList(
        "blocks",
        listOf("stone", "cobblestone", "clay", "red_sandstone", "red_sand"),
        ItemListEntry.Mode.WHITELIST,
        ItemListEntry.Filter.BLOCKS_ONLY,
    )
    private val range = double("range", 4.5, 1.0, 6.0)
    private val height = int("height", 0, 0, 5)
    private val depth = int("depth", 1, 0, 5)
    private val fov = float("fov", 180f, 10f, 360f)
    private val shape = enum("shape", Shape.SPHERE)
    private val sidewaysOffset = int("sideways offset", 0, -4, 4)
    private val forwardsOffset = int("forwards backwards offset", 0, -4, 4)
    private val rotations = enum("rotations", Rotations.CLIENT)
    private val priority = enum("priority", Priority.LOWEST_ROTATION)
    private val stuckTimeout = int("stuck timeout ticks", 80, 20, 300)
    private val preview = boolean("preview", true)
    private val targetColor = color("target color", Color(255, 70, 70, 200))

    private var target: BlockPos? = null
    private var targetFace = Direction.UP
    private var targetTicks = 0
    private var forcingAttack = false

    override fun onDisabled() {
        stop(Minecraft.getInstance())
    }

    override fun onTick(client: Minecraft) {
        val level = client.level ?: return stop(client)
        val player = client.player ?: return stop(client)
        if (PowderChestSolver.isSolving()) {
            pauseForPowderChest(client)
            return
        }

        setAttackHeld(client, true)
        val current = target
        if (current == null ||
            level.getBlockState(current).isAir ||
            !SkyBlockUtils.matchesBlockList(level.getBlockState(current), blocks) ||
            player.eyePosition.distanceTo(Vec3.atCenterOf(current)) > range.value + 0.75 ||
            targetTicks >= stuckTimeout.value
        ) {
            target = null
            targetTicks = 0
            RotationManager.clearRotation(ROTATION_OWNER)
        }

        if (target == null) {
            target = findTarget(client)
            targetFace = target?.let { visibleFace(client, it) } ?: Direction.UP
        }

        val pos = target ?: run {
            RotationManager.clearRotation(ROTATION_OWNER)
            return
        }
        val rotation = SkyBlockUtils.rotationTo(player, Vec3.atCenterOf(pos))
        when (rotations.value) {
            Rotations.NONE -> RotationManager.clearRotation(ROTATION_OWNER)
            Rotations.CLIENT -> {
                SkyBlockUtils.preparePerspectiveRotation()
                RotationManager.setTargetRotation(rotation.first, rotation.second, ROTATION_OWNER)
                RotationManager.tick()
            }
        }

        targetTicks++
    }

    @JvmStatic
    fun overrideVanillaHitResult(): HitResult? {
        if (!isEnabled() || !forcingAttack) return null
        val client = Minecraft.getInstance()
        val level = client.level ?: return null
        val pos = target ?: return null
        if (level.getBlockState(pos).isAir) return null
        if (rotations.value == Rotations.CLIENT &&
            (!RotationManager.ownsRotation(ROTATION_OWNER) || !RotationManager.hasReachedTarget(3f))
        ) {
            return null
        }

        val point = Vec3.atCenterOf(pos).add(
            targetFace.stepX * 0.5,
            targetFace.stepY * 0.5,
            targetFace.stepZ * 0.5,
        )
        return BlockHitResult(point, targetFace, pos, false)
    }

    @JvmStatic
    fun shouldSuppressVanillaAttack(): Boolean {
        if (!isEnabled() || !forcingAttack) return false
        if (target == null) return true
        return rotations.value == Rotations.CLIENT &&
            (!RotationManager.ownsRotation(ROTATION_OWNER) || !RotationManager.hasReachedTarget(3f))
    }

    @JvmStatic
    fun pauseForPowderChest() {
        pauseForPowderChest(Minecraft.getInstance())
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        if (!preview.value) return
        target?.let {
            renderSkyBlockBoxes(ctx, listOf(SkyBlockBox(AABB(it).deflate(0.004), targetColor.value)), 2f)
        }
    }

    private fun findTarget(client: Minecraft): BlockPos? {
        val level = client.level ?: return null
        val player = client.player ?: return null
        val eye = player.eyePosition
        val center = player.blockPosition().above()
        val horizontal = ceil(range.value).toInt()
        return sequence {
            for (x in -horizontal..horizontal) {
                for (y in -depth.value..height.value) {
                    for (z in -horizontal..horizontal) {
                        yield(center.offset(x, y, z))
                    }
                }
            }
        }
            .filter { SkyBlockUtils.matchesBlockList(level.getBlockState(it), blocks) }
            .filter { eye.distanceTo(Vec3.atCenterOf(it)) <= range.value }
            .filter { angleFromCrosshair(player.yRot, player.xRot, player, it) <= fov.value / 2f }
            .filter { matchesShape(player, it) }
            .filter { visibleFace(client, it) != null }
            .minByOrNull {
                when (priority.value) {
                    Priority.CLOSEST -> eye.distanceToSqr(Vec3.atCenterOf(it))
                    Priority.LOWEST_ROTATION -> angleFromCrosshair(player.yRot, player.xRot, player, it).toDouble()
                }
            }
    }

    private fun matchesShape(player: net.minecraft.client.player.LocalPlayer, pos: BlockPos): Boolean =
        when (shape.value) {
            Shape.SPHERE -> true
            Shape.FACING_AXIS -> axisOffsets(player, pos).let { (forward, side) ->
                forward in 0..ceil(range.value).toInt() && side == 0
            }
            Shape.AXIS_TUNNELS -> axisOffsets(player, pos).let { (forward, side) ->
                forward in 0..ceil(range.value).toInt() && (side == 0 || abs(side) == 2)
            }
            Shape.TRIGGERBOT -> {
                val eye = player.eyePosition
                AABB(pos).clip(eye, eye.add(player.lookAngle.scale(range.value + 0.75))).isPresent
            }
        }

    private fun axisOffsets(
        player: net.minecraft.client.player.LocalPlayer,
        pos: BlockPos,
    ): Pair<Int, Int> {
        val forward = player.direction
        val right = forward.clockWise
        val origin = player.blockPosition()
            .offset(
                forward.stepX * forwardsOffset.value + right.stepX * sidewaysOffset.value,
                0,
                forward.stepZ * forwardsOffset.value + right.stepZ * sidewaysOffset.value,
            )
        val dx = pos.x - origin.x
        val dz = pos.z - origin.z
        return dx * forward.stepX + dz * forward.stepZ to
            dx * right.stepX + dz * right.stepZ
    }

    private fun angleFromCrosshair(yaw: Float, pitch: Float, player: net.minecraft.client.player.LocalPlayer, pos: BlockPos): Float {
        val targetRotation = SkyBlockUtils.rotationTo(player, Vec3.atCenterOf(pos))
        return abs(Mth.wrapDegrees(targetRotation.first - yaw)) + abs(targetRotation.second - pitch)
    }

    private fun visibleFace(client: Minecraft, pos: BlockPos): Direction? {
        val level = client.level ?: return null
        val player = client.player ?: return null
        val hit = level.clip(
            ClipContext(
                player.eyePosition,
                Vec3.atCenterOf(pos),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player,
            ),
        )
        return hit.direction.takeIf { hit.blockPos == pos }
    }

    private fun stop(client: Minecraft) {
        setAttackHeld(client, false)
        target = null
        targetTicks = 0
        RotationManager.clearRotation(ROTATION_OWNER)
    }

    private fun pauseForPowderChest(client: Minecraft) {
        setAttackHeld(client, false)
        target = null
        targetTicks = 0
        RotationManager.clearRotation(ROTATION_OWNER)
    }

    private fun setAttackHeld(client: Minecraft, held: Boolean) {
        if (held) {
            forcingAttack = true
            client.options.keyAttack.setDown(true)
            return
        }
        if (!forcingAttack) return

        forcingAttack = false
        val physicalAttack = InputUtil.isPhysicalKeyDown(client.options.keyAttack)
        client.options.keyAttack.setDown(physicalAttack || ClickLock.wantsAttack(client))
    }
}
