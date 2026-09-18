package onl.luka.grizzly.command

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import onl.luka.grizzly.util.NotificationManager

object CommandFeedback {

    private val messageColor = 0xFF9E9E9E.toInt()

    @JvmStatic
    fun chat(text: String) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) {
            NotificationManager.show("Command", text)
            return
        }
        mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text).withColor(messageColor))
    }
}
