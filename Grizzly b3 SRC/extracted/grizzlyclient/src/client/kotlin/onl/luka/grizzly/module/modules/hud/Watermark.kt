package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.gui.ui.HudUiResources
import onl.luka.grizzly.gui.ui.MinecraftUiRenderer
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.gui.ui.UiRuntime
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.modules.hud.watermark.CamelWatermark
import onl.luka.grizzly.module.modules.hud.watermark.SimpleWatermark
import onl.luka.grizzly.module.modules.hud.watermark.WatermarkContext
import onl.luka.grizzly.module.modules.hud.watermark.WatermarkBackground
import onl.luka.grizzly.module.modules.hud.watermark.WatermarkDesign
import onl.luka.grizzly.module.modules.hud.watermark.WurstWatermark
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object Watermark : HudModule(
    "Watermark",
    "Shows the client name in the corner",
    defaultX = 0f,
    defaultY = 0.004f,
) {

    enum class Design { SIMPLE, CAMEL, WURST }
    enum class VersionStyle { GRIZZLY, LEGACY }

    private val design = enum("design", Design.SIMPLE)
    private val versionStyle = enum("version", VersionStyle.GRIZZLY).also {
        it.visibleWhen = { impl().usesVersionStyle }
    }
    private val accentColor = color("accent color", Color(120, 200, 255, 255), allowAlpha = true).also {
        it.pickerMode = ColorEntry.PickerMode.THEME
        it.visibleWhen = { impl().usesAccentColor }
    }
    private val textColor = color("text color", Color(255, 255, 255, 255), allowAlpha = true).also {
        it.visibleWhen = { impl().usesTextColor }
    }

    private val background = boolean("background", false).also {
        it.visibleWhen = { impl().backgroundStyle == WatermarkBackground.OPTIONAL }
    }
    private val bgColor = color("bg color", Color(0, 0, 0, 120), allowAlpha = true).also {
        it.visibleWhen = { impl().backgroundStyle == WatermarkBackground.OPTIONAL && background.value }
    }
    private val backgroundMode = enum("background mode", HudBackgroundMode.SOLID).also {
        it.visibleWhen = { panelEffectsAvailable() }
    }
    private val blurStrength = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { panelEffectsAvailable() && backgroundMode.value == HudBackgroundMode.BLUR }
    }
    private val panelShadow = hudDropShadow { panelEffectsAvailable() }
    private val textShadow = boolean("text shadow", true)

    private fun panelEffectsAvailable(): Boolean = impl().panelEffects && backgroundActive()

    private fun backgroundActive(): Boolean = when (impl().backgroundStyle) {
        WatermarkBackground.NONE -> false
        WatermarkBackground.ALWAYS -> true
        WatermarkBackground.OPTIONAL -> background.value
    }

    init {
        enable()
    }

    private fun impl(): WatermarkDesign = when (design.value) {
        Design.SIMPLE -> SimpleWatermark
        Design.CAMEL -> CamelWatermark
        Design.WURST -> WurstWatermark
    }

    private fun modVersion(id: String): String =
        FabricLoader.getInstance()
            .getModContainer(id)
            .map { it.metadata.version.friendlyString }
            .orElse("?")

    private fun context(): WatermarkContext {
        val theme = Colour.accent.liveColor(Colour.accent.value)
        val time = ColorEntry.chromaTimeSeconds()
        return WatermarkContext(
            templates = HudUiResources.templates(),
            accent = accentColor.liveColor(theme, time).argb,
            text = textColor.liveColor(theme, time).argb,
            background = bgColor.liveColor(bgColor.value, time).argb,
            showBackground = backgroundActive(),
            blurBackground = panelEffectsAvailable() && backgroundMode.value == HudBackgroundMode.BLUR,
            textShadow = textShadow.value,
            name = "Grizzly",
            version = modVersion("medved"),
            mcVersion = modVersion("minecraft"),
            versionText = versionText(),
        )
    }

    private fun versionText(): String = when (versionStyle.value) {
        VersionStyle.GRIZZLY -> "b${modVersion("medved")} MC${modVersion("minecraft")}"
        VersionStyle.LEGACY -> LEGACY_VERSION
    }

    override fun hudWidth(): Int = impl().width(context())

    override fun hudHeight(): Int = impl().height(context())

    override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        withFont { drawHud(extractor, delta) }
    }

    private fun drawHud(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        val mc = Minecraft.getInstance()
        val px = hudPixelX(mc.window.guiScaledWidth)
        val py = (hudY.value * mc.window.guiScaledHeight).toInt()
        val scale = hudScale.value

        extractor.pose().pushMatrix()
        extractor.pose().translate(px.toFloat(), py.toFloat())
        if (scale != 1.0f) extractor.pose().scale(scale, scale)
        Font.withRenderScale(scale) { renderHudElement(extractor) }
        extractor.pose().popMatrix()
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        val ctx = context()
        val impl = impl()
        val document = impl.document(ctx)
        val runtime = UiRuntime(MinecraftUiRenderer(g)) { node -> impl.renderNode(g, ctx, node) }
        val width = impl.width(ctx)
        val height = impl.height(ctx)

        if (panelEffectsAvailable()) {
            panelShadow.draw(g, 0, 0, width, height, PANEL_RADIUS)
            if (ctx.blurBackground) {
                HudBlur.draw(g, 0, 0, width, height, PANEL_RADIUS, ctx.background, blurStrength.value)
            }
        }

        runtime.layout(document, UiRect(0f, 0f, width.toFloat(), height.toFloat()))
        runtime.render(document, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    }

    override fun hudInfo(): String = design.value.name.lowercase()

    private const val PANEL_RADIUS = 3

    private const val LEGACY_VERSION = "v6.35.3 MC1.8"
}
