package onl.luka.grizzly.module.modules.render

import onl.luka.grizzly.module.Module
import java.time.LocalTime

object Ambience : Module(
    name = "Ambience",
    description = "Changes the world appearance properties",
    category = Category.RENDER,
) {
    private const val TICKS_PER_DAY = 24000L
    private const val SECONDS_PER_DAY = 86400L
    private const val MIDNIGHT_OFFSET = 18000L

    private val realTime = boolean("real world time", false)
    private val time = int("time", 6000, 0, TICKS_PER_DAY.toInt()).also {
        it.visibleWhen = { !realTime.value }
    }

    // 26.2 drives sky colour, light and the sun off the world clock rather than a stored day
    // time, so overriding the tick count is what actually moves the sky.
    @JvmStatic
    fun clockTimeOverride(): Long {
        if (!isEnabled()) return -1L
        if (!realTime.value) return time.value.toLong()

        val secondsOfDay = LocalTime.now().toSecondOfDay().toLong()
        return (secondsOfDay * TICKS_PER_DAY / SECONDS_PER_DAY + MIDNIGHT_OFFSET) % TICKS_PER_DAY
    }

    override fun hudInfo(): String =
        if (realTime.value) "real" else time.value.toString()
}
