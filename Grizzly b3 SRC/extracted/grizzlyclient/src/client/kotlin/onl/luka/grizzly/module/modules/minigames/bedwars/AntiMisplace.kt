package onl.luka.grizzly.module.modules.minigames.bedwars

import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.util.NotificationManager
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

internal class AntiMisplace(private val settings: Settings) {
    data class Settings(
        val enabled: BooleanEntry,
        val notify: BooleanEntry,
    )

    private var lastNotice = 0L

    fun shouldCancelUse(client: Minecraft): Boolean {
        if (!settings.enabled.value || client.gui.screen() != null) return false
        val player = client.player ?: return false
        val level = client.level ?: return false
        if (player.mainHandItem.item != Items.OBSIDIAN && player.offhandItem.item != Items.OBSIDIAN) return false

        val hit = client.hitResult as? BlockHitResult ?: return false
        if (hit.type != HitResult.Type.BLOCK) return false
        val placement = hit.blockPos.relative(hit.direction)
        val protectsBed = net.minecraft.core.Direction.entries.any { direction ->
            level.getBlockState(placement.relative(direction)).block is BedBlock
        }
        if (protectsBed) return false

        val now = System.currentTimeMillis()
        if (settings.notify.value && now - lastNotice > 750L) {
            NotificationManager.show("Anti Misplace", "Obsidian must be next to a bed", 2200L)
            lastNotice = now
        }
        return true
    }

    fun reset() {
        lastNotice = 0L
    }
}
