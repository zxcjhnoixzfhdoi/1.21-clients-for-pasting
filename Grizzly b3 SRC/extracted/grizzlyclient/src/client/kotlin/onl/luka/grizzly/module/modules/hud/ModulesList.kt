package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.module.modules.player.ClientBrand
import onl.luka.grizzly.util.CORNER_BL
import onl.luka.grizzly.util.CORNER_BR
import onl.luka.grizzly.util.CORNER_TL
import onl.luka.grizzly.util.CORNER_TR
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import onl.luka.grizzly.util.Text
import onl.luka.grizzly.util.roundedFill
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.PI
import kotlin.math.sin

object ModulesList : HudModule("Modules List", "Shows all enabled modules", defaultX = 0.996f, defaultY = 0.01f) {

    enum class Sort   { SIZE, ALPHA, ORDER }
    enum class Capitalize { UPPER, LOWER, TITLE }
    enum class ConfigFormat { NONE, BRACKETS }
    enum class Shadow { NONE, SIMPLE, SOFT }
    enum class BorderStyle { LEFT, RIGHT, FULL, DASHED }
    enum class ColorEffect { SOLID, GRADIENT, WAVE, CATEGORIES }

    val showVisuals    = boolean("show visuals", false)
    val border         = boolean("border", false).also { it.aliases("left border") }
    val borderStyle    = enum("border style", BorderStyle.LEFT).also {
        it.visibleWhen = { border.value }
    }
    val borderSize     = int("border size", 1, 1, 5).also {
        it.visibleWhen = { border.value }
    }
    val background     = boolean("background", true)
    val backgroundMode = enum("background mode", HudBackgroundMode.SOLID).also {
        it.visibleWhen = { background.value }
    }
    val blurStrength   = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { background.value && backgroundMode.value == HudBackgroundMode.BLUR }
    }
    val rounded        = boolean("rounded", false).also { it.visibleWhen = { background.value } }
    val roundRadius    = int("round radius", 3, 1, 5).also { it.visibleWhen = { rounded.value && background.value } }
    val bgColor        = color("bg color", Color(0, 0, 0, 0), allowAlpha = true)
    private val panelShadow = hudDropShadow(stepped = true) { background.value }
    val textColor      = color("text color", Color(195, 195, 195, 255), allowAlpha = true).also {
        it.visibleWhen = { colorEffect.value != ColorEffect.CATEGORIES }
    }
    val colorEffect    = enum("color effect", ColorEffect.WAVE)
    val gradientColor  = color("gradient color", Color(95, 95, 95, 255), allowAlpha = true).also {
        it.pickerMode = ColorEntry.PickerMode.CUSTOM
        it.visibleWhen = {
            colorEffect.value == ColorEffect.GRADIENT || colorEffect.value == ColorEffect.WAVE
        }
    }
    val waveSpeed      = float("wave speed", 0.35f, 0.05f, 3.0f).also {
        it.visibleWhen = { colorEffect.value == ColorEffect.WAVE }
    }
    val waveLength     = int("wave length", 6, 2, 20).also {
        it.visibleWhen = { colorEffect.value == ColorEffect.WAVE }
    }
    val configTextColor = color("config text color", Color(255, 255, 255, 255), allowAlpha = true)
        .also { it.visibleWhen = { showConfig.value } }
    val sort           = enum("sort", Sort.SIZE)
    val reverseOrder   = boolean("reverse order", false)
    val capitalize     = enum("capitalize", Capitalize.LOWER)
    val removeSpaces   = boolean("remove spaces", false)
    val shadow         = enum("shadow", Shadow.SOFT)
    val shadowStrength = int("shadow strength", 165, 40, 255).also { it.visibleWhen = { shadow.value != Shadow.NONE } }
    val padX           = int("pad x", 2, 0, 6)
    val padY           = int("pad y", 3, 0, 6)
    val widthSnap      = int("width snap", 1, 0, 16)
    val showConfig     = boolean("show config", true)
    val configFormat   = enum("config format", ConfigFormat.NONE).also {
        it.visibleWhen = { showConfig.value }
    }
    val hiddenModules  = moduleList("hidden modules")

    private const val FONT_H = 8
    private val ROW_H get() = FONT_H + padY.value * 2
    private const val DASH_GAP = 1

    private val ACRONYMS = mapOf(
        "2d" to "2D",
        "3d" to "3D",
        "aac" to "AAC",
        "aio" to "AIO",
        "aps" to "APS",
        "cps" to "CPS",
        "esp" to "ESP",
        "fps" to "FPS",
        "gui" to "GUI",
        "hp" to "HP",
        "hud" to "HUD",
        "hvh" to "HvH",
        "id" to "ID",
        "ip" to "IP",
        "kb" to "KB",
        "ms" to "MS",
        "ncp" to "NCP",
        "pvp" to "PvP",
        "tps" to "TPS",
        "vl" to "VL",
        "xp" to "XP",
    )
    private val ROUNDED_UNSUPPORTED_BORDERS = setOf(BorderStyle.RIGHT, BorderStyle.FULL)

    init {
        borderStyle.optionFilter = { style ->
            !roundedBackgroundActive() || style !in ROUNDED_UNSUPPORTED_BORDERS
        }
        borderStyle.onChange { style ->
            normalizeRoundedBorder(style)
        }
        rounded.onChange { normalizeRoundedBorder(borderStyle.value) }
        background.onChange { normalizeRoundedBorder(borderStyle.value) }
        enable()
        panelShadow.enabled.value = true
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun shadowColor(alpha: Int): Int = argb(alpha.coerceIn(0, 255), 0, 0, 0)

    private fun borderInsets(onRight: Boolean = hudX.value > 0.5f): Pair<Int, Int> {
        if (!border.value) return 0 to 0
        return when (effectiveBorderStyle()) {
            BorderStyle.LEFT, BorderStyle.DASHED -> {
                if (onRight) 0 to borderSize.value else borderSize.value to 0
            }
            BorderStyle.RIGHT -> {
                if (onRight) borderSize.value to 0 else 0 to borderSize.value
            }
            BorderStyle.FULL -> borderSize.value to borderSize.value
        }
    }

    private fun effectiveBorderStyle(): BorderStyle {
        val style = borderStyle.value
        return if (roundedBackgroundActive() && style in ROUNDED_UNSUPPORTED_BORDERS) {
            BorderStyle.LEFT
        } else {
            style
        }
    }

    private fun roundedBackgroundActive(): Boolean = background.value && rounded.value

    private fun normalizeRoundedBorder(style: BorderStyle) {
        if (roundedBackgroundActive() && style in ROUNDED_UNSUPPORTED_BORDERS) {
            borderStyle.value = BorderStyle.LEFT
        }
    }

    private fun rowCorners(index: Int, rowWidths: List<Int>, onRight: Boolean): Int {
        val width = rowWidths[index]
        val roundsTop = index == 0 || width > rowWidths[index - 1]
        val roundsBottom = index == rowWidths.lastIndex || width > rowWidths[index + 1]
        return if (onRight) {
            (if (roundsTop) CORNER_TL else 0) or (if (roundsBottom) CORNER_BL else 0)
        } else {
            (if (roundsTop) CORNER_TR else 0) or (if (roundsBottom) CORNER_BR else 0)
        }
    }

    private fun categoryColor(category: Module.Category): Color = when (category) {
        Module.Category.COMBAT -> Color(255, 138, 138)
        Module.Category.ANARCHY -> Color(255, 172, 128)
        Module.Category.MOVEMENT -> Color(138, 180, 255)
        Module.Category.RENDER -> Color(142, 226, 160)
        Module.Category.PLAYER -> Color(196, 160, 255)
        Module.Category.WORLD -> Color(130, 220, 214)
        Module.Category.UTILITY -> Color(255, 224, 138)
        Module.Category.MINIGAMES -> Color(255, 158, 214)
        Module.Category.SKYBLOCK -> Color(206, 232, 140)
        Module.Category.HUD -> Color(150, 214, 255)
        Module.Category.OTHER -> Color(204, 204, 218)
        Module.Category.EXPLOITS -> Color(226, 150, 240)
    }

    private fun rowColor(mod: Module, index: Int, count: Int, timeSeconds: Float): Int {
        if (colorEffect.value == ColorEffect.CATEGORIES) return categoryColor(mod.category).argb

        val theme = Colour.accent.liveColor(Colour.accent.value, timeSeconds)
        val start = textColor.liveColor(theme, timeSeconds)
        if (colorEffect.value == ColorEffect.SOLID) return start.argb

        val end = gradientColor.liveColor(theme, timeSeconds)
        val amount = when (colorEffect.value) {
            ColorEffect.SOLID, ColorEffect.CATEGORIES -> 0f
            ColorEffect.GRADIENT -> if (count <= 1) 0f else index.toFloat() / (count - 1).toFloat()
            ColorEffect.WAVE -> {
                val rowPhase = index.toDouble() * (2.0 * PI / waveLength.value.toDouble())
                val timePhase = timeSeconds.toDouble() * waveSpeed.value.toDouble() * 2.0 * PI
                ((sin(rowPhase - timePhase) + 1.0) * 0.5).toFloat()
            }
        }
        return lerpColor(start, end, amount).argb
    }

    private fun lerpColor(from: Color, to: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * t).toInt().coerceIn(0, 255)
        return Color(
            channel(from.r, to.r),
            channel(from.g, to.g),
            channel(from.b, to.b),
            channel(from.a, to.a),
        )
    }

    private fun applyCase(text: String) = when (capitalize.value) {
        Capitalize.UPPER -> text.uppercase()
        Capitalize.LOWER -> text.lowercase()
        Capitalize.TITLE -> titleCase(text)
    }

    private fun titleCase(text: String): String =
        text.split(' ').joinToString(" ") { word ->
            ACRONYMS[word.lowercase()] ?: word.lowercase().replaceFirstChar { it.uppercase() }
        }

    /**
     * The one place a module name is turned into what actually gets drawn. Every measurement runs
     * through here too, so widths, backgrounds and the size sort stay in step with the text.
     */
    private fun displayName(mod: Module): String {
        val cased = applyCase(mod.name)
        return if (removeSpaces.value) cased.replace(" ", "") else cased
    }

    private fun displayInfo(mod: Module): String {
        if (!showConfig.value) return ""
        val info = applyCase(mod.hudInfo())
        if (info.isEmpty()) return ""

        return when (configFormat.value) {
            ConfigFormat.NONE -> info
            ConfigFormat.BRACKETS -> "[$info]"
        }
    }

    private fun activeModules(): List<Module> {
        val font = Font.getFont()
        var mods = ModuleManager.getAll()
            .filter { it !== this }
            .filter { it.isEnabled() }
            .filter { it.showInModulesList }
            .filterNot { hiddenModules.contains(it.name) }
            .filter {
                showVisuals.value ||
                (it.category != Module.Category.HUD && it.category != Module.Category.RENDER && it::class != ClientBrand::class)
            }
        mods = when (sort.value) {
            Sort.SIZE         -> mods.sortedByDescending {
                val nameW = font.width(Font.styledText(displayName(it)))
                val info = displayInfo(it)
                val infoW = if (info.isNotEmpty()) 4 + font.width(Font.styledText(info)) else 0
                nameW + infoW
            }
            Sort.ALPHA        -> mods.sortedBy { displayName(it) }
            Sort.ORDER        -> mods
        }
        if (reverseOrder.value) mods = mods.reversed()
        return mods
    }

    override fun hudPixelX(screenW: Int): Int {
        val px = (hudX.value * screenW).toInt()
        return if (hudX.value > 0.5f) px - measuredWidth() else px
    }

    override fun hudXFromLeftPixel(leftPx: Int, screenW: Int): Float =
        if (hudX.value > 0.5f) (leftPx + measuredWidth()).toFloat() / screenW
        else leftPx.toFloat() / screenW

    override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        withFont { drawHud(extractor, delta) }
    }

    private fun drawHud(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        val mc = Minecraft.getInstance()
        val px = hudPixelX(mc.window.guiScaledWidth)
        val py = (hudY.value * mc.window.guiScaledHeight).toInt()
        val sc = hudScale.value
        extractor.pose().pushMatrix()
        extractor.pose().translate(px.toFloat(), py.toFloat())
        if (sc != 1.0f) extractor.pose().scale(sc, sc)
        Font.withRenderScale(sc) {
            renderHudElement(extractor)
        }
        extractor.pose().popMatrix()
    }

    override fun hudWidth(): Int {
        val font = Font.getFont()
        val mods = activeModules()
        if (mods.isEmpty()) return 60
        val (leftInset, rightInset) = borderInsets()
        return mods.maxOf { mod ->
            val nameW = font.width(Font.styledText(displayName(mod)))
            val info = displayInfo(mod)
            val infoW = if (info.isNotEmpty()) 4 + font.width(Font.styledText(info)) else 0
            leftInset + padX.value + nameW + infoW + padX.value + rightInset
        }
    }

    override fun hudHeight(): Int {
        val mods = activeModules()
        return if (mods.isEmpty()) ROW_H else mods.size * ROW_H
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        val mods = activeModules()
        if (mods.isEmpty()) return

        val font = Font.getFont()
        val onRight = hudX.value > 0.5f
        val (leftInset, rightInset) = borderInsets(onRight)
        val moduleConfigTextColor = configTextColor.liveColor(configTextColor.value).argb
        val timeSeconds = ColorEntry.chromaTimeSeconds()

        val rawWidths = mods.map { mod ->
            val nameW = font.width(Font.styledText(displayName(mod)))
            val info = displayInfo(mod)
            val infoW = if (info.isNotEmpty()) 4 + font.width(Font.styledText(info)) else 0
            leftInset + padX.value + nameW + infoW + padX.value + rightInset
        }

        data class Group(val start: Int, val end: Int, val maxW: Int)
        val groups = mutableListOf<Group>()
        var i = 0
        while (i < rawWidths.size) {
            var j = i + 1
            var maxW = rawWidths[i]
            val anchor = rawWidths[i]

            while (j < rawWidths.size && kotlin.math.abs(rawWidths[j] - anchor) <= widthSnap.value) {
                maxW = maxOf(maxW, rawWidths[j])
                j++
            }
            groups.add(Group(i, j, maxW))
            i = j
        }

        val rowWidths = rawWidths.indices.map { idx ->
            groups.first { idx >= it.start && idx < it.end }.maxW
        }
        val totalWidth = rowWidths.maxOrNull() ?: return

        if (background.value) {
            val shadowRadius = if (rounded.value) roundRadius.value else 0
            panelShadow.drawStepped(g, 0, 0, rowWidths, ROW_H, onRight, shadowRadius)
        }

        var ry = 0
        for (idx in mods.indices) {
            val mod = mods[idx]
            val nameComp = Font.styledText(displayName(mod))
            val nameW = font.width(nameComp)

            val info = displayInfo(mod)
            val infoComp = if (info.isNotEmpty()) Font.styledText(info) else null
            val infoW = if (infoComp != null) 4 + font.width(infoComp) else 0

            val rowW = rowWidths[idx]
            val rowX = if (onRight) totalWidth - rowW else 0
            val textY = ry + (ROW_H - FONT_H) / 2
            val moduleTextColor = rowColor(mod, idx, mods.size, timeSeconds)
            val corners = if (background.value && rounded.value) rowCorners(idx, rowWidths, onRight) else 0

            if (onRight) {
                val rightEdge = totalWidth
                if (background.value) {
                    drawRowBackground(g, rowX, ry, rowW, corners)
                }
                drawBorder(g, rowX, ry, rowW, idx, rowWidths, onRight, moduleTextColor)
                if (infoComp != null) {
                    val infoX = rightEdge - rightInset - padX.value - font.width(infoComp)
                    drawText(g, font, infoComp, infoX, textY, moduleConfigTextColor)
                    val nameX = infoX - 4 - nameW
                    drawText(g, font, nameComp, nameX, textY, moduleTextColor)
                } else {
                    val nameX = rightEdge - rightInset - padX.value - nameW
                    drawText(g, font, nameComp, nameX, textY, moduleTextColor)
                }
            } else {
                if (background.value) {
                    drawRowBackground(g, rowX, ry, rowW, corners)
                }
                drawBorder(g, rowX, ry, rowW, idx, rowWidths, onRight, moduleTextColor)
                val nameX = rowX + leftInset + padX.value
                drawText(g, font, nameComp, nameX, textY, moduleTextColor)
                if (infoComp != null) {
                    val infoX = nameX + nameW + 4
                    drawText(g, font, infoComp, infoX, textY, moduleConfigTextColor)
                }
            }

            ry += ROW_H
        }
    }

    private fun drawRowBackground(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        corners: Int,
    ) {
        val color = bgColor.liveColor(bgColor.value).argb
        val radius = if (rounded.value) roundRadius.value else 0
        if (backgroundMode.value == HudBackgroundMode.BLUR) {
            HudBlur.draw(g, x, y, width, ROW_H, radius, color, blurStrength.value, corners)
        } else if (rounded.value) {
            g.roundedFill(x, y, width, ROW_H, radius, color, corners)
        } else {
            g.fill(x, y, x + width, y + ROW_H, color)
        }
    }

    private fun drawBorder(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        index: Int,
        rowWidths: List<Int>,
        onRight: Boolean,
        color: Int,
    ) {
        if (!border.value) return
        val stroke = borderSize.value

        when (effectiveBorderStyle()) {
            BorderStyle.LEFT -> drawSideBorder(g, x, y, width, stroke, color, onRight, false)
            BorderStyle.RIGHT -> drawSideBorder(g, x, y, width, stroke, color, !onRight, false)
            BorderStyle.DASHED -> {
                drawSideBorder(g, x, y, width, stroke, color, onRight, true)
            }
            BorderStyle.FULL -> {
                drawFullBorder(g, x, y, width, index, rowWidths, onRight, stroke, color)
            }
        }
    }

    private fun drawSideBorder(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        stroke: Int,
        color: Int,
        atRight: Boolean,
        dashed: Boolean,
    ) {
        val top = if (dashed) y + DASH_GAP else y
        val bottom = if (dashed) {
            (y + ROW_H - DASH_GAP).coerceAtLeast(top + 1)
        } else {
            y + ROW_H
        }
        val left = if (atRight) x + width - stroke else x
        g.fill(left, top, left + stroke, bottom, color)
    }

    private fun drawFullBorder(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        index: Int,
        rowWidths: List<Int>,
        onRight: Boolean,
        stroke: Int,
        color: Int,
    ) {
        val right = x + width
        g.fill(x, y, x + stroke, y + ROW_H, color)
        g.fill(right - stroke, y, right, y + ROW_H, color)

        val totalWidth = rowWidths.maxOrNull() ?: width
        fun rowX(rowIndex: Int) = if (onRight) totalWidth - rowWidths[rowIndex] else 0

        fun drawExposedEdge(adjacentIndex: Int?, edgeY: Int) {
            if (adjacentIndex == null) {
                g.fill(x, edgeY, right, edgeY + stroke, color)
                return
            }

            val adjacentLeft = rowX(adjacentIndex)
            val adjacentRight = adjacentLeft + rowWidths[adjacentIndex]

            val leftEnd = adjacentLeft.coerceAtMost(right)
            if (x < leftEnd) {
                val to = (leftEnd + stroke).coerceAtMost(right)
                g.fill(x, edgeY, to, edgeY + stroke, color)
            }

            val rightStart = adjacentRight.coerceAtLeast(x)
            if (rightStart < right) {
                val from = (rightStart - stroke).coerceAtLeast(x)
                g.fill(from, edgeY, right, edgeY + stroke, color)
            }
        }

        drawExposedEdge(index.takeIf { it > 0 }?.minus(1), y)
        drawExposedEdge(
            index.takeIf { it < rowWidths.lastIndex }?.plus(1),
            y + ROW_H - stroke,
        )

    }

    private fun drawText(
        g: GuiGraphicsExtractor,
        font: net.minecraft.client.gui.Font,
        text: Component,
        x: Int,
        y: Int,
        color: Int
    ) {
        when (shadow.value) {
            Shadow.NONE -> {}
            Shadow.SIMPLE -> g.Text(font, text, x + 1, y + 1, shadowColor((shadowStrength.value * 0.75f).toInt()), false)
            Shadow.SOFT -> drawSoftShadow(g, font, text, x, y)
        }
        g.Text(font, text, x, y, color, false)
    }

    private fun drawSoftShadow(
        g: GuiGraphicsExtractor,
        font: net.minecraft.client.gui.Font,
        text: Component,
        x: Int,
        y: Int
    ) {
        val strength = shadowStrength.value
        val outer = (strength * 0.22f).toInt()
        val mid = (strength * 0.34f).toInt()
        val inner = (strength * 0.50f).toInt()

        g.Text(font, text, x - 1, y, shadowColor(mid), false)
        g.Text(font, text, x + 1, y, shadowColor(mid), false)
        g.Text(font, text, x, y - 1, shadowColor(mid), false)
        g.Text(font, text, x, y + 1, shadowColor(mid), false)

        g.Text(font, text, x - 1, y - 1, shadowColor(outer), false)
        g.Text(font, text, x + 1, y - 1, shadowColor(outer), false)
        g.Text(font, text, x - 1, y + 1, shadowColor(inner), false)
        g.Text(font, text, x + 1, y + 1, shadowColor(inner), false)

        g.Text(font, text, x, y + 2, shadowColor(outer), false)
    }
}
