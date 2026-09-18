package onl.luka.grizzly.module.modules.hud.watermark

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor

object CamelWatermark : WatermarkDesign {

    override val backgroundStyle = WatermarkBackground.NONE

    private const val NAME_SCALE = 1.6f
    private const val BUILD_SCALE = 0.8f
    private const val PAD_X = 4
    private const val PAD_Y = 3
    private const val GAP = 2
    private const val NAME_H = 14
    private const val OFFSET = 1

    private fun buildText(ctx: WatermarkContext) = "b${ctx.version}"

    override fun width(ctx: WatermarkContext): Int =
        PAD_X * 2 + ctx.textWidth(ctx.name, NAME_SCALE) + GAP +
            ctx.textWidth(buildText(ctx), BUILD_SCALE) + OFFSET

    override fun height(ctx: WatermarkContext): Int = PAD_Y * 2 + NAME_H + OFFSET

    override fun document(ctx: WatermarkContext): UiDocument {
        val nameWidth = ctx.textWidth(ctx.name, NAME_SCALE)
        val build = buildText(ctx)

        return UiDocument(
            ctx.templates.instantiate(
                "watermark-camel",
                mapOf(
                    "hud.width" to width(ctx).toString(),
                    "hud.height" to height(ctx).toString(),
                    "mark.name" to ctx.name,
                    "mark.nameX" to PAD_X.toString(),
                    "mark.nameY" to PAD_Y.toString(),
                    "mark.nameWidth" to nameWidth.toString(),
                    "mark.nameShadowX" to (PAD_X + OFFSET).toString(),
                    "mark.nameShadowY" to (PAD_Y + OFFSET).toString(),
                    "mark.nameColor" to ctx.accent.xmlColor(),
                    "mark.nameScale" to NAME_SCALE.toString(),
                    "mark.build" to build,
                    "mark.buildX" to (PAD_X + nameWidth + GAP).toString(),
                    "mark.buildY" to (PAD_Y + NAME_H - 7).toString(),
                    "mark.buildWidth" to ctx.textWidth(build, BUILD_SCALE).toString(),
                    "mark.buildShadowX" to (PAD_X + nameWidth + GAP + OFFSET).toString(),
                    "mark.buildShadowY" to (PAD_Y + NAME_H - 7 + OFFSET).toString(),
                    "mark.buildColor" to ctx.text.xmlColor(),
                    "mark.buildScale" to BUILD_SCALE.toString(),
                ),
            ),
        )
    }
}
