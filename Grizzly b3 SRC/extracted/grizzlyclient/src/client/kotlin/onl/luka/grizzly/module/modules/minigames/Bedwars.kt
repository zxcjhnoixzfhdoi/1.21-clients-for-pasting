package onl.luka.grizzly.module.modules.minigames

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.minigames.bedwars.AntiMisplace
import onl.luka.grizzly.module.modules.minigames.bedwars.BedTracker
import onl.luka.grizzly.module.modules.minigames.bedwars.BedwarsAlerts
import onl.luka.grizzly.module.modules.minigames.bedwars.BedwarsContext
import onl.luka.grizzly.module.modules.minigames.bedwars.ResourceTracker
import onl.luka.grizzly.module.modules.minigames.bedwars.ShopHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

object Bedwars : HudModule(
    name = "Bedwars",
    description = "Shop, placement, alert, resource, and bed helpers for Bedwars",
    category = Module.Category.MINIGAMES,
) {
    private val generalGroup = group("General")
    private val shopGroup = group("Shop Helper", "Highlights purchases and prevents common shop mistakes")
    private val misplaceGroup = group("Anti Misplace", "Prevents obsidian placements that do not defend a bed")
    private val alertsGroup = group("Alerts", "Held item, consume, pickup, armor, trap, and bed alerts")
    private val resourcesGroup = group("Resources", "Tracks Bedwars currencies in your inventory")
    private val bedGroup = group("Bed Tracker", "Tracks your bed and warns when enemies approach it")

    private val onlyInBedwars = boolean("only in bedwars", true).inGroup(generalGroup)
    private val ignoreTeams = boolean("ignore teammates", true).inGroup(generalGroup)

    private val shop = ShopHelper(
        ShopHelper.Settings(
            enabled = boolean("shop helper", true).inGroup(shopGroup),
            highlightAffordable = boolean("highlight affordable", true).inGroup(shopGroup),
            highlightAlpha = int("highlight opacity", 72, 10, 200).inGroup(shopGroup),
            replaceClicks = boolean("middle click purchases", true).inGroup(shopGroup),
            preventDuplicates = boolean("prevent duplicates", true).inGroup(shopGroup),
        ),
    )

    private val antiMisplace = AntiMisplace(
        AntiMisplace.Settings(
            enabled = boolean("anti misplace", true).inGroup(misplaceGroup),
            notify = boolean("misplace notification", true).inGroup(misplaceGroup),
        ),
    )

    private val alerts = BedwarsAlerts(
        BedwarsAlerts.Settings(
            enabled = boolean("alerts", true).inGroup(alertsGroup),
            sound = boolean("alert sound", true).inGroup(alertsGroup),
            showDistance = boolean("show alert distance", true).inGroup(alertsGroup),
            maxDistance = int("alert range", 0, 0, 250).inGroup(alertsGroup),
            cooldownSeconds = int("alert cooldown", 6, 1, 30).inGroup(alertsGroup),
            itemAlerts = boolean("item alerts", true).inGroup(alertsGroup),
            consumeAlerts = boolean("consume alerts", true).inGroup(alertsGroup),
            pickupAlerts = boolean("pickup alerts", true).inGroup(alertsGroup),
            armorAlerts = boolean("armor alerts", true).inGroup(alertsGroup),
            gameAlerts = boolean("trap and bed alerts", true).inGroup(alertsGroup),
            alertWeapons = boolean("alert weapons", true).inGroup(alertsGroup),
            alertTools = boolean("alert tools", true).inGroup(alertsGroup),
            alertPotions = boolean("alert potions", true).inGroup(alertsGroup),
            alertUtility = boolean("alert utility", true).inGroup(alertsGroup),
            pickupIron = boolean("pickup iron", false).inGroup(alertsGroup),
            pickupGold = boolean("pickup gold", false).inGroup(alertsGroup),
            pickupDiamonds = boolean("pickup diamonds", true).inGroup(alertsGroup),
            pickupEmeralds = boolean("pickup emeralds", true).inGroup(alertsGroup),
        ),
        ::isEnemy,
    )

    private val resources = ResourceTracker(
        ResourceTracker.Settings(
            enabled = boolean("resource tracker", true).inGroup(resourcesGroup),
            iron = boolean("show iron", true).inGroup(resourcesGroup),
            gold = boolean("show gold", true).inGroup(resourcesGroup),
            diamonds = boolean("show diamonds", true).inGroup(resourcesGroup),
            emeralds = boolean("show emeralds", true).inGroup(resourcesGroup),
            notifyChanges = boolean("resource change alerts", false).inGroup(resourcesGroup),
            background = boolean("resource background", true).inGroup(resourcesGroup),
            textColor = color("resource text color", Color(245, 245, 250, 255), allowAlpha = false).inGroup(resourcesGroup),
        ),
    )

    private val bedTracker = BedTracker(
        BedTracker.Settings(
            enabled = boolean("bed tracker", true).inGroup(bedGroup),
            showDistance = boolean("show bed distance", true).inGroup(bedGroup),
            enemyAlerts = boolean("enemy proximity alerts", true).inGroup(bedGroup),
            alertDistance = int("bed alert range", 25, 5, 75).inGroup(bedGroup),
            alertCooldown = int("bed alert cooldown", 8, 2, 30).inGroup(bedGroup),
            scanRadius = int("bed scan radius", 28, 12, 48).inGroup(bedGroup),
        ),
        ::isEnemy,
        alerts::sendBedWarning,
    )

    private var runtimeActive = false

    init {
        hudX.value = 0.015f
        hudX.defaultValue = 0.015f
        hudY.value = 0.18f
        hudY.defaultValue = 0.18f
    }

    fun initializeContext() {
        BedwarsContext.initialize()
    }

    override fun onTick(client: Minecraft) {
        val active = BedwarsContext.isActive(client, onlyInBedwars.value)
        if (!active) {
            if (runtimeActive) resetFeatures()
            runtimeActive = false
            return
        }

        runtimeActive = true
        shop.tick(client)
        alerts.tick(client)
        resources.tick(client)
        bedTracker.tick(client)
    }

    override fun onDisabled() {
        runtimeActive = false
        resetFeatures()
    }

    @JvmStatic
    fun onWorldLeave() {
        runtimeActive = false
        resetFeatures()
        BedwarsContext.reset()
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        if (!runtimeActive) return
        resources.render(g, bedTracker.hudLine(Minecraft.getInstance()))
    }

    override fun hudWidth(): Int = resources.hudWidth()

    override fun hudHeight(): Int = resources.hudHeight(bedTracker.hasHudLine())

    @JvmStatic
    fun renderShopSlot(g: GuiGraphicsExtractor, slot: Slot, title: String) {
        if (!isRuntimeReady()) return
        shop.renderSlot(g, slot, title)
    }

    @JvmStatic
    fun handleShopSlotClick(
        slot: Slot?,
        slotId: Int,
        mouseButton: Int,
        input: ContainerInput,
        title: String,
        containerId: Int,
    ): Boolean {
        if (!isRuntimeReady()) return false
        return shop.handleClick(slot, slotId, mouseButton, input, title, containerId)
    }

    @JvmStatic
    fun shouldCancelUseItem(client: Minecraft): Boolean {
        if (!isRuntimeReady()) return false
        return antiMisplace.shouldCancelUse(client)
    }

    @JvmStatic
    fun onTakeItem(packet: ClientboundTakeItemEntityPacket) {
        if (!isRuntimeReady()) return
        alerts.onTakeItem(packet)
    }

    @JvmStatic
    fun onSystemChat(message: String) {
        if (!isRuntimeReady()) return
        alerts.onSystemChat(message)
    }

    private fun isEnemy(local: net.minecraft.world.entity.player.Player, other: net.minecraft.world.entity.player.Player): Boolean =
        BedwarsContext.isEnemy(local, other, ignoreTeams.value)

    private fun isRuntimeReady(): Boolean = isEnabled() && runtimeActive

    private fun resetFeatures() {
        shop.reset()
        antiMisplace.reset()
        alerts.reset()
        resources.reset()
        bedTracker.reset()
    }
}
