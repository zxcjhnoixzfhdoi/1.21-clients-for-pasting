package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID

object CorpseFinder : Module(
    "Corpse Finder",
    "Highlights corpses and Littlefoot in Glacite Mineshafts",
    Category.SKYBLOCK,
) {
    private val onlyInMineshaft = boolean("only in mineshafts", true)
    private val showLapis = boolean("lapis corpses", true)
    private val showTungsten = boolean("tungsten corpses", true)
    private val showUmber = boolean("umber corpses", true)
    private val showVanguard = boolean("vanguard corpses", true)
    private val showLittlefoot = boolean("littlefoot", true)
    private val announce = boolean("announce discoveries", true)

    private val lapisColor = color("lapis color", Color(60, 115, 255, 210))
    private val tungstenColor = color("tungsten color", Color(170, 180, 190, 210))
    private val umberColor = color("umber color", Color(215, 120, 50, 210))
    private val vanguardColor = color("vanguard color", Color(180, 70, 255, 220))
    private val littlefootColor = color("littlefoot color", Color(255, 215, 80, 230))

    private val boxes = mutableListOf<SkyBlockBox>()
    private val announced = mutableSetOf<UUID>()
    private var ticks = 0

    override fun onDisabled() {
        boxes.clear()
        announced.clear()
    }

    override fun onTick(client: Minecraft) {
        if (++ticks % 10 != 0) return
        val level = client.level ?: return boxes.clear()
        if (onlyInMineshaft.value && !SkyBlockUtils.isInMineshaft(client)) {
            boxes.clear()
            return
        }

        val next = mutableListOf<SkyBlockBox>()
        for (entity in level.entitiesForRendering()) {
            val result = when {
                entity is ArmorStand -> corpseColor(entity)?.let { it to "corpse" }
                showLittlefoot.value && entity.displayName.string.contains("Littlefoot", ignoreCase = true) ->
                    littlefootColor.value to "Littlefoot"
                else -> null
            } ?: continue

            next += SkyBlockBox(entity.boundingBox.inflate(0.08), result.first)
            if (announce.value && announced.add(entity.uuid)) {
                client.player?.sendSystemMessage(
                    Component.literal("Found ${result.second} at ${entity.blockX}, ${entity.blockY}, ${entity.blockZ}"),
                )
            }
        }

        boxes.clear()
        boxes += next
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        renderSkyBlockBoxes(ctx, boxes)
    }

    private fun corpseColor(stand: ArmorStand): Color? =
        when (SkyBlockUtils.skyBlockId(stand.getItemBySlot(EquipmentSlot.HEAD))) {
            "LAPIS_ARMOR_HELMET" -> lapisColor.value.takeIf { showLapis.value }
            "MINERAL_HELMET" -> tungstenColor.value.takeIf { showTungsten.value }
            "ARMOR_OF_YOG_HELMET", "YOG_HELMET" -> umberColor.value.takeIf { showUmber.value }
            "VANGUARD_HELMET" -> vanguardColor.value.takeIf { showVanguard.value }
            else -> null
        }
}
