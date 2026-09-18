package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.minigames.PartyGames
import onl.luka.grizzly.module.modules.skyblock.DojoHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

enum class AntiBotMode { SMART, MATRIX }

object TargetFilter : Module(
    name = "Target Filter",
    description = "Filters targets across combat modules (anti-bot, teams, etc)",
    category = Category.OTHER
) {
    override val showInModulesList = false

    private val ignoreTeam  = boolean("ignore team", true)
    private val ignoreColor = boolean("ignore color", true)
    private val ignoreInvis = boolean("ignore invisible", true)
    private val antiBot     = boolean("anti-bot", true)
    private val antiBotMode = enum("anti-bot mode", AntiBotMode.SMART).also {
        it.visibleWhen = { antiBot.value }
    }

    private var lastAttackTick = Int.MIN_VALUE

    @JvmStatic
    fun onAttack() {
        lastAttackTick = Minecraft.getInstance().player?.tickCount ?: return
    }

    private fun isSmartBot(target: Player, info: net.minecraft.client.multiplayer.PlayerInfo?): Boolean {
        if (info == null || info.latency < 0) return true

        val name = target.name.string
        if (name.isEmpty() || name.contains(" ") || name.startsWith("§")) return true
        if (info.latency == 0 || info.latency > 100000) return true

        return target.tickCount < 10
    }

    private fun isMatrixBot(
        player: Player,
        target: Player,
        info: net.minecraft.client.multiplayer.PlayerInfo?,
    ): Boolean {
        if (info == null) return true

        val sinceAttack = player.tickCount - lastAttackTick
        return target.tickCount <= MATRIX_SPAWN_TICKS && sinceAttack in 0..MATRIX_SPAWN_TICKS
    }

    private const val MATRIX_SPAWN_TICKS = 8

    fun isValidTarget(player: Player, target: LivingEntity): Boolean {
        if (PartyGames.shouldBlockTarget(target) || DojoHelper.shouldBlockTarget(target)) {
            return false
        }
        if (!isEnabled()) return true

        if (ignoreInvis.value && target.isInvisible) {
            return false
        }

        if (ignoreTeam.value) {
            val pTeam = player.team
            val tTeam = target.team
            if (pTeam != null && tTeam != null && pTeam.isAlliedTo(tTeam)) {
                return false
            }
        }

        if (antiBot.value && target is Player) {
            if (target.uuid == player.uuid) return false

            val info = Minecraft.getInstance().connection?.getPlayerInfo(target.uuid)
            val bot = when (antiBotMode.value) {
                AntiBotMode.SMART -> isSmartBot(target, info)
                AntiBotMode.MATRIX -> isMatrixBot(player, target, info)
            }
            if (bot) return false
        }

        if (ignoreColor.value && target is Player) {
            val pHead = player.getItemBySlot(EquipmentSlot.HEAD)
            val tHead = target.getItemBySlot(EquipmentSlot.HEAD)

            if (!pHead.isEmpty && !tHead.isEmpty) {
                val pColor = pHead.get(DataComponents.DYED_COLOR)?.rgb()
                val tColor = tHead.get(DataComponents.DYED_COLOR)?.rgb()

                if (pColor != null && tColor != null && pColor == tColor) {
                    return false
                }
            }
        }

        return true
    }
}
