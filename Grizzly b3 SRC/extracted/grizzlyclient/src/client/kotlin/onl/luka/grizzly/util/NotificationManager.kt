package onl.luka.grizzly.util

import onl.luka.grizzly.gui.ui.HudUiResources
import onl.luka.grizzly.gui.ui.MinecraftUiRenderer
import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.gui.ui.UiRuntime
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.module.modules.other.Notifications
import onl.luka.grizzly.util.roundedFill
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

object NotificationManager {
    private const val SLIDE_MS = 330L
    private const val MARGIN = 10
    private const val GAP = 6
    private const val PAD_Y = 7

    enum class Kind { INFO, CONFIG, ENABLED, DISABLED, ERROR }

    private class Notif(
        val title: String,
        val message: String,
        val durationMs: Long,
        val kind: Kind,
    ) {
        val createdAt = System.currentTimeMillis()
        var currentY = Float.NaN

        val hasMessage get() = message.isNotBlank()
        val elapsed get() = System.currentTimeMillis() - createdAt
        val expired get() = elapsed >= durationMs
        val lifeProgress get() = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        fun height(lineHeight: Int): Int =
            if (hasMessage) lineHeight * 2 + PAD_Y * 3 + 2 else lineHeight + PAD_Y * 2 + 4

        val slideProgress: Float get() {
            val e = elapsed
            return when {
                e < SLIDE_MS -> easeOutBack(e.toFloat() / SLIDE_MS)
                e > durationMs - SLIDE_MS -> {
                    val t = (durationMs - e).coerceAtLeast(0L).toFloat() / SLIDE_MS
                    t * t
                }
                else -> 1f
            }
        }
    }

    private val queue = ArrayDeque<Notif>()

    @JvmStatic
    fun show(title: String, message: String = "", durationMs: Long = 3000L) {
        show(title, message, durationMs, Kind.INFO)
    }

    @JvmStatic
    fun showConfig(title: String, message: String = "", durationMs: Long = 3000L) {
        show(title, message, durationMs, Kind.CONFIG)
    }

    /** Red-accented toast for a failure the user should notice. */
    @JvmStatic
    fun showError(title: String, message: String = "", durationMs: Long = 3000L) {
        show(title, message, durationMs, Kind.ERROR)
    }

    @JvmStatic
    fun showModuleToggle(module: Module, enabled: Boolean) {
        show(
            title = module.name,
            message = if (enabled) "Enabled" else "Disabled",
            durationMs = 2400L,
            kind = if (enabled) Kind.ENABLED else Kind.DISABLED,
        )
    }

    /** The module stays on, so this reads as a change of configuration rather than a toggle. */
    fun showProfileSwitch(module: Module, profileName: String) {
        show(
            title = module.name,
            message = "Set to \"$profileName\"",
            durationMs = 2400L,
            kind = Kind.ENABLED,
        )
    }

    private fun show(title: String, message: String, durationMs: Long, kind: Kind) {
        val d = durationMs.coerceAtLeast(SLIDE_MS * 2 + 200L)
        synchronized(queue) { queue.addLast(Notif(title, message, d, kind)) }
    }

    @JvmStatic
    fun render(g: GuiGraphicsExtractor) {
        if (!Notifications.isEnabled()) return
        Notifications.withFont { renderQueue(g) }
    }

