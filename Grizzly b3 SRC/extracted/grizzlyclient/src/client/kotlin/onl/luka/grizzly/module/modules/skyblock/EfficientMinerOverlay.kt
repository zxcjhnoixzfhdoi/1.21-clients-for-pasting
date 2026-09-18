package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.module.Module
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.abs

object EfficientMinerOverlay : Module(
    "Efficient Miner Overlay",
    "Shows the Mining Spread pattern for the block under the crosshair",
    Category.SKYBLOCK,
) {
    private val blocks = itemList(
        "blocks",
        listOf("clay", "red_sandstone"),
        ItemListEntry.Mode.WHITELIST,
        ItemListEntry.Filter.BLOCKS_ONLY,
    )
    private val spreadBlocks = int("spread blocks", 3, 1, 12)
    private val showBedrockRisk = boolean("show bedrock risk", true)
    private val targetColor = color("target color", Color(80, 190, 255, 210))
    private val spreadColor = color("spread color", Color(255, 205, 55, 190))
    private val bedrockColor = color("bedrock risk color", Color(255, 45, 45, 225))

    private val boxes = mutableListOf<SkyBlockBox>()

    override fun onDisabled() {
        boxes.clear()
    }

    override fun onTick(client: Minecraft) {
        val level = client.level ?: return boxes.clear()
        val hit = client.hitResult as? BlockHitResult
        if (hit == null || hit.type != HitResult.Type.BLOCK) {
            boxes.clear()
            return
        }

        val target = hit.blockPos
        if (!SkyBlockUtils.matchesBlockList(level.getBlockState(target), blocks)) {
            boxes.clear()
            return
        }

        val adjacent = adjacentPositions(target, hit.direction)
        val predicted = adjacent
            .filter { SkyBlockUtils.matchesBlockList(level.getBlockState(it), blocks) }
            .take(spreadBlocks.value)

        val next = mutableListOf(
            SkyBlockBox(AABB(target).deflate(0.004), targetColor.value),
        )
        predicted.forEach {
            next += SkyBlockBox(AABB(it).deflate(0.008), spreadColor.value)
        }
        if (showBedrockRisk.value) {
            adjacent
                .filter { level.getBlockState(it).`is`(Blocks.BEDROCK) }
                .forEach { next += SkyBlockBox(AABB(it).deflate(0.002), bedrockColor.value) }
        }

        boxes.clear()
        boxes += next
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        renderSkyBlockBoxes(ctx, boxes, 1.5f)
    }

    private fun adjacentPositions(target: BlockPos, face: Direction): List<BlockPos> =
        buildList {
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (x != 0 || y != 0 || z != 0) {
                            add(Offset(x, y, z))
                        }
                    }
                }
            }
        }
            .sortedWith(
                compareBy<Offset> { it.depthFor(face) }
                    .thenBy { it.manhattanDistance }
                    .thenBy { it.y }
                    .thenBy { it.x }
                    .thenBy { it.z },
            )
            .map { target.offset(it.x, it.y, it.z) }

    private data class Offset(val x: Int, val y: Int, val z: Int) {
        val manhattanDistance: Int = abs(x) + abs(y) + abs(z)

        fun depthFor(face: Direction): Int =
            when (face.axis) {
                Direction.Axis.X -> abs(x)
                Direction.Axis.Y -> abs(y)
                Direction.Axis.Z -> abs(z)
            }
    }
}
