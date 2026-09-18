package onl.luka.grizzly.module.modules.minigames.bedwars

import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.NotificationManager
import onl.luka.grizzly.util.Text
import onl.luka.grizzly.util.roundedFill
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal class ResourceTracker(private val settings: Settings) {
    data class Settings(
        val enabled: BooleanEntry,
        val iron: BooleanEntry,
        val gold: BooleanEntry,
        val diamonds: BooleanEntry,
        val emeralds: BooleanEntry,
        val notifyChanges: BooleanEntry,
        val background: BooleanEntry,
        val textColor: ColorEntry,
    )

    private data class Resource(
        val item: Item,
        val label: String,
        val enabled: BooleanEntry,
    )

    private val tracked = listOf(
        Resource(Items.IRON_INGOT, "Iron", settings.iron),
        Resource(Items.GOLD_INGOT, "Gold", settings.gold),
        Resource(Items.DIAMOND, "Diamonds", settings.diamonds),
        Resource(Items.EMERALD, "Emeralds", settings.emeralds),
    )
    private val counts = mutableMapOf<Item, Int>()
    private val previousCounts = mutableMapOf<Item, Int>()
    private var initialized = false
    private var lastWidth = 84
    private var lastHeight = 20

    fun tick(client: Minecraft) {
        if (!settings.enabled.value) return
        val player = client.player ?: return
        counts.clear()
        for (resource in tracked) {
            var count = 0
            for (slot in 0 until player.inventory.containerSize) {
                val stack = player.inventory.getItem(slot)
                if (stack.item == resource.item) count += stack.count
            }
            counts[resource.item] = count
        }

        if (initialized && settings.notifyChanges.value) {
            for (resource in tracked) {
                if (!resource.enabled.value) continue
                val old = previousCounts[resource.item] ?: 0
                val current = counts[resource.item] ?: 0
                if (old == current) continue
                val delta = current - old
                NotificationManager.show(
                    "Resources",
                    "${if (delta > 0) "+" else ""}$delta ${resource.label} ($current)",
                    2000L,
                )
            }
        }
        previousCounts.clear()
        previousCounts.putAll(counts)
        initialized = true
    }

    fun render(g: GuiGraphicsExtractor, bedLine: String?) {
        val lines = buildList {
            if (settings.enabled.value) {
                tracked.filter { it.enabled.value }.forEach { resource ->
                    add(resource to (counts[resource.item] ?: 0).toString())
                }
            }
        }
        if (lines.isEmpty() && bedLine == null) {
            lastWidth = 84
            lastHeight = 20
            return
        }

        val font = Font.getFont()
        val lineHeight = maxOf(16, font.lineHeight + 5)
        val textColor = settings.textColor.liveColor(settings.textColor.value).argb
        val resourceWidth = lines.maxOfOrNull { (resource, count) ->
            font.width(Font.styledText("${resource.label}: $count")) + 28
        } ?: 0
        val bedWidth = bedLine?.let { font.width(Font.styledText(it)) + 10 } ?: 0
        lastWidth = maxOf(
            if (lines.isNotEmpty()) 84 else 0,
            resourceWidth,
            bedWidth,
        )
        lastHeight = if (bedLine == null) {
            8 + lines.size * lineHeight
        } else {
            8 + lines.size * lineHeight + font.lineHeight
        }

        if (settings.background.value) {
            g.roundedFill(0, 0, lastWidth, lastHeight, 5, 0xB812141C.toInt())
        }

        var y = 4
        for ((resource, count) in lines) {
            g.item(ItemStack(resource.item), 4, y)
            g.Text(font, Font.styledText("${resource.label}: $count"), 24, y + 4, textColor)
            y += lineHeight
        }
        if (bedLine != null) {
            g.Text(font, Font.styledText(bedLine), 5, y, textColor)
        }
    }

    fun hudWidth(): Int = lastWidth

    fun hudHeight(hasBedLine: Boolean): Int {
        val enabledLines = if (settings.enabled.value) tracked.count { it.enabled.value } else 0
        if (enabledLines == 0 && !hasBedLine) return 20
        val font = Font.getFont()
        val lineHeight = maxOf(16, font.lineHeight + 5)
        return 8 + enabledLines * lineHeight + if (hasBedLine) font.lineHeight else 0
    }

    fun reset() {
        counts.clear()
        previousCounts.clear()
        initialized = false
        lastWidth = 84
        lastHeight = 20
    }
}
