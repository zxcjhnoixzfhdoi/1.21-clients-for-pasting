package onl.luka.grizzly.module.modules.minigames.bedwars

import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.IntEntry
import onl.luka.grizzly.util.NotificationManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.EnumMap
import java.util.Locale

internal class ShopHelper(private val settings: Settings) {
    data class Settings(
        val enabled: BooleanEntry,
        val highlightAffordable: BooleanEntry,
        val highlightAlpha: IntEntry,
        val replaceClicks: BooleanEntry,
        val preventDuplicates: BooleanEntry,
    )

    private enum class Resource(val item: Item, val rgb: Int) {
        IRON(Items.IRON_INGOT, 0xF2F2F2),
        GOLD(Items.GOLD_INGOT, 0xFFD24A),
        DIAMOND(Items.DIAMOND, 0x55E6FF),
        EMERALD(Items.EMERALD, 0x36D46B),
    }

    private data class Cost(val resource: Resource, val amount: Int)
    private data class Gear(val category: String, val tier: Int)

    private val resources = EnumMap<Resource, Int>(Resource::class.java)
    private var lastDuplicateNotice = 0L

    fun tick(client: Minecraft) {
        if (!settings.enabled.value) return
        val player = client.player ?: return
        resources.clear()
        for (resource in Resource.entries) {
            var count = 0
            for (slot in 0 until player.inventory.containerSize) {
                val stack = player.inventory.getItem(slot)
                if (stack.item == resource.item) count += stack.count
            }
            resources[resource] = count
        }
    }

    fun renderSlot(g: GuiGraphicsExtractor, slot: Slot, title: String) {
        if (!settings.enabled.value || !settings.highlightAffordable.value || !isShop(title)) return
        val stack = slot.item
        val cost = cost(stack) ?: return
        if (!shouldHighlight(stack, cost)) return

        val alpha = settings.highlightAlpha.value.coerceIn(0, 255)
        val color = (alpha shl 24) or cost.resource.rgb
        val x = slot.x
        val y = slot.y
        g.fill(x, y, x + 16, y + 16, color)
    }

    fun handleClick(
        slot: Slot?,
        slotId: Int,
        mouseButton: Int,
        input: ContainerInput,
        title: String,
        containerId: Int,
    ): Boolean {
        if (!settings.enabled.value || slot == null || !slot.hasItem() || !isShop(title)) return false
        if (input != ContainerInput.PICKUP || mouseButton != 0) return false
        if (slotId in SHOP_NAVIGATION_SLOTS) return false

        val stack = slot.item
        if (settings.preventDuplicates.value && !isUpgradeShop(title) && isDuplicate(stack)) {
            val now = System.currentTimeMillis()
            if (now - lastDuplicateNotice > 750L) {
                NotificationManager.show("Shop Helper", "Prevented a duplicate purchase", 2200L)
                lastDuplicateNotice = now
            }
            return true
        }

        if (!settings.replaceClicks.value) return false
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        val gameMode = client.gameMode ?: return false
        gameMode.handleContainerInput(containerId, slotId, 2, ContainerInput.CLONE, player)
        return true
    }

    private fun shouldHighlight(stack: ItemStack, cost: Cost): Boolean {
        if ((resources[cost.resource] ?: 0) < cost.amount) return false
        return gear(stack)?.let { !hasEqualOrBetter(it) } ?: true
    }

    private fun isDuplicate(stack: ItemStack): Boolean {
        val gear = gear(stack) ?: return false
        return hasEqualOrBetter(gear)
    }

    private fun hasEqualOrBetter(candidate: Gear): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        for (slot in 0 until player.inventory.containerSize) {
            val owned = gear(player.inventory.getItem(slot)) ?: continue
            if (owned.category == candidate.category && owned.tier >= candidate.tier) return true
        }
        return false
    }

    private fun gear(stack: ItemStack): Gear? {
        if (stack.isEmpty) return null
        return when (stack.item) {
            Items.WOODEN_SWORD -> Gear("sword", 1)
            Items.STONE_SWORD -> Gear("sword", 2)
            Items.GOLDEN_SWORD -> Gear("sword", 2)
            Items.IRON_SWORD -> Gear("sword", 3)
            Items.DIAMOND_SWORD -> Gear("sword", 4)
            Items.NETHERITE_SWORD -> Gear("sword", 5)

            Items.WOODEN_PICKAXE -> Gear("pickaxe", 1)
            Items.STONE_PICKAXE -> Gear("pickaxe", 2)
            Items.IRON_PICKAXE -> Gear("pickaxe", 2)
            Items.GOLDEN_PICKAXE -> Gear("pickaxe", 3)
            Items.DIAMOND_PICKAXE -> Gear("pickaxe", 4)
            Items.NETHERITE_PICKAXE -> Gear("pickaxe", 5)

            Items.WOODEN_AXE -> Gear("axe", 1)
            Items.STONE_AXE -> Gear("axe", 2)
            Items.GOLDEN_AXE -> Gear("axe", 2)
            Items.IRON_AXE -> Gear("axe", 3)
            Items.DIAMOND_AXE -> Gear("axe", 4)
            Items.NETHERITE_AXE -> Gear("axe", 5)

            Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS -> Gear("armor", 1)
            Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_LEGGINGS -> Gear("armor", 2)
            Items.GOLDEN_BOOTS, Items.GOLDEN_LEGGINGS -> Gear("armor", 2)
            Items.IRON_BOOTS, Items.IRON_LEGGINGS -> Gear("armor", 3)
            Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS -> Gear("armor", 4)
            Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS -> Gear("armor", 5)

            Items.SHEARS -> Gear("shears", 1)
            Items.STICK -> Gear("stick", 1)
            else -> null
        }
    }

    private fun cost(stack: ItemStack): Cost? {
        if (stack.isEmpty) return null
        val lines = runCatching { Screen.getTooltipFromItem(Minecraft.getInstance(), stack) }.getOrNull() ?: return null
        for (component in lines) {
            val match = COST_PATTERN.find(component.string) ?: continue
            val amount = match.groupValues[1].toIntOrNull() ?: continue
            val resource = when (match.groupValues[2].lowercase(Locale.ROOT)) {
                "iron" -> Resource.IRON
                "gold" -> Resource.GOLD
                "diamond", "diamonds" -> Resource.DIAMOND
                "emerald", "emeralds" -> Resource.EMERALD
                else -> continue
            }
            return Cost(resource, amount)
        }
        return null
    }

    private fun isShop(title: String): Boolean {
        val clean = title.lowercase(Locale.ROOT)
        return SHOP_TITLES.any(clean::contains)
    }

    private fun isUpgradeShop(title: String): Boolean {
        val clean = title.lowercase(Locale.ROOT)
        return "upgrade" in clean || "trap" in clean
    }

    fun reset() {
        resources.clear()
        lastDuplicateNotice = 0L
    }

    private companion object {
        val SHOP_NAVIGATION_SLOTS = 0..8
        val COST_PATTERN = Regex("(?i)(?:cost:\\s*|,\\s*)(\\d+)\\s+(iron|gold|diamonds?|emeralds?)")
        val SHOP_TITLES = listOf(
            "quick buy",
            "item shop",
            "blocks",
            "melee",
            "armor",
            "tools",
            "ranged",
            "potions",
            "utility",
            "rotating items",
            "upgrades & traps",
            "team upgrades",
        )
    }
}
