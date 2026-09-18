package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.KeybindEntry
import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object ClickLock : Module(
    "Click Lock",
    "Holds the mining button until its own toggle key is pressed again",
    Category.SKYBLOCK,
) {
    private val toggleKey = keybind("toggle key", KeybindEntry.mouseButtonCode(GLFW.GLFW_MOUSE_BUTTON_4))
        .onPress {
            if (isEnabled()) {
                active = !active
                applyAttackState(Minecraft.getInstance())
            }
        }
    private val releaseOnScreen = boolean("release in menus", true)

    var active: Boolean = false
        private set
    private var suspended = false

    override fun onDisabled() {
        active = false
        suspended = false
        Minecraft.getInstance().options.keyAttack.setDown(false)
    }

    override fun onTick(client: Minecraft) {
        applyAttackState(client)
    }

    override fun hudInfo(): String = if (active) "Locked" else ""

    fun suspendForAbility(value: Boolean) {
        suspended = value
        applyAttackState(Minecraft.getInstance())
    }

    internal fun wantsAttack(client: Minecraft): Boolean {
        val screenOpen = client.gui.screen() != null
        return active && !suspended && (!releaseOnScreen.value || !screenOpen)
    }

    private fun applyAttackState(client: Minecraft) {
        client.options.keyAttack.setDown(wantsAttack(client))
    }
}
