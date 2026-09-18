package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.util.RotationManager
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import kotlin.math.atan2
import kotlin.math.sqrt

internal object SkyBlockUtils {
    fun preparePerspectiveRotation() {
        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
    }

    fun skyBlockId(stack: ItemStack): String? =
        stack.get(DataComponents.CUSTOM_DATA)
            ?.copyTag()
            ?.getString("id")
            ?.orElse(null)

    fun itemId(stack: ItemStack): String =
        BuiltInRegistries.ITEM.getKey(stack.item).path

    fun blockId(state: BlockState): String =
        BuiltInRegistries.BLOCK.getKey(state.block).path

    fun matchesBlockList(state: BlockState, list: ItemListEntry): Boolean {
        val blockItem = state.block.asItem()
        if (blockItem !is BlockItem && blockItem.defaultInstance.isEmpty) return false
        val selected = list.contains(BuiltInRegistries.ITEM.getKey(blockItem).path)
        return if (list.mode == ItemListEntry.Mode.WHITELIST) selected else !selected
    }

    fun hotbarIndex(oneBasedSlot: Int): Int = (oneBasedSlot - 1).coerceIn(0, 8)

    fun rotationTo(player: LocalPlayer, target: Vec3): Pair<Float, Float> {
        val eye = player.eyePosition
        val dx = target.x - eye.x
        val dy = target.y - eye.y
        val dz = target.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz)
        return Math.toDegrees(atan2(dz, dx)).toFloat() - 90f to
            -Math.toDegrees(atan2(dy, horizontal)).toFloat()
    }

    fun nearbyBlocks(center: BlockPos, horizontal: Int, vertical: Int): Sequence<BlockPos> =
        sequence {
            for (x in -horizontal..horizontal) {
                for (y in -vertical..vertical) {
                    for (z in -horizontal..horizontal) {
                        yield(center.offset(x, y, z))
                    }
                }
            }
        }

    fun tabLines(mc: Minecraft): List<String> =
        mc.connection?.listedOnlinePlayers?.mapNotNull { info ->
            info.tabListDisplayName?.string ?: info.profile.name
        }.orEmpty()

    fun scoreboardLines(mc: Minecraft): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val sidebarSlots = DisplaySlot.entries.filter {
            it == DisplaySlot.SIDEBAR || it.name.startsWith("TEAM_")
        }
        val objectives = sidebarSlots
            .mapNotNull(scoreboard::getDisplayObjective)
            .distinct()
        if (objectives.isEmpty()) return emptyList()

        return buildList {
            objectives.forEach { objective ->
                add(objective.displayName.string)
                scoreboard.listPlayerScores(objective)
                    .asSequence()
                    .filterNot { it.isHidden }
                    .sortedByDescending { it.value() }
                    .forEach { entry ->
                        val team = scoreboard.getPlayersTeam(entry.owner())
                        add(
                            PlayerTeam.formatNameForTeam(
                                team,
                                entry.ownerName(),
                            ).string,
                        )
                        entry.display()?.string?.let(::add)
                        add(
                            PlayerTeam.formatNameForTeam(
                                team,
                                Component.literal(entry.owner()),
                            ).string,
                        )
                    }
            }
        }.filter(String::isNotBlank).distinct()
    }

    fun isInMineshaft(mc: Minecraft): Boolean =
        tabLines(mc).any { it.contains("Mineshaft", ignoreCase = true) }
}
