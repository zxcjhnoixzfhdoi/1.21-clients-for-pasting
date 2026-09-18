package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.module.modules.world.Clutch
import onl.luka.grizzly.module.modules.world.scaffold.Scaffold
import onl.luka.grizzly.util.radius
import onl.luka.grizzly.util.roundedFill
import onl.luka.grizzly.util.Text
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack

object ScaffoldInfo : HudModule("Block Counter", "Displays hotbar block count for the item in hand") {
    private val bgColor = color("bg color", Color(0, 0, 0, 140), allowAlpha = true)
    private val backgroundMode = enum("background mode", HudBackgroundMode.SOLID)
    private val blurStrength = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { backgroundMode.value == HudBackgroundMode.BLUR }
    }
    private val panelShadow = hudDropShadow()
    private val textColor = color("text color", Color(255, 255, 255, 255))
    private val scaffOnly = boolean("only on scaffold", true)

    private val padding = 4
    private val iconSize = 12
    private val iconGap = 3

    init {
        hudX.value = 0.5f
        hudX.defaultValue = 0.5f
        hudY.value = 0.52f
        hudY.defaultValue = 0.52f
        enable()
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        if (scaffOnly.value && (!Scaffold.isEnabled() && !Clutch.isEnabled())) return
        
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val handStack = player.mainHandItem
        if (handStack.item !is BlockItem) return

        val font = Font.getFont()
        val countText = getText(handStack)
        val textComp = Font.styledText(countText)

        val textWidth = font.width(textComp)
        val totalWidth = textWidth + (padding * 2) + iconSize + iconGap
        val height = font.lineHeight + (padding * 2)

        val px = hudX.value - (totalWidth / 2)
        val py = hudY.value

        g.pose().pushMatrix()
        g.pose().translate(px, py)

        val background = bgColor.liveColor(bgColor.value).argb
        panelShadow.draw(g, 0, 0, totalWidth, height, radius)
        if (backgroundMode.value == HudBackgroundMode.BLUR) {
            HudBlur.draw(g, 0, 0, totalWidth, height, radius, background, blurStrength.value)
        } else {
            g.roundedFill(0, 0, totalWidth, height, radius, background)
        }

        val iconY = font.lineHeight + padding - iconSize
        g.item(handStack, padding, iconY)

        val textX = padding + iconSize + iconGap
        val textY = padding
        g.Text(font, textComp, textX, textY, textColor.liveColor(textColor.value).argb)

        g.pose().popMatrix()
    }

    override fun hudWidth(): Int = 20
    override fun hudHeight(): Int = 20

    fun getText(handStack: ItemStack): String {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return ""
        if (handStack.item !is BlockItem) return ""
        
        var count = 0
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (!stack.isEmpty && stack.item == handStack.item) {
                count += stack.count
            }
        }
        return " $count"
    }
}
