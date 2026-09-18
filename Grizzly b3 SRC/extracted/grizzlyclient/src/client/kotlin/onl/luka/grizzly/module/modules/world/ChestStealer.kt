package onl.luka.grizzly.module.modules.world

import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.combat.SilentAura
import onl.luka.grizzly.module.modules.utility.InventoryManager
import onl.luka.grizzly.util.ContainerActions
import onl.luka.grizzly.util.SilentScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.Locale

object ChestStealer : Module(
    "Chest Stealer",
    "Silently steals filtered items from open chests",
    Category.WORLD,
) {
    enum class Mode { ALL_CONTAINERS, ONLY_CHESTS }

    val mode = enum("mode", Mode.ONLY_CHESTS)
    val stealFilter = itemList(
        "Steal Filter",
        listOf(
            "ender_pearl",
            "axes_category",
            "swords_category",
            "bow",
            "potion",
            "splash_potion",
            "golden_apple",
            "blocks_category",
        ),
        defaultMode = ItemListEntry.Mode.WHITELIST,
        filter = ItemListEntry.Filter.NONE,
    )
    val delayAmount = intRange("Delay (ms)", 50 to 100, 0, 1000)
    val startDelay = int("Start Delay (ms)", 100, 0, 2000)
    val closeDelay = int("Close Delay (ms)", 100, 0, 2000)
    val instantSteal = boolean("Instant Steal", false)
    val instantItems = int("Instant Items", -1, -1, 54).also {
        it.visibleWhen = { instantSteal.value }
    }

    private var nextActionTime = 0L
    private var silentOpenedByModule = false
    private var openedAt = 0L
    private var closeAt = 0L
    private var instantDone = false
    private var classifiedContainerId = Int.MIN_VALUE
    private var openedFromChest = false
    private var openedContainerId = Int.MIN_VALUE
    private var openedContainerIsVanilla = false
    private val VANILLA_TITLES = setOf(
        "container.chest",
        "container.chestDouble",
        "container.barrel",
        "container.enderchest",
        "container.shulkerBox",
    )

    @JvmStatic
    fun onContainerOpened(containerId: Int, title: Component) {
        openedContainerId = containerId
        openedContainerIsVanilla = title.string == "Chest" || (title.contents as? TranslatableContents)?.key in VANILLA_TITLES
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val gameMode = client.gameMode ?: return

        if (SilentAura.shouldInterruptChestActions(client)) {
            interruptForSilentAura(player)
            return
        }

        val menu = player.containerMenu
        if (menu === player.inventoryMenu || menu !is ChestMenu) {
            resetContainerState()
            return
        }

        if (menu.containerId != classifiedContainerId) {
            classifiedContainerId = menu.containerId
            openedFromChest = menu.containerId == openedContainerId && openedContainerIsVanilla
            openedAt = System.currentTimeMillis()
            instantDone = false
            closeAt = 0L
        }
        if (mode.value == Mode.ONLY_CHESTS && !openedFromChest) return

        val currentScreen = client.gui.screen()
        if (currentScreen is ContainerScreen) {
            client.gui.setScreen(SilentScreen(currentScreen))
            silentOpenedByModule = true
        }

        if (currentScreen != null && currentScreen !is SilentScreen && currentScreen !is ContainerScreen) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - openedAt < startDelay.value) return

        val targets = stealableSlots(menu, player)
        if (targets.isEmpty() || !hasInventorySpace(player)) {
            if (closeAt == 0L) {
                closeAt = now
                return
            }
            if (now - closeAt >= closeDelay.value) closeSilently(client, player)
            return
        }
        closeAt = 0L

        if (instantSteal.value && !instantDone) {
            instantDone = true
            val count = if (instantItems.value < 0) targets.size else minOf(instantItems.value, targets.size)
            if (count > 0) {
                for (index in 0 until count) {
                    if (!ContainerActions.click(this, player, gameMode, targets[index], 0, ContainerInput.QUICK_MOVE)) break
                }
                setDelay()
                return
            }
        }

        if (now < nextActionTime) return
        if (ContainerActions.click(this, player, gameMode, targets.first(), 0, ContainerInput.QUICK_MOVE)) {
            setDelay()
        }
    }

    private fun stealableSlots(menu: ChestMenu, player: net.minecraft.client.player.LocalPlayer): List<Int> {
        val chestSlotCount = (menu.slots.size - 36).coerceAtLeast(0)
        if (chestSlotCount <= 0) return emptyList()

        val managed = InventoryManager.isEnabled()
        return (0 until chestSlotCount).filter { slotIndex ->
            val stack = menu.getSlot(slotIndex).item
            !stack.isEmpty &&
                matchesFilter(stack) &&
                (!managed || InventoryManager.shouldTakeChestItem(player, stack))
        }
    }

    private fun hasInventorySpace(player: net.minecraft.client.player.LocalPlayer): Boolean =
        (0..35).any { player.inventory.getItem(it).isEmpty }

    private fun resetContainerState() {
        silentOpenedByModule = false
        classifiedContainerId = Int.MIN_VALUE
        openedAt = 0L
        closeAt = 0L
        instantDone = false
    }

    override fun onDisabled() {
        val mc = Minecraft.getInstance()
        val player = mc.player
        if (silentOpenedByModule && player != null) {
            closeSilently(mc, player)
        }
        resetContainerState()
    }

    private fun closeSilently(client: Minecraft, player: net.minecraft.client.player.LocalPlayer) {
        player.closeContainer()
        if (client.gui.screen() is SilentScreen) client.gui.setScreen(null)
        silentOpenedByModule = false
    }

    private fun interruptForSilentAura(player: net.minecraft.client.player.LocalPlayer) {
        if (player.containerMenu is ChestMenu) {
            closeSilently(Minecraft.getInstance(), player)
        }
        resetContainerState()
        nextActionTime = 0L
    }

    private fun matchesFilter(stack: ItemStack): Boolean {
        val itemName = stack.item.descriptionId.lowercase(Locale.ROOT)
        val list = stealFilter.value
        if (list.isEmpty()) {
            return true
        }

        var matched = false
        for (entry in list) {
            val token = entry.lowercase(Locale.ROOT).trim()
            if (token.isBlank()) continue

            if (token.endsWith("_category")) {
                val categoryId = token.removeSuffix("_category")
                val category = itemCategories.firstOrNull { it.id == categoryId }
                if (category != null && category.matches(itemName)) {
                    matched = true
                    break
                }
            } else if (itemName.contains(token)) {
                matched = true
                break
            }
        }

        return when (stealFilter.mode) {
            ItemListEntry.Mode.WHITELIST -> matched
            ItemListEntry.Mode.BLACKLIST -> !matched
            else -> matched
        }
    }

    private fun setDelay() {
        val (min, max) = delayAmount.value
        val actualDelay = if (min >= max) min else min + java.util.Random().nextInt(max - min + 1)
        nextActionTime = System.currentTimeMillis() + actualDelay
    }
}
