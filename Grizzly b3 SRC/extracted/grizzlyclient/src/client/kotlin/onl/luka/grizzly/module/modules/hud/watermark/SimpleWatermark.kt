package onl.luka.grizzly.module.modules.hud.watermark

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor

object SimpleWatermark : WatermarkDesign {

    override val panelEffects = true

    private const val PAD_X = 3
    private const val PAD_Y = 2
    private const val LINE_H = 9

    private fun head(ctx: WatermarkContext) = ctx.name.take(1).uppercase()

    private fun tail(ctx: WatermarkContext) = ctx.name.drop(1)

    override fun width(ctx: WatermarkContext): Int =
        PAD_X * 2 + ctx.textWidth(head(ctx)) + ctx.textWidth(tail(ctx))

    override fun height(ctx: WatermarkContext): Int = PAD_Y * 2 + LINE_H

    override fun document(ctx: WatermarkContext): UiDocument {
        val headText = head(ctx)
        val headWidth = ctx.textWidth(headText)

        return UiDocument(
            ctx.templates.instantiate(
                "watermark-simple",
                mapOf(
                    "hud.width" to width(ctx).toString(),
                    "hud.height" to height(ctx).toString(),
                    "hud.background" to ctx.panelBackground().xmlColor(),
                    "hud.shadow" to ctx.shadow,
                    "mark.head" to headText,
                    "mark.headWidth" to headWidth.toString(),
                    "mark.headColor" to ctx.accent.xmlColor(),
                    "mark.tail" to tail(ctx),
                    "mark.tailX" to (PAD_X + headWidth).toString(),
                    "mark.tailWidth" to ctx.textWidth(tail(ctx)).toString(),
                    "mark.tailColor" to ctx.text.xmlColor(),
                    "pad.x" to PAD_X.toString(),
                    "pad.y" to PAD_Y.toString(),
                ),
            ),
        )
    }
}
