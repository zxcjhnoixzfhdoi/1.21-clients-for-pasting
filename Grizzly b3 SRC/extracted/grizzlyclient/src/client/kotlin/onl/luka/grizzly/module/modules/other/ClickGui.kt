package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.gui.ClickGui
import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object ClickGui : Module("Click Gui", "The click GUI", Category.OTHER) {

    override val showInModulesList = false
    override val showKeybindNotifications = false

    private var screenInstance: ClickGui? = null


    val showDescriptions = boolean("show_descriptions", true)
    val showBackground   = boolean("show_background", true)
    val blurBackground   = boolean("blur_background", true).also {
        it.visibleWhen = { showBackground.value }
    }
    val resetLayout      = button("reset_layout", "Reset Layout") { ClickGui.resetPositions() }

    init {
        keybind.value = GLFW.GLFW_KEY_RIGHT_SHIFT
    }

    override fun onEnabled() {
        val mc = Minecraft.getInstance()
        val screen = screenInstance ?: ClickGui().also { screenInstance = it }
        if (mc.gui.screen() !== screen) mc.gui.setScreen(screen)
    }

    override fun onDisabled() {
        val mc = Minecraft.getInstance()
        if (mc.gui.screen() is ClickGui) mc.gui.setScreen(null)
    }
}
