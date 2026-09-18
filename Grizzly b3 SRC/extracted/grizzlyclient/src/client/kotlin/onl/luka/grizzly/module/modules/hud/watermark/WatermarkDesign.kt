package onl.luka.grizzly.module.modules.hud.watermark

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiNode
import onl.luka.grizzly.gui.ui.UiTemplateSet
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

enum class WatermarkBackground { OPTIONAL, NONE, ALWAYS }

class WatermarkContext(
    val templates: UiTemplateSet,
    val accent: Int,
    val text: Int,
    val background: Int,
    val showBackground: Boolean,
    val blurBackground: Boolean,
    val textShadow: Boolean,
    val name: String,
    val version: String,
    val mcVersion: String,
    val versionText: String,
) {
    val shadow: String get() = textShadow.toString()

    fun textWidth(value: String, scale: Float = 1f): Int =
        (Font.getFont().width(Font.styledText(value)) * scale).roundToInt()

    fun panelBackground(): Int = if (showBackground && !blurBackground) background else 0
}

interface WatermarkDesign {
    fun width(ctx: WatermarkContext): Int

    fun height(ctx: WatermarkContext): Int

    fun document(ctx: WatermarkContext): UiDocument

    val backgroundStyle: WatermarkBackground get() = WatermarkBackground.OPTIONAL

    val panelEffects: Boolean get() = false

    val usesTextColor: Boolean get() = true

    val usesAccentColor: Boolean get() = true

    val usesVersionStyle: Boolean get() = false

    fun renderNode(g: GuiGraphicsExtractor, ctx: WatermarkContext, node: UiNode): Boolean = false
}
