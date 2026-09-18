package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB

object FrozenTreasuresESP : Module(
    "Frozen Treasures ESP",
    "Highlights frozen treasure drops on the Glacial Cave floor",
    Category.SKYBLOCK,
) {
    private val color = color("color", Color(85, 210, 255, 220))
    private val boxes = mutableListOf<SkyBlockBox>()
    private var ticks = 0

    private val treasureNames = setOf(
        "Packed Ice",
        "Enchanted Ice",
        "Enchanted Packed Ice",
        "Ice Bait",
        "Glowy Chum Bait",
        "Glacial Fragment",
        "White Gift",
        "Green Gift",
        "Red Gift",
        "Glacial Talisman",
    )

    override fun onDisabled() {
        boxes.clear()
    }

    override fun onTick(client: Minecraft) {
        if (++ticks % 8 != 0) return
        val level = client.level ?: return boxes.clear()
        val markerColor = color.value
        val next = level.entitiesForRendering()
            .filterIsInstance<ArmorStand>()
            .filter { stand ->
                val headName = stand.getItemBySlot(EquipmentSlot.HEAD).hoverName.string
                treasureNames.any { headName.contains(it, ignoreCase = true) }
            }
            .map { BlockPos.containing(it.x, it.y + TREASURE_BLOCK_Y_OFFSET, it.z) }
            .distinct()
            .map {
                SkyBlockBox(
                    bounds = AABB(it).inflate(BLOCK_BOX_EXPANSION),
                    color = markerColor,
                    fillAlpha = markerColor.a / 255f * 0.35f,
                )
            }
            .toList()
        boxes.clear()
        boxes += next
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        renderSkyBlockBoxes(ctx, boxes, lineWidth = 2f)
    }

    private const val TREASURE_BLOCK_Y_OFFSET = 2.0
    private const val BLOCK_BOX_EXPANSION = 0.003
}
