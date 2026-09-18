package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity

object ExhibitionDesign : TargetHudDesign {

    private const val BEZEL = 3
    private const val PANEL_H = 40
    private const val MIN_PANEL_W = 120
    private const val MIN_BAR_W = 60
    private const val PANEL_FILL = 0xFF161616.toInt()

    override val rendersEntity = true

    override fun width(target: LivingEntity?): Int = panelWidth(target) + BEZEL * 2

    override fun height(target: LivingEntity?): Int = PANEL_H + BEZEL * 2

    private fun panelWidth(target: LivingEntity?): Int {
        target ?: return MIN_PANEL_W
        val nameWidth = Font.getFont().width(Font.styledText(target.name.string))
        return maxOf(MIN_PANEL_W, 40 + nameWidth)
    }

    override fun document(ctx: TargetHudContext): UiDocument {
        val tgt = ctx.target
        val panelW = panelWidth(tgt)
        val columnX = BEZEL + 39
        val barBoxW = (panelW - 39 - 3).coerceAtLeast(MIN_BAR_W + 2)
        val barW = barBoxW - 2

        val health = tgt.health.coerceAtLeast(0f)
        val absorption = tgt.absorptionAmount.coerceAtLeast(0f)
        val total = (tgt.maxHealth + absorption).coerceAtLeast(0.001f)
        val healthW = (barW * (health / total)).toInt().coerceIn(0, barW)
        val absorptionW = (barW * (absorption / total)).toInt().coerceIn(0, barW - healthW)
        val hpColor = TargetHudParts.healthGradientColor(ctx.healthFraction)

        val distance = Minecraft.getInstance().player?.distanceTo(tgt)?.toInt() ?: 0

        val values = mapOf(
            "hud.width" to (panelW + BEZEL * 2).toString(),
            "bezel.width" to (panelW + BEZEL * 2 - 2).toString(),
            "panel.width" to panelW.toString(),
            // The bezel sits behind the panel, so a translucent colour is composited onto the
            // original's dark fill instead of letting the bezel grey bleed through.
            "hud.background" to TargetHudParts.compositeOver(ctx.background, PANEL_FILL).xmlColor(),
            "hud.text" to 0xFFFFFFFF.toInt().xmlColor(),
            "hud.shadow" to ctx.shadow,
            "target.name" to tgt.name.string,
            "text.width" to (panelW - 41).toString(),
            "hp.boxW" to barBoxW.toString(),
            "hp.trackW" to barW.toString(),
            "hp.fillW" to healthW.toString(),
            "hp.color" to hpColor.xmlColor(),
            "hp.dim" to TargetHudParts.withAlpha(hpColor, 35).xmlColor(),
            "abs.x" to (columnX + 1 + healthW).toString(),
            "abs.width" to absorptionW.toString(),
            "info.text" to "HP: ${health.toInt()} | Dist: $distance",
            "equip.width" to (panelW - 38).toString(),
        )

        // Ten equal segments, matching the notched bar the original draws.
        val ticks = (1 until 10).map { index ->
            ctx.templates.instantiate(
                "target-hud-exhibition-tick",
                mapOf("tick.x" to (columnX + 1 + barW * index / 10).toString()),
            )
        }

        return UiDocument(
            ctx.templates.instantiate("target-hud-exhibition", values, mapOf("ticks" to ticks)),
        )
    }
}
