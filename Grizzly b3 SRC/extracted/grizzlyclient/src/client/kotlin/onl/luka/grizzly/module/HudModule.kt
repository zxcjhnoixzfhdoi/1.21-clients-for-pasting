package onl.luka.grizzly.module

import onl.luka.grizzly.config.entry.HudEditEntry
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class HudModule(
    name: String,
    description: String,
    category: Module.Category = Module.Category.HUD,
    defaultX: Float = 0.0f,
    defaultY: Float = 0.0f,
) : Module(name, description, category) {

    val editButton = register(HudEditEntry())
    val hudX = float("hud_x", defaultX, 0.0f, 1.0f).also { it.visibleWhen = { false } }
    val hudY = float("hud_y", defaultY, 0.0f, 1.0f).also { it.visibleWhen = { false } }
    val hudScale = float("hud_scale", 1.0f, 0.25f, 4.0f).also { it.visibleWhen = { false } }
    val fontOverride = registerFontOverride()

    abstract fun renderHudElement(g: GuiGraphicsExtractor)
    abstract fun hudWidth(): Int
    abstract fun hudHeight(): Int

    // Measuring outside renderHudElement has to run under the same font the element draws with,
    // or the editor handles and the layout that pushes elements apart use the wrong widths.
    fun measuredWidth(): Int = withFont { hudWidth() }

    fun measuredHeight(): Int = withFont { hudHeight() }

    open fun hudPixelX(screenW: Int): Int = (hudX.value * screenW).toInt()

    open fun hudXFromLeftPixel(leftPx: Int, screenW: Int): Float = leftPx.toFloat() / screenW

    override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        val mc = Minecraft.getInstance()
        val px = (hudX.value * mc.window.guiScaledWidth).toInt()
        val py = (hudY.value * mc.window.guiScaledHeight).toInt()
        val sc = hudScale.value
        extractor.pose().pushMatrix()
        extractor.pose().translate(px.toFloat(), py.toFloat())
        if (sc != 1.0f) extractor.pose().scale(sc, sc)
        Font.withRenderScale(sc) {
            withFont { renderHudElement(extractor) }
        }
        extractor.pose().popMatrix()
    }
}
