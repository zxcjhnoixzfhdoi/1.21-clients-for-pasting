package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import onl.luka.grizzly.util.Text
import onl.luka.grizzly.util.roundedFill
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

object ScoreboardHud : HudModule(
    "Scoreboard",
    "Replaces the vanilla scoreboard with a movable, styleable one",
    defaultX = 0.996f,
    defaultY = 0.35f,
) {

    private const val LINE_H = 9
    private const val MAX_ROWS = 15
    private const val SCORE_GAP = 6

    val background     = boolean("background", true)
    val backgroundMode = enum("background mode", HudBackgroundMode.SOLID).also {
        it.visibleWhen = { background.value }
    }
    val blurStrength   = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { background.value && backgroundMode.value == HudBackgroundMode.BLUR }
    }
    val rounded        = boolean("rounded", true).also { it.visibleWhen = { background.value } }
    val roundRadius    = int("round radius", 4, 1, 8).also {
        it.visibleWhen = { background.value && rounded.value }
    }
    val bgColor        = color("bg color", Color(0, 0, 0, 110), allowAlpha = true).also {
        it.visibleWhen = { background.value }
    }
    val titleBar       = boolean("title bar", true).also { it.visibleWhen = { background.value } }
    val titleBarColor  = color("title bar color", Color(0, 0, 0, 90), allowAlpha = true).also {
        it.visibleWhen = { background.value && titleBar.value }
    }
    private val panelShadow = hudDropShadow { background.value }
    val showTitle      = boolean("show title", true)
    val showScores     = boolean("show scores", true)
    val textColor      = color("text color", Color(255, 255, 255, 255), allowAlpha = true)
    val scoreColor     = color("score color", Color(255, 85, 85, 255), allowAlpha = true).also {
        it.visibleWhen = { showScores.value }
    }
    val textShadow     = boolean("text shadow", true)
    val padX           = int("pad x", 3, 0, 8)
    val padY           = int("pad y", 2, 0, 8)
    val avoidModulesList = boolean("avoid modules list", true)

    private data class Row(val name: Component, val score: String)

    private fun objective(): Objective? =
        board()?.getDisplayObjective(DisplaySlot.SIDEBAR)

    private fun board(): Scoreboard? = Minecraft.getInstance().level?.scoreboard

    private fun rows(): List<Row> {
        val objective = objective() ?: return emptyList()
        val board = board() ?: return emptyList()

        return board.listPlayerScores(objective)
            .filterNot { it.isHidden }
            .sortedByDescending { it.value() }
            .take(MAX_ROWS)
            .map { entry ->
                val team = board.getPlayersTeam(entry.owner())
                Row(
                    name = Font.styledText(PlayerTeam.formatNameForTeam(team, entry.ownerName())),
                    score = entry.value().toString(),
                )
            }
    }

    private fun titleText(): Component? =
        if (showTitle.value) objective()?.displayName?.let(Font::styledText) else null

    private fun rowWidth(font: net.minecraft.client.gui.Font, row: Row): Int {
        val nameW = font.width(row.name)
        return if (showScores.value) nameW + SCORE_GAP + font.width(Font.styledText(row.score)) else nameW
    }

    override fun hudWidth(): Int {
        val font = Font.getFont()
        val rows = rows()
        if (rows.isEmpty()) return 80

        val titleW = titleText()?.let { font.width(it) } ?: 0
        return maxOf(titleW, rows.maxOf { rowWidth(font, it) }) + padX.value * 2
    }

    override fun hudHeight(): Int {
        val rows = rows()
        if (rows.isEmpty()) return LINE_H + padY.value * 2
        val lines = rows.size + (if (titleText() != null) 1 else 0)
        return lines * LINE_H + padY.value * 2
    }

    override fun hudPixelX(screenW: Int): Int {
        val px = (hudX.value * screenW).toInt()
        return if (hudX.value > 0.5f) px - (measuredWidth() * hudScale.value).toInt() else px
    }

    override fun hudXFromLeftPixel(leftPx: Int, screenW: Int): Float =
        if (hudX.value > 0.5f) (leftPx + measuredWidth() * hudScale.value).toFloat() / screenW
        else leftPx.toFloat() / screenW

    private fun avoidedY(baseY: Int, screenW: Int, screenH: Int): Int {
        if (!avoidModulesList.value || !ModulesList.isEnabled()) return baseY

        val listScale = ModulesList.hudScale.value
        val listX = ModulesList.hudPixelX(screenW)
        val listW = (ModulesList.measuredWidth() * listScale).toInt()
        val listY = (ModulesList.hudY.value * screenH).toInt()
        val listH = (ModulesList.measuredHeight() * listScale).toInt()

        val x = hudPixelX(screenW)
        val w = (hudWidth() * hudScale.value).toInt()
        val h = (hudHeight() * hudScale.value).toInt()

        val overlapsX = x < listX + listW && listX < x + w
        val overlapsY = baseY < listY + listH && listY < baseY + h
        if (!overlapsX || !overlapsY) return baseY

        val pushDown = baseY + h / 2 >= listY + listH / 2
        return if (pushDown) {
            (listY + listH + 1).coerceAtMost((screenH - h).coerceAtLeast(0))
        } else {
            (listY - h - 1).coerceAtLeast(0)
        }
    }

    override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        withFont { drawHud(extractor, delta) }
    }

    private fun drawHud(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        if (rows().isEmpty()) return

        val mc = Minecraft.getInstance()
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        val px = hudPixelX(screenW)
        val py = avoidedY((hudY.value * screenH).toInt(), screenW, screenH)
        val sc = hudScale.value

        extractor.pose().pushMatrix()
        extractor.pose().translate(px.toFloat(), py.toFloat())
        if (sc != 1.0f) extractor.pose().scale(sc, sc)
        Font.withRenderScale(sc) { renderHudElement(extractor) }
        extractor.pose().popMatrix()
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        val rows = rows()
        if (rows.isEmpty()) return

        val font = Font.getFont()
        val title = titleText()
        val width = hudWidth()
        val height = hudHeight()
        val radius = if (rounded.value) roundRadius.value else 0

        if (background.value) {
            panelShadow.draw(g, 0, 0, width, height, radius)
            drawPanel(g, width, height, radius)
            if (title != null && titleBar.value) {
                drawTitleBar(g, width, radius)
            }
        }

        var y = padY.value
        if (title != null) {
            val titleX = (width - font.width(title)) / 2
            g.Text(font, title, titleX, y, textColor.liveColor(textColor.value).argb, textShadow.value)
            y += LINE_H
        }

        val nameColor = textColor.liveColor(textColor.value).argb
        val valueColor = scoreColor.liveColor(scoreColor.value).argb
        for (row in rows) {
            g.Text(font, row.name, padX.value, y, nameColor, textShadow.value)
            if (showScores.value) {
                val scoreComp = Font.styledText(row.score)
                val scoreX = width - padX.value - font.width(scoreComp)
                g.Text(font, scoreComp, scoreX, y, valueColor, textShadow.value)
            }
            y += LINE_H
        }
    }

    private fun drawPanel(g: GuiGraphicsExtractor, width: Int, height: Int, radius: Int) {
        val color = bgColor.liveColor(bgColor.value).argb
        if (backgroundMode.value == HudBackgroundMode.BLUR) {
            HudBlur.draw(g, 0, 0, width, height, radius, color, blurStrength.value)
        } else if (rounded.value) {
            g.roundedFill(0, 0, width, height, radius, color)
        } else {
            g.fill(0, 0, width, height, color)
        }
    }

    private fun drawTitleBar(g: GuiGraphicsExtractor, width: Int, radius: Int) {
        val color = titleBarColor.liveColor(titleBarColor.value).argb
        val barHeight = LINE_H + padY.value
        if (rounded.value) {
            g.roundedFill(0, 0, width, barHeight, radius, color, onl.luka.grizzly.util.CORNER_TL or onl.luka.grizzly.util.CORNER_TR)
        } else {
            g.fill(0, 0, width, barHeight, color)
        }
    }

    override fun hudInfo(): String = ""
}
