package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts.xmlColor
import onl.luka.grizzly.module.modules.other.Font
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.roundToInt

object GrizzlyDesign : TargetHudDesign {

    override val timedAnimation = true
    override val popInScale = 0.08f
    override val panelEffects = true

    override fun width(target: LivingEntity?): Int = 150

    override fun height(target: LivingEntity?): Int {
        val lineHeight = Font.getFont().lineHeight
        var rows = 3
        if ((target?.absorptionAmount ?: 0f) > 0f) rows++
        return (2 * lineHeight + 11) + (rows - 1) * (lineHeight + 1)
    }

    override fun document(ctx: TargetHudContext): UiDocument {
        val templates = ctx.templates
        val tgt = ctx.target
        val font = ctx.font
        val bg = ctx.background
        val text = TargetHudParts.readableTextColor(bg)
        val muted = TargetHudParts.mutedTextColor(bg)
        val barBack = TargetHudParts.barBackColor(bg)

        val h = height(tgt)
        val rowStep = font.lineHeight + 1
        val player = Minecraft.getInstance().player
        val hpValStr = "%.1f".format(tgt.health)
        val hpValW = font.width(Font.styledText(hpValStr)) + 4
        val hpLabelW = font.width(Font.styledText("HP"))
        val hpBarX = 54 + hpLabelW + 3
        val statRows = 2 +
            (if (tgt.absorptionAmount > 0f) 1 else 0) +
            (if (player != null) 1 else 0)
        var row = ((h - statRows * rowStep) / 2).coerceAtLeast(5 + font.lineHeight + 2)
        val hpBarW = (140 - hpBarX - hpValW).coerceAtLeast(0)
        val hpColor = TargetHudParts.healthBucketColor(tgt)
        val faceSize = 34
        val faceY = ((h - faceSize) / 2).coerceAtLeast(5)
        val pulse = TargetHudParts.damagePulse(tgt)
        val faceDrawSize = (faceSize * (1f + 0.1f * pulse)).roundToInt()

        val values = mutableMapOf(
            "hud.height" to h.toString(),
            "hud.background" to if (ctx.blurBackground) "#00000000" else bg.xmlColor(),
            "hud.text" to text.xmlColor(),
            "hud.muted" to muted.xmlColor(),
            "hud.barBack" to barBack.xmlColor(),
            "hud.shadow" to ctx.shadow,
            "target.name" to tgt.name.string,
            "target.hurt" to pulse.toString(),
            "face.x" to (10 - (faceDrawSize - faceSize) / 2).toString(),
            "face.y" to (faceY - (faceDrawSize - faceSize) / 2).toString(),
            "face.size" to faceDrawSize.toString(),
            "hp.y" to row.toString(),
            "hp.labelW" to hpLabelW.toString(),
            "hp.barX" to hpBarX.toString(),
            "hp.barY" to (row + 1).toString(),
            "hp.barW" to hpBarW.toString(),
            "hp.fillW" to (hpBarW * ctx.healthFraction).toInt().toString(),
            "hp.value" to hpValStr,
            "hp.valueX" to (143 - hpValW).toString(),
            "hp.valueW" to hpValW.toString(),
            "hp.color" to hpColor.xmlColor(),
        )
        row += rowStep

        val absorptionNodes = if (tgt.absorptionAmount > 0f) {
            val absLabelW = font.width(Font.styledText("Abs"))
            val absBarX = 54 + absLabelW + 3
            val absBarW = (140 - absBarX).coerceAtLeast(0)
            val absValues = values + mapOf(
                "abs.y" to row.toString(),
                "abs.labelW" to absLabelW.toString(),
                "abs.barX" to absBarX.toString(),
                "abs.barY" to (row + 1).toString(),
                "abs.barW" to absBarW.toString(),
                "abs.fillW" to (absBarW * (tgt.absorptionAmount / 20f).coerceIn(0f, 1f)).toInt().toString(),
            )
            row += rowStep
            listOf(templates.instantiate("target-hud-absorption", absValues))
        } else {
            emptyList()
        }

        values["distance.y"] = row.toString()
        values["distance.text"] = player?.let { "Dist: ${"%.1fm".format(it.distanceTo(tgt))}" }.orEmpty()
        if (player != null) row += rowStep

        val chance = player?.let { winChance(it, tgt) } ?: -1f
        values["win.y"] = row.toString()
        values["win.text"] = when {
            player == null -> ""
            chance >= 0 -> "Win: ${"%.0f".format(chance)}%"
            else -> "Win: ?"
        }
        values["win.color"] = when {
            chance < 0 -> muted
            chance >= 60 -> 0xFF55FF55.toInt()
            chance >= 40 -> 0xFFFFAA00.toInt()
            else -> 0xFFFF5555.toInt()
        }.xmlColor()

        return UiDocument(
            templates.instantiate("target-hud-grizzly", values, mapOf("absorption" to absorptionNodes)),
        )
    }

    private fun winChance(self: Player, enemy: LivingEntity): Float {
        if (self.maxHealth <= 0f || enemy.maxHealth <= 0f) return -1f

        val selfDmg = weaponDamage(self)
        val enemyDmg = weaponDamage(enemy)
        if (selfDmg <= 0f) return 0f

        val hitsToKillEnemy = kotlin.math.ceil((effectiveHp(enemy) / selfDmg).toDouble())
            .toLong().coerceAtLeast(1L)
        val hitsToKillSelf = if (enemyDmg > 0f) {
            kotlin.math.ceil((effectiveHp(self) / enemyDmg).toDouble()).toLong().coerceAtLeast(1L)
        } else {
            Long.MAX_VALUE / 2
        }

        val total = (hitsToKillEnemy + hitsToKillSelf).toFloat()
        return (hitsToKillSelf.toFloat() / total * 100f).coerceIn(0f, 100f)
    }

    private fun weaponDamage(entity: LivingEntity): Float {
        val stack = entity.mainHandItem
        if (!stack.isEmpty) {
            val mods = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS)
            if (mods != null) {
                var bonus = 0.0
                mods.forEach(net.minecraft.world.entity.EquipmentSlot.MAINHAND) { attr, modifier ->
                    if (attr == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE &&
                        modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    ) {
                        bonus += modifier.amount()
                    }
                }
                if (bonus > 0.0) return (1f + bonus.toFloat())
            }
        }
        return entity.attributes.getValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
            .toFloat().coerceAtLeast(1f)
    }

    private fun effectiveHp(entity: LivingEntity): Float {
        val armorReduction = 1f - (entity.armorValue * 0.04f).coerceIn(0f, 0.8f)
        return (entity.health + entity.absorptionAmount) / armorReduction
    }
}
