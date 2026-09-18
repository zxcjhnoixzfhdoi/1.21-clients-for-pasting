package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

object MineshaftOreGuide : Module(
    "Mineshaft Ore Guide",
    "Color-codes the higher-yield Umber and Tungsten blocks",
    Category.SKYBLOCK,
) {
    private val range = int("range", 16, 4, 32)
    private val verticalRange = int("vertical range", 10, 2, 24)
    private val onlyInMineshaft = boolean("only in mineshafts", true)
    private val showLowYield = boolean("show low yield", false)

    private val tungstenBest = color("tungsten clay", Color(85, 210, 255, 190))
    private val tungstenLow = color("tungsten cobblestone", Color(110, 110, 110, 120))
    private val umberBest = color("umber red sand", Color(255, 95, 45, 190))
    private val umberMedium = color("umber dark terracotta", Color(215, 135, 55, 175))
    private val umberLow = color("umber orange terracotta", Color(225, 165, 70, 120))

    private val boxes = mutableListOf<SkyBlockBox>()
    private var ticks = 0

    override fun onDisabled() {
        boxes.clear()
    }

    override fun onTick(client: Minecraft) {
        if (++ticks % 12 != 0) return
        val level = client.level ?: return boxes.clear()
        val player = client.player ?: return boxes.clear()
        if (onlyInMineshaft.value && !SkyBlockUtils.isInMineshaft(client)) {
            boxes.clear()
            return
        }

        val next = mutableListOf<SkyBlockBox>()
        for (pos in SkyBlockUtils.nearbyBlocks(player.blockPosition(), range.value, verticalRange.value)) {
            val state = level.getBlockState(pos)
            val blockColor = when (SkyBlockUtils.blockId(state)) {
                "clay" -> tungstenBest.value
                "cobblestone" -> tungstenLow.value.takeIf { showLowYield.value }
                "red_sand" -> umberBest.value
                "brown_terracotta", "red_terracotta" -> umberMedium.value
                "orange_terracotta" -> umberLow.value.takeIf { showLowYield.value }
                else -> null
            } ?: continue
            next += SkyBlockBox(blockBounds(pos), blockColor)
        }
        boxes.clear()
        boxes += next
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        renderSkyBlockBoxes(ctx, boxes, 1f)
    }

    private fun blockBounds(pos: BlockPos) =
        net.minecraft.world.phys.AABB(pos).deflate(0.003)
}
