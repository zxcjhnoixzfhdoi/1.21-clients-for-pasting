package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import net.minecraft.world.entity.LivingEntity

object WurstDesign : TargetHudDesign {

    private const val WIDTH = 185
    private const val HEIGHT = 34
    private const val PAD = 4

    override fun width(target: LivingEntity?): Int = WIDTH

    override fun height(target: LivingEntity?): Int = HEIGHT

    override fun document(ctx: TargetHudContext): UiDocument {
        val barWidth = WIDTH - PAD * 2

        return UiDocument(
            ctx.templates.instantiate(
                "target-hud-wurst",
                mapOf(
                    "hud.width" to WIDTH.toString(),
                    "hud.shadow" to ctx.shadow,
                    "target.name" to "Name: ${ctx.target.name.string}",
                    "text.width" to barWidth.toString(),
                    "bar.fillW" to (barWidth * ctx.healthFraction).toInt().toString(),
                ),
            ),
        )
    }
}
