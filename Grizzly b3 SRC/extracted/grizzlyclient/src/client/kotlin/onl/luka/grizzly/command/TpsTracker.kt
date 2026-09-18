package onl.luka.grizzly.command

/**
 * Measures the server tick rate using the server game time received in
 * time-sync packets. The server increments its game time once per server tick,
 * so the rate at which it advances against the wall clock is the real TPS.
 */
object TpsTracker {

    private const val SAMPLE_MS = 1000L

    private var windowStartNanos = 0L
    private var windowStartTime = -1L
    private var smoothedTps = 20.0

    /** Called for every server time-sync packet. */
    @JvmStatic
    fun onServerTime(gameTime: Long) {
        val now = System.nanoTime()

        // First sample or a time reset (new world / server restart): restart.
        if (windowStartTime < 0 || gameTime < windowStartTime) {
            windowStartTime = gameTime
            windowStartNanos = now
            return
        }
        if (gameTime <= windowStartTime) return

        val elapsedMs = (now - windowStartNanos) / 1_000_000.0
        if (elapsedMs >= SAMPLE_MS) {
            val measured = (gameTime - windowStartTime) / elapsedMs * 1000.0
            smoothedTps = if (smoothedTps <= 0.0) measured else smoothedTps + (measured - smoothedTps) * 0.3
            windowStartTime = gameTime
            windowStartNanos = now
        }
    }

    fun current(): Double = smoothedTps
}
