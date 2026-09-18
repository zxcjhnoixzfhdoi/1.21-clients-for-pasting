package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module

object Colour : Module("Color", "Global accent color for GUI and HUD elements", Category.OTHER) {

    override val isProtected = true
    override val showInModulesList = false
    init { enabled.value = true }

    val bg = color("bg", Color(9, 9, 9), allowAlpha = false)
    val accent = color("accent", Color(170, 20, 20), allowAlpha = false)
}
