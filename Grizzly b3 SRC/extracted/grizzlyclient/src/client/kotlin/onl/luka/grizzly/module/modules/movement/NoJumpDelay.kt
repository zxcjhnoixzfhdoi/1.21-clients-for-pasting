package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.mixin.client.LivingEntityAccessor
import net.minecraft.client.Minecraft

object NoJumpDelay : Module(
    name = "No Jump Delay",
    description = "Removes the jump cooldown. Works mostly for head hitters if you hold space and run down a 1x2 corridor",
    category = Category.MOVEMENT,
) {
    override fun onTick(client: Minecraft) {
        if (client.player == null) return
        (client.player as LivingEntityAccessor).setNoJumpDelay(0)
    }
}
