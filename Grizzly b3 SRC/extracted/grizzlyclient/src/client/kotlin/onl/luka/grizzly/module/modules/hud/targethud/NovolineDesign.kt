package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.world.entity.LivingEntity

object NovolineDesign : TargetHudDesign {

    private const val HEIGHT = 42
    private const val BASE_WIDTH = 74
    private const val COLUMN_X = 44
    private const val BAR_BASE_WIDTH = 26

    private const val BAR_TRACK = 0x96151515.toInt()

    override val animatesWidth = true

    override val usesAccentEnd = false

    private fun nameWidth(target: LivingEntity?): Int =
        target?.let { Font.getFont().width(Font.styledText(it.name.string)) } ?: 0

    override fun width(target: LivingEntity?): Int = BASE_WIDTH + nameWidth(target)

    override fun height(target: LivingEntity?): Int = HEIGHT

    override fun document(ctx: TargetHudContext): UiDocument {
        val barWidth = BAR_BASE_WIDTH + nameWidth(ctx.target)
        val fraction = ctx.healthFraction

        // The trailing bar lags behind so a hit leaves a visible slice of the old health.
        val ghost = maxOf(fraction, ctx.ghostFraction)

        return UiDocument(
            ctx.templates.instantiate(
                "target-hud-novoline",
                mapOf(
                    "hud.width" to width(ctx.target).toString(),
                    "hud.shadow" to ctx.shadow,
                    "target.name" to ctx.target.name.string,
                    "target.hurt" to TargetHudParts.damagePulse(ctx.target).toString(),
                    "text.width" to (width(ctx.target) - COLUMN_X - 4).toString(),
                    "bar.width" to barWidth.toString(),
                    "bar.fillW" to (barWidth * fraction).toInt().toString(),
                    "bar.ghostW" to (barWidth * ghost).toInt().toString(),
                    "bar.track" to BAR_TRACK.xmlColor(),
                    "accent.color" to ctx.accent.xmlColor(),
                    "accent.trail" to TargetHudParts.darken(ctx.accent, 0.5f).xmlColor(),
                    "hp.percent" to "%.1f%%".format(fraction * 100f),
                ),
            ),
        )
    }
}
