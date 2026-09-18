package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.world.entity.LivingEntity

object AstolfoDesign : TargetHudDesign {

    private const val MIN_WIDTH = 110
    private const val HEIGHT = 45
    private const val COLUMN_X = 34
    private const val RIGHT_PAD = 4

    override val rendersEntity = true

    override fun width(target: LivingEntity?): Int {
        target ?: return MIN_WIDTH
        val nameWidth = Font.getFont().width(Font.styledText(target.name.string))
        return maxOf(MIN_WIDTH, nameWidth + 70)
    }

    override fun height(target: LivingEntity?): Int = HEIGHT

    override fun document(ctx: TargetHudContext): UiDocument {
        val panelW = width(ctx.target)
        val barW = panelW - COLUMN_X - RIGHT_PAD
        val fraction = ctx.healthFraction
        val ghost = maxOf(fraction, ctx.ghostFraction)

        return UiDocument(
            ctx.templates.instantiate(
                "target-hud-astolfo",
                mapOf(
                    "hud.width" to panelW.toString(),
                    "hud.background" to ctx.background.xmlColor(),
                    "hud.shadow" to ctx.shadow,
                    "target.name" to ctx.target.name.string,
                    "text.width" to (panelW - COLUMN_X - RIGHT_PAD).toString(),
                    "bar.width" to barW.toString(),
                    "bar.fillW" to (barW * fraction).toInt().toString(),
                    "bar.ghostW" to (barW * ghost).toInt().toString(),
                    "accent.from" to ctx.accent.xmlColor(),
                    "accent.to" to ctx.accentEnd.xmlColor(),
                    "accent.ghostFrom" to TargetHudParts.darken(ctx.accent, 0.6f).xmlColor(),
                    "accent.ghostTo" to TargetHudParts.darken(ctx.accentEnd, 0.6f).xmlColor(),
                    "accent.trackFrom" to TargetHudParts.darken(ctx.accent, 0.25f).xmlColor(),
                    "accent.trackTo" to TargetHudParts.darken(ctx.accentEnd, 0.25f).xmlColor(),
                    "hp.value" to "%.1f ❤".format(ctx.target.health.coerceAtLeast(0f)),
                ),
            ),
        )
    }
}