    private fun renderQueue(g: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val lineHeight = Font.getFont().lineHeight
        val width = Notifications.width.value
        val radius = Notifications.radius

        synchronized(queue) {
            queue.removeAll { it.expired }
            if (queue.isEmpty()) return

            val runtime = UiRuntime(MinecraftUiRenderer(g))
            var bottomY = sh - MARGIN

            for (notif in queue.reversed()) {
                val h = notif.height(lineHeight)
                val targetY = (bottomY - h).toFloat()
                bottomY -= h + GAP

                if (notif.currentY.isNaN()) notif.currentY = targetY
                else notif.currentY += (targetY - notif.currentY) * 0.35f

                val eased = notif.slideProgress
                val x = (sw - MARGIN - width + (width + MARGIN) * (1f - eased)).roundToInt()
                val y = notif.currentY.roundToInt()
                val background = panelColor(eased)

                // Drawn here rather than by the template so toasts share the same blur and
                // shadow primitives, and settings, as every other panel in the client.
                Notifications.panelShadow.draw(g, x, y, width, h, radius)
                if (Notifications.backgroundMode.value == HudBackgroundMode.BLUR) {
                    HudBlur.draw(g, x, y, width, h, radius, background, Notifications.blurStrength.value)
                } else {
                    g.roundedFill(x, y, width, h, radius, background)
                }

                val document = notif.document(x, y, width, h, radius, eased)
                runtime.layout(document, UiRect(0f, 0f, sw.toFloat(), sh.toFloat()))
                runtime.render(document, -1f, -1f)
            }
        }
    }

    private fun Notif.document(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        radius: Int,
        eased: Float,
    ): UiDocument {
        val accent = accentFor(kind)
        val messageColor = if (kind == Kind.CONFIG) 0xFFF4F4FA.toInt() else accent
        val textAlpha = (255 * eased).roundToInt().coerceIn(0, 255)
        val mutedAlpha = (176 * eased).roundToInt().coerceIn(0, 255)
        val showProgress = Notifications.progressBar.value
        val trackW = if (showProgress) w - 18 else 0
        val progressW = (trackW * (1f - lifeProgress)).roundToInt().coerceAtLeast(0)

        val values = mapOf(
            "toast.x" to x.toString(),
            "toast.y" to y.toString(),
            "toast.w" to w.toString(),
            "toast.h" to h.toString(),
            "toast.radius" to radius.toString(),
            "toast.textW" to (w - 22).coerceAtLeast(0).toString(),
            "toast.title" to title,
            "toast.message" to message,
            "toast.messageH" to if (hasMessage) "9" else "0",
            "toast.bg" to hex(0),
            "toast.messageColor" to hex(withAlpha(messageColor, textAlpha)),
            "toast.progressColor" to hex(withAlpha(accent, textAlpha)),
            "toast.text" to hex(withAlpha(0xFFF4F4FA.toInt(), textAlpha)),
            "toast.messageFg" to hex(withAlpha(0xFFE0E0EA.toInt(), mutedAlpha)),
            "toast.progressTrack" to hex(withAlpha(0xFFFFFFFF.toInt(), (24 * eased).roundToInt())),
            "toast.trackW" to trackW.toString(),
            "toast.progressW" to progressW.toString(),
            "toast.progressH" to if (showProgress) "2" else "0",
            "toast.progressY" to (h - 4).coerceAtLeast(0).toString(),
        )

        return UiDocument(HudUiResources.templates().instantiate("notification-toast", values))
            .validate("notification toast UI")
    }

    private fun accentFor(kind: Kind): Int =
        when (kind) {
            Kind.INFO -> Colour.accent.liveColor(Colour.accent.value).argb
            Kind.CONFIG -> Colour.accent.liveColor(Colour.accent.value).argb
            Kind.ENABLED -> 0xFF42F58D.toInt()
            Kind.DISABLED -> 0xFFFF5C72.toInt()
            Kind.ERROR -> 0xFFFF4040.toInt()
        }

    private fun panelColor(eased: Float): Int {
        val theme = Colour.accent.liveColor(Colour.accent.value)
        val configured = Notifications.bgColor.liveColor(theme).argb
        val alpha = (((configured ushr 24) and 0xFF) * eased).roundToInt().coerceIn(0, 255)
        return withAlpha(configured, alpha)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

    private fun hex(argb: Int): String =
        "#%08X".format(argb)

    private fun easeOutBack(tRaw: Float): Float {
        val t = tRaw.coerceIn(0f, 1f)
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return (1f + c3 * (t - 1f) * (t - 1f) * (t - 1f) + c1 * (t - 1f) * (t - 1f))
            .coerceIn(0f, 1f)
    }
}
