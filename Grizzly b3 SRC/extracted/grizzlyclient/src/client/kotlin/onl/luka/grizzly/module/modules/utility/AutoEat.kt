package onl.luka.grizzly.module.modules.utility

import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.InputUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object AutoEat : Module(
    name = "Auto Eat",
    description = "Eats when you get hungry, and gaps when you get low",
    category = Category.UTILITY,
) {

    private val gappleGroup = group("Auto Gapple")
    private val safetyGroup = group("Safety")

    private val foodFilter = itemList("food filter", listOf("food_category"))
    private val hunger = int("hunger", 16, 1, 20)
    private val restoreSlot = boolean("restore slot", true)

    private val autoGapple = boolean("auto gapple", true).inGroup(gappleGroup)
    private val gappleHealth = float("gapple health", 14f, 1f, 20f).inGroup(gappleGroup).also {
        it.visibleWhen = { autoGapple.value }
    }
    private val enchantedOnly = boolean("enchanted only", false).inGroup(gappleGroup).also {
        it.visibleWhen = { autoGapple.value }
    }

    private val avoidPlayers = boolean("avoid players", true).inGroup(safetyGroup)
    private val playerRange = float("player range", 6f, 1f, 20f).inGroup(safetyGroup).also {
        it.visibleWhen = { avoidPlayers.value }
    }
    private val damageTicks = int("damage ticks", 10, 0, 60).inGroup(safetyGroup)

    private var eating = false
    private var previousSlot = -1
    private var ticksSinceDamage = Int.MAX_VALUE
    private var lastHealth = -1f

    override fun onEnabled() {
        eating = false
        previousSlot = -1
        ticksSinceDamage = Int.MAX_VALUE
        lastHealth = -1f
    }

    override fun onDisabled() {
        stop(Minecraft.getInstance())
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        trackDamage(player)

        if (client.gui.screen() != null) {
            stop(client)
            return
        }

        if (eating) {
            continueEating(client, player)
            return
        }

        if (player.isUsingItem || !isSafe(client, player)) return

        val slot = chooseSlot(player)
        if (slot == -1) return

        previousSlot = player.inventory.selectedSlot
        if (previousSlot != slot) player.inventory.setSelectedSlot(slot)
        eating = true
        client.options.keyUse.setDown(true)
    }

    private fun continueEating(client: Minecraft, player: LocalPlayer) {
        // Losing the item mid bite ends the attempt rather than leaving use held down.
        if (!isEdible(player.mainHandItem)) {
            stop(client)
            return
        }
        client.options.keyUse.setDown(true)
        if (player.isUsingItem) return
        if (chooseSlot(player) == -1) stop(client)
    }

    /** Gapples take priority; hunger only matters once health is fine. */
    private fun chooseSlot(player: LocalPlayer): Int {
        if (autoGapple.value && player.health <= gappleHealth.value) {
            val gapple = findGapple(player)
            if (gapple != -1) return gapple
        }
        if (player.foodData.foodLevel > hunger.value) return -1
        return findFood(player)
    }

    private fun findGapple(player: LocalPlayer): Int {
        var fallback = -1
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            if (stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)) return slot
            if (!enchantedOnly.value && stack.`is`(Items.GOLDEN_APPLE) && fallback == -1) fallback = slot
        }
        return fallback
    }

    private fun findFood(player: LocalPlayer): Int {
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            if (isEdible(stack) && matchesFilter(stack)) return slot
        }
        return -1
    }

    private fun isSafe(client: Minecraft, player: LocalPlayer): Boolean {
        if (ticksSinceDamage < damageTicks.value) return false
        if (!avoidPlayers.value) return true
        val level = client.level ?: return true
        val nearest = level.players()
            .filter { it !== player && it.isAlive }
            .minOfOrNull { player.distanceTo(it) }
            ?: return true
        return nearest > playerRange.value
    }

    private fun stop(client: Minecraft) {
        if (!eating) return
        eating = false
        client.options.keyUse.setDown(InputUtil.isPhysicalKeyDown(client.options.keyUse))
        val player = client.player
        if (restoreSlot.value && player != null && previousSlot in 0..8) {
            player.inventory.setSelectedSlot(previousSlot)
        }
        previousSlot = -1
    }

    private fun trackDamage(player: LocalPlayer) {
        if (lastHealth >= 0f && player.health < lastHealth) {
            ticksSinceDamage = 0
        } else if (ticksSinceDamage < Int.MAX_VALUE) {
            ticksSinceDamage++
        }
        lastHealth = player.health
    }

    private fun isEdible(stack: ItemStack): Boolean =
        !stack.isEmpty && stack.get(DataComponents.FOOD) != null

    private fun matchesFilter(stack: ItemStack): Boolean {
        val entries = foodFilter.value
        if (entries.isEmpty()) return foodFilter.mode == ItemListEntry.Mode.BLACKLIST

        val id = BuiltInRegistries.ITEM.getKey(stack.item).path.lowercase()
        val listed = entries.any { entry ->
            if (entry.endsWith("_category")) {
                val category = entry.removeSuffix("_category")
                itemCategories.firstOrNull { it.id == category }?.matches?.invoke(id) == true
            } else {
                id.contains(entry.lowercase())
            }
        }
        return if (foodFilter.mode == ItemListEntry.Mode.BLACKLIST) !listed else listed
    }

    override fun hudInfo(): String = if (eating) "eating" else ""
}
