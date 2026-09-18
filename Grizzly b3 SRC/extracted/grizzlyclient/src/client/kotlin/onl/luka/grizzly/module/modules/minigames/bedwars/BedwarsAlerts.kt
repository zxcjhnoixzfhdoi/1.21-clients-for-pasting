package onl.luka.grizzly.module.modules.minigames.bedwars

import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.IntEntry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

internal class BedwarsAlerts(
    private val settings: Settings,
    private val isEnemy: (Player, Player) -> Boolean,
) {
    data class Settings(
        val enabled: BooleanEntry,
        val sound: BooleanEntry,
        val showDistance: BooleanEntry,
        val maxDistance: IntEntry,
        val cooldownSeconds: IntEntry,
        val itemAlerts: BooleanEntry,
        val consumeAlerts: BooleanEntry,
        val pickupAlerts: BooleanEntry,
        val armorAlerts: BooleanEntry,
        val gameAlerts: BooleanEntry,
        val alertWeapons: BooleanEntry,
        val alertTools: BooleanEntry,
        val alertPotions: BooleanEntry,
        val alertUtility: BooleanEntry,
        val pickupIron: BooleanEntry,
        val pickupGold: BooleanEntry,
        val pickupDiamonds: BooleanEntry,
        val pickupEmeralds: BooleanEntry,
    )

    private data class TrackedUse(val stack: ItemStack, val startedTick: Int)
    private data class AlertLabel(val text: String, val color: ChatFormatting)

    private val cooldowns = mutableMapOf<String, Long>()
    private val usingItems = mutableMapOf<UUID, TrackedUse>()
    private val reportedHeldLabels = mutableMapOf<String, MutableSet<String>>()
    private val playersAwaitingRespawn = mutableSetOf<String>()
    private val armorTiers = mutableMapOf<String, Int>()
    private var lastArmorScanTick = 0

    fun tick(client: Minecraft) {
        if (!settings.enabled.value) return
        val local = client.player ?: return
        val level = client.level ?: return

        for (player in level.players()) {
            if (player === local) continue
            val playerKey = playerKey(player)
            if (!player.isAlive || player.isSpectator) {
                playersAwaitingRespawn.add(playerKey)
                usingItems.remove(player.uuid)
                continue
            }
            if (playersAwaitingRespawn.remove(playerKey)) {
                reportedHeldLabels.remove(playerKey)
                usingItems.remove(player.uuid)
            }
            if (!isEnemy(local, player) || !withinRange(local, player)) continue
            if (settings.itemAlerts.value) checkHeldItem(local, player)
            if (settings.consumeAlerts.value) checkConsume(local, player)
        }

        if (settings.armorAlerts.value && local.tickCount - lastArmorScanTick >= 20) {
            lastArmorScanTick = local.tickCount
            for (player in level.players()) {
                if (isEnemy(local, player) && withinRange(local, player)) checkArmor(local, player)
            }
        }

        val present = level.players().mapTo(hashSetOf()) { it.uuid }
        usingItems.keys.retainAll(present)
    }

    fun onTakeItem(packet: ClientboundTakeItemEntityPacket) {
        if (!settings.enabled.value || !settings.pickupAlerts.value) return
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val local = client.player ?: return
        val collector = level.getEntity(packet.playerId) as? Player ?: return
        if (!isEnemy(local, collector) || !withinRange(local, collector)) return
        val itemEntity = level.getEntity(packet.itemId) as? ItemEntity ?: return
        val label = pickupLabel(itemEntity.item) ?: return
        alert(local, collector, "picked up", label, "pickup:${label.text}")
    }

    fun onSystemChat(raw: String) {
        if (!settings.enabled.value || !settings.gameAlerts.value) return
        val clean = raw.trim()
        if (clean.isEmpty()) return
        val lower = clean.lowercase()
        when {
            "trap triggered" in lower || "triggered your trap" in lower ->
                send(
                    Component.literal("Trap triggered").withStyle(ChatFormatting.RED),
                    Component.literal(clean).withStyle(ChatFormatting.WHITE),
                    "game:trap",
                    force = true,
                )
            "bed destruction" in lower && "your bed" in lower ->
                send(
                    Component.literal("Bed destroyed").withStyle(ChatFormatting.RED),
                    Component.literal("Your bed was destroyed").withStyle(ChatFormatting.RED),
                    "game:bed",
                    force = true,
                )
        }
    }

    fun sendBedWarning(player: Player, distance: Int) {
        if (!settings.enabled.value) return
        send(
            title = Component.literal("Bed warning").withStyle(ChatFormatting.RED),
            message = playerTitle(player)
                .append(Component.literal(" is ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("${distance}m").withStyle(ChatFormatting.RED))
                .append(Component.literal(" from your bed").withStyle(ChatFormatting.WHITE)),
            key = "bed:${player.uuid}",
            force = true,
        )
    }

    private fun checkHeldItem(local: Player, player: Player) {
        val playerKey = playerKey(player)
        val stacks = listOf(player.mainHandItem, player.offhandItem)
        val label = stacks.firstNotNullOfOrNull(::heldItemLabel) ?: return
        val reported = reportedHeldLabels.getOrPut(playerKey) { mutableSetOf() }
        if (!reported.add(label.text)) return
        alert(local, player, "has", label, "item:${label.text}")
    }

    private fun checkConsume(local: Player, player: Player) {
        val uuid = player.uuid
        if (player.isUsingItem) {
            val stack = player.useItem
            if (consumeLabel(stack) != null) {
                usingItems.putIfAbsent(uuid, TrackedUse(stack.copy(), player.tickCount))
            }
            return
        }

        val previous = usingItems.remove(uuid) ?: return
        if (player.tickCount - previous.startedTick < 8) return
        val label = consumeLabel(previous.stack) ?: return
        alert(local, player, "consumed", label, "consume:${label.text}", cooldownMs = 1600L)
    }

    private fun checkArmor(local: Player, player: Player) {
        val leggings = player.getItemBySlot(EquipmentSlot.LEGS)
        val tier = when (leggings.item) {
            Items.CHAINMAIL_LEGGINGS -> 1
            Items.IRON_LEGGINGS -> 2
            Items.DIAMOND_LEGGINGS -> 3
            else -> 0
        }
        val playerKey = playerKey(player)
        val previous = armorTiers[playerKey] ?: 0
        if (tier <= previous) return
        armorTiers[playerKey] = tier
        val label = when (tier) {
            1 -> AlertLabel("Chainmail Armor", ChatFormatting.GRAY)
            2 -> AlertLabel("Iron Armor", ChatFormatting.WHITE)
            3 -> AlertLabel("Diamond Armor", ChatFormatting.AQUA)
            else -> return
        }
        alert(local, player, "purchased", label, "armor:$tier")
    }

    private fun heldItemLabel(stack: ItemStack): AlertLabel? {
        if (stack.isEmpty) return null
        val name = stack.hoverName.string.lowercase()
        if (settings.alertWeapons.value) {
            when (stack.item) {
                Items.IRON_SWORD -> return AlertLabel("Iron Sword", ChatFormatting.WHITE)
                Items.DIAMOND_SWORD -> return AlertLabel("Diamond Sword", ChatFormatting.AQUA)
                Items.BOW -> return if (stack.isEnchanted) {
                    AlertLabel("Enchanted Bow", ChatFormatting.LIGHT_PURPLE)
                } else {
                    AlertLabel("Bow", ChatFormatting.GOLD)
                }
                Items.STICK -> if ("knockback" in name) return AlertLabel("Knockback Stick", ChatFormatting.YELLOW)
            }
        }
        if (settings.alertTools.value) {
            when (stack.item) {
                Items.GOLDEN_PICKAXE -> return AlertLabel("Golden Pickaxe", ChatFormatting.GOLD)
                Items.DIAMOND_PICKAXE -> return AlertLabel("Diamond Pickaxe", ChatFormatting.AQUA)
            }
        }
        if (settings.alertPotions.value) {
            potionLabel(name)?.let { return it }
        }
        if (settings.alertUtility.value) {
            return when {
                stack.item == Items.FIRE_CHARGE || "fireball" in name -> AlertLabel("Fireball", ChatFormatting.GOLD)
                stack.item == Items.TNT -> AlertLabel("TNT", ChatFormatting.RED)
                stack.item == Items.ENDER_PEARL -> AlertLabel("Ender Pearl", ChatFormatting.DARK_AQUA)
                stack.item == Items.OBSIDIAN -> AlertLabel("Obsidian", ChatFormatting.DARK_PURPLE)
                stack.item == Items.GOLDEN_APPLE -> AlertLabel("Golden Apple", ChatFormatting.GOLD)
                stack.item == Items.MILK_BUCKET || name == "milk" -> AlertLabel("Milk", ChatFormatting.WHITE)
                stack.item == Items.WATER_BUCKET -> AlertLabel("Water Bucket", ChatFormatting.BLUE)
                "bridge egg" in name -> AlertLabel("Bridge Egg", ChatFormatting.LIGHT_PURPLE)
                "bedbug" in name || "bed bug" in name -> AlertLabel("Bedbug", ChatFormatting.DARK_GRAY)
                "iron golem" in name -> AlertLabel("Iron Golem", ChatFormatting.GRAY)
                "pop-up tower" in name || "popup tower" in name -> AlertLabel("Pop-Up Tower", ChatFormatting.YELLOW)
                else -> null
            }
        }
        return null
    }

    private fun consumeLabel(stack: ItemStack): AlertLabel? {
        if (stack.isEmpty) return null
        val name = stack.hoverName.string.lowercase()
        return when {
            stack.item == Items.GOLDEN_APPLE -> AlertLabel("Golden Apple", ChatFormatting.GOLD)
            stack.item == Items.MILK_BUCKET || name == "milk" -> AlertLabel("Milk", ChatFormatting.WHITE)
            else -> potionLabel(name)
        }
    }

    private fun potionLabel(name: String): AlertLabel? = when {
        "invis" in name -> AlertLabel("Invisibility Potion", ChatFormatting.LIGHT_PURPLE)
        "jump" in name -> AlertLabel("Jump Potion", ChatFormatting.GREEN)
        "speed" in name -> AlertLabel("Speed Potion", ChatFormatting.AQUA)
        else -> null
    }

    private fun pickupLabel(stack: ItemStack): AlertLabel? = when (stack.item) {
        Items.IRON_INGOT -> if (settings.pickupIron.value) AlertLabel("Iron", ChatFormatting.WHITE) else null
        Items.GOLD_INGOT -> if (settings.pickupGold.value) AlertLabel("Gold", ChatFormatting.GOLD) else null
        Items.DIAMOND -> if (settings.pickupDiamonds.value) AlertLabel("Diamonds", ChatFormatting.AQUA) else null
        Items.EMERALD -> if (settings.pickupEmeralds.value) AlertLabel("Emeralds", ChatFormatting.GREEN) else null
        else -> null
    }

    private fun alert(
        local: Player,
        player: Player,
        action: String,
        label: AlertLabel,
        key: String,
        cooldownMs: Long = settings.cooldownSeconds.value * 1000L,
    ) {
        val distance = local.distanceTo(player).roundToInt()
        val message = Component.literal("$action ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(label.text).withStyle(label.color))
        if (settings.showDistance.value) {
            message.append(Component.literal(" (${distance}m)").withStyle(ChatFormatting.GRAY))
        }
        send(playerTitle(player), message, "${player.uuid}:$key", cooldownMs = cooldownMs)
    }

    private fun send(
        title: Component,
        message: Component,
        key: String,
        cooldownMs: Long = settings.cooldownSeconds.value * 1000L,
        force: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        val last = cooldowns[key] ?: 0L
        if (!force && now - last < cooldownMs) return
        if (force && now - last < 1500L) return
        cooldowns[key] = now

        val client = Minecraft.getInstance()
        val chatMessage = Component.literal("[")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal("G").withStyle(Style.EMPTY.withColor(0xA66A3F).withBold(true)))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .append(title)
            .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
            .append(message)
        client.player?.sendSystemMessage(chatMessage)
        if (settings.sound.value) {
            client.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.65f, 1.25f)
        }
    }

    private fun playerTitle(player: Player): net.minecraft.network.chat.MutableComponent {
        val rgb = player.getItemBySlot(EquipmentSlot.HEAD)
            .get(DataComponents.DYED_COLOR)
            ?.rgb()
        return if (rgb != null) {
            player.name.copy().withStyle(Style.EMPTY.withColor(rgb))
        } else {
            player.name.copy().withStyle(ChatFormatting.WHITE)
        }
    }

    private fun withinRange(local: Player, player: Player): Boolean {
        val max = settings.maxDistance.value
        return max <= 0 || local.distanceToSqr(player) <= max.toDouble() * max.toDouble()
    }

    private fun playerKey(player: Player): String =
        player.name.string.lowercase(Locale.ROOT)

    fun reset() {
        cooldowns.clear()
        usingItems.clear()
        reportedHeldLabels.clear()
        playersAwaitingRespawn.clear()
        armorTiers.clear()
        lastArmorScanTick = 0
    }
}
