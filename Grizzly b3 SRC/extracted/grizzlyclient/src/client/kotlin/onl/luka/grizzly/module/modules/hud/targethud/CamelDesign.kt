package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import net.minecraft.world.entity.LivingEntity
import kotlin.math.ceil

object CamelDesign : TargetHudDesign {

    private const val WIDTH = 122
    private const val HEIGHT = 58
    private const val NAME_SCALE = 1.33f
    private const val BAR_WIDTH = 77

    override val rendersEntity = true

    override fun width(target: LivingEntity?): Int = WIDTH

    override fun height(target: LivingEntity?): Int = HEIGHT

    override fun document(ctx: TargetHudContext): UiDocument {
        val bg = ctx.background
        val name = TargetHudParts.fitName(
            ctx.target.name.string,
            ((WIDTH - 36f - 5f) / NAME_SCALE).toInt(),
            ctx.font,
        )

        return UiDocument(
            ctx.templates.instantiate(
                "target-hud-camel",
                mapOf(
                    "hud.background" to bg.xmlColor(),
                    "hud.text" to TargetHudParts.readableTextColor(bg).xmlColor(),
                    "hud.barBack" to TargetHudParts.barBackColor(bg).xmlColor(),
                    "hud.shadow" to ctx.shadow,
                    "target.name" to name,
                    "hp.fillW" to (BAR_WIDTH * ctx.healthFraction).toInt().toString(),
                    "hp.whole" to ceil(ctx.target.health.toDouble()).toInt().coerceAtLeast(0).toString(),
                ),
            ),
        )
    }
}
