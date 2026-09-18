package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiNode
import onl.luka.grizzly.gui.ui.UiTemplateSet
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.LivingEntity

/**
 * Everything a design needs for one frame. Sizes are asked for outside of rendering too,
 * so [TargetHudDesign.width] and [TargetHudDesign.height] take the target on its own.
 */
class TargetHudContext(
    val g: GuiGraphicsExtractor,
    val target: LivingEntity,
    val templates: UiTemplateSet,
    val background: Int,
    val accent: Int,
    val accentEnd: Int,
    val textShadow: Boolean,
    val blurBackground: Boolean,
    val absX: Float,
    val absY: Float,
    val scale: Float,
    val ghostFraction: Float,
) {
    val font get() = Font.getFont()
    val shadow: String get() = textShadow.toString()

    val healthFraction: Float
        get() = (target.health / target.maxHealth.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
}

interface TargetHudDesign {
    fun width(target: LivingEntity?): Int

    fun height(target: LivingEntity?): Int

    fun document(ctx: TargetHudContext): UiDocument

    /** Live entity renders have to escape the HUD matrix, so those designs place themselves. */
    val rendersEntity: Boolean get() = false

    /** Grizzly's timed pop-in, which also keeps the panel parked while it fades out. */
    val timedAnimation: Boolean get() = false

    /** Scale the panel grows from when it appears. */
    val popInScale: Float get() = 0.55f

    /** Whether the drop shadow and blur background settings apply. */
    val panelEffects: Boolean get() = false

    val animatesWidth: Boolean get() = false

    val usesAccentEnd: Boolean get() = true

    fun renderNode(ctx: TargetHudContext, node: UiNode): Boolean = false
}
