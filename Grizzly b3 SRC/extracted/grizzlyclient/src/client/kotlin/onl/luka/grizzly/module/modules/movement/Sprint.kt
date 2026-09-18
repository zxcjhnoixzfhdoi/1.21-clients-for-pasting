package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft

object Sprint : Module(
    name = "Sprint",
    description = "Automatically sprints",
    category = Category.MOVEMENT,
) {
    init { enabled.value = true }
    private val omnisprint = boolean("omnisprint", false)

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        if (omnisprint.value) {
            player.setSprinting(true)
        } else {
            client.options.keySprint.isDown = true
        }
    }
}
