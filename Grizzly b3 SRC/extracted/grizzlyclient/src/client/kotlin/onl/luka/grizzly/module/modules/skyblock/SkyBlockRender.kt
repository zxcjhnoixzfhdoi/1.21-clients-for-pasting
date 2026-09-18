package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.util.RenderUtil
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.world.phys.AABB

internal data class SkyBlockBox(
    val bounds: AABB,
    val color: Color,
    val fillAlpha: Float = color.a / 255f * 0.2f,
    val outlineAlpha: Float = color.a / 255f,
)

internal fun renderSkyBlockBoxes(
    ctx: LevelRenderContext,
    boxes: Iterable<SkyBlockBox>,
    lineWidth: Float = 1.5f,
) {
    val materialized = boxes.toList()
    if (materialized.isEmpty()) return

    RenderUtil.worldContext(ctx) { _, sink ->
        sink.draw(RenderUtil.ESP_FILLED) { pose, consumer ->
            materialized.forEach { box ->
                val color = box.color
                RenderUtil.boxFilledBothSides(
                    consumer,
                    pose,
                    box.bounds,
                    color.r / 255f,
                    color.g / 255f,
                    color.b / 255f,
                    box.fillAlpha,
                )
            }
        }
        sink.draw(RenderUtil.ESP_LINES) { pose, consumer ->
            materialized.forEach { box ->
                val color = box.color
                RenderUtil.boxOutline(
                    consumer,
                    pose,
                    box.bounds,
                    color.r / 255f,
                    color.g / 255f,
                    color.b / 255f,
                    box.outlineAlpha,
                    lineWidth,
                )
            }
        }
    }
}
