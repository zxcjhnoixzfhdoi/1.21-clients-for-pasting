package onl.luka.grizzly.module.modules.hud.watermark

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiNode
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

object WurstWatermark : WatermarkDesign {

    private const val LOGO_W = 72
    private const val LOGO_H = 18
    private const val LOGO_Y = 3
    private const val BAR_TOP = 6
    private const val BAR_H = 11
    private const val TEXT_X = 74
    private const val TEXT_Y = 8
    private const val TAIL_PAD = 4

    private const val BAR_COLOR = 0x80FFFFFF.toInt()
    private const val TEXT_COLOR = 0xFF000000.toInt()

    private const val TEXTURE_W = 505
    private const val TEXTURE_H = 128
    private val TEXTURE = Identifier.fromNamespaceAndPath("medved", "textures/wurst_128.png")

    override val backgroundStyle = WatermarkBackground.ALWAYS

    override val usesTextColor = false

    override val usesAccentColor = false

    override val usesVersionStyle = true

    override fun width(ctx: WatermarkContext): Int =
        TEXT_X + ctx.textWidth(ctx.versionText) + TAIL_PAD

    override fun height(ctx: WatermarkContext): Int = LOGO_Y + LOGO_H + 2

    override fun document(ctx: WatermarkContext): UiDocument {
        val version = ctx.versionText

        return UiDocument(
            ctx.templates.instantiate(
                "watermark-wurst",
                mapOf(
                    "hud.width" to width(ctx).toString(),
                    "hud.height" to height(ctx).toString(),
                    "hud.shadow" to ctx.shadow,
                    "bar.width" to width(ctx).toString(),
                    "bar.top" to BAR_TOP.toString(),
                    "bar.height" to BAR_H.toString(),
                    "bar.color" to BAR_COLOR.xmlColor(),
                    "logo.y" to LOGO_Y.toString(),
                    "logo.width" to LOGO_W.toString(),
                    "logo.height" to LOGO_H.toString(),
                    "mark.version" to version,
                    "mark.versionX" to TEXT_X.toString(),
                    "mark.versionY" to TEXT_Y.toString(),
                    "mark.versionWidth" to ctx.textWidth(version).toString(),
                    "mark.versionColor" to TEXT_COLOR.xmlColor(),
                ),
            ),
        )
    }

    override fun renderNode(g: GuiGraphicsExtractor, ctx: WatermarkContext, node: UiNode): Boolean {
        if (node.type != "watermark-logo") return false

        g.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            node.bounds.x.roundToInt(),
            node.bounds.y.roundToInt(),
            0f,
            0f,
            node.bounds.width.roundToInt().coerceAtLeast(1),
            node.bounds.height.roundToInt().coerceAtLeast(1),
            TEXTURE_W,
            TEXTURE_H,
            TEXTURE_W,
            TEXTURE_H,
        )
        return true
    }
}
