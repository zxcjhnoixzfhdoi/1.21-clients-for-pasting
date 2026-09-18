package onl.luka.grizzly.module.modules.player

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft

object FastMine : Module(
    name = "Fast Mine",
    description = "Reduces the delay between blocks and speeds up block breaking",
    category = Category.PLAYER,
) {
    private val breakDelay = int("break delay (ms)", 250, 0, 250)
    private val breakSpeed = float("break speed", 1.0f, 1.0f, 2.0f)
    private val disableInCreative = boolean("disable in creative", true)
    private val decreaseBreakDelay = boolean("decrease break delay", false)

    private var passiveDelayLastSeen = 0

    fun getBreakSpeedMultiplier(): Float {
        if (!canModifyMining()) return 1.0f
        return breakSpeed.value.coerceAtLeast(1.0f)
    }

    fun getBreakDelayOverrideOrMinusOne(): Int {
        if (!canModifyMining() || Minecraft.getInstance().gui.screen() != null) return -1
        val ticks = breakDelay.value / 50
        return if (ticks >= 5) -1 else ticks
    }

    fun adjustPassiveBreakDelay(delay: Int): Int {
        if (!isEnabled() || !decreaseBreakDelay.value || Minecraft.getInstance().player == null) {
            passiveDelayLastSeen = delay
            return delay
        }

        val adjusted = if (delay > 0 && delay == passiveDelayLastSeen) delay - 1 else delay
        passiveDelayLastSeen = adjusted
        return adjusted
    }

    private fun canModifyMining(): Boolean {
        if (!isEnabled()) return false
        val player = Minecraft.getInstance().player ?: return false
        return !disableInCreative.value || !player.hasInfiniteMaterials()
    }

    override fun onDisabled() {
        passiveDelayLastSeen = 0
    }

    override fun hudInfo(): String = "${formatMultiplier(breakSpeed.value)}x"

    private fun formatMultiplier(value: Float): String =
        if (value % 1.0f == 0.0f) value.toInt().toString() else "%.2f".format(value)
}
