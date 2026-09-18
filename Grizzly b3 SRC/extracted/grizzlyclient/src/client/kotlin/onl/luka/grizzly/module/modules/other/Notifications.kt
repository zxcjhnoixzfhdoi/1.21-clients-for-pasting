package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur

object Notifications : Module("Notifications", "Style the client's toast popups", Category.OTHER) {

    override val showInModulesList = false
    override val showKeybindNotifications = false

    init { enabled.value = true }

    val fontOverride = registerFontOverride()
    val width = int("width", 176, 120, 320)
    val rounded = boolean("rounded", true)
    val roundRadius = int("round radius", 8, 1, 16).also {
        it.visibleWhen = { rounded.value }
    }
    val backgroundMode = enum("background mode", HudBackgroundMode.SOLID)
    val blurStrength = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { backgroundMode.value == HudBackgroundMode.BLUR }
    }
    val bgColor = color("bg color", Color(18, 19, 27, 222), allowAlpha = true)
    val progressBar = boolean("progress bar", true)
    val panelShadow = hudDropShadow()

    val radius: Int get() = if (rounded.value) roundRadius.value else 0
}
