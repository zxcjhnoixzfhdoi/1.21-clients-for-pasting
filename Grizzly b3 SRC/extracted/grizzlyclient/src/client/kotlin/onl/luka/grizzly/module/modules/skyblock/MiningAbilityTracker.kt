package onl.luka.grizzly.module.modules.skyblock

import net.minecraft.client.Minecraft

internal object MiningAbilityTracker {
    enum class State { UNKNOWN, READY, COOLDOWN }

    private val cooldownPattern = Regex(
        """(Mining Speed Boost|Pickobulus|Maniac Miner|Vein Seeker|Gemstone Infusion).*?(\d+)s""",
        RegexOption.IGNORE_CASE,
    )
    private val readyPattern = Regex(
        """(Mining Speed Boost|Pickobulus|Maniac Miner|Vein Seeker|Gemstone Infusion).*?(Available|Ready|✔)""",
        RegexOption.IGNORE_CASE,
    )

    var state: State = State.UNKNOWN
        private set
    var abilityName: String = "Mining Ability"
        private set
    var cooldownSeconds: Int = 0
        private set

    fun update(mc: Minecraft) {
        val lines = SkyBlockUtils.tabLines(mc)
        for (line in lines) {
            cooldownPattern.find(line)?.let { match ->
                abilityName = match.groupValues[1]
                cooldownSeconds = match.groupValues[2].toIntOrNull() ?: 0
                state = State.COOLDOWN
                return
            }
            readyPattern.find(line)?.let { match ->
                abilityName = match.groupValues[1]
                cooldownSeconds = 0
                state = State.READY
                return
            }
        }
        state = State.UNKNOWN
        cooldownSeconds = 0
    }
}
