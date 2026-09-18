package onl.luka.grizzly.module.modules.hud.targethud

import onl.luka.grizzly.gui.ui.UiNode
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.Text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.roundToInt

/** Draws and colour maths shared by more than one target HUD design. */
object TargetHudParts {

    /** Handles the custom XML node types the designs share. */
    fun renderNode(ctx: TargetHudContext, node: UiNode): Boolean = when (node.type) {
        "target-face" -> {
            playerFace(
                ctx.g,
                ctx.target,
                node.bounds.x.roundToInt(),
                node.bounds.y.roundToInt(),
                node.bounds.width.roundToInt(),
                node.style.radius.roundToInt(),
                damagePulse(ctx.target),
            )
            true
        }
        "target-entity" -> {
            ctx.g.pose().popMatrix()
            entityAvatar(ctx, node.bounds, node.attributes["entityScale"]?.toFloatOrNull() ?: 26f)
            ctx.g.pose().pushMatrix()
            ctx.g.pose().translate(ctx.absX, ctx.absY)
            if (ctx.scale != 1f) ctx.g.pose().scale(ctx.scale, ctx.scale)
            true
        }
        "target-equipment" -> {
            equipment(ctx.g, ctx.target, node.bounds)
            true
        }
        else -> false
    }

    fun damagePulse(entity: LivingEntity): Float {
        if (entity.hurtTime <= 0) return 0f
        return (entity.hurtTime / entity.hurtDuration.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    }

    fun Int.xmlColor(): String = "#%08X".format(this)

    fun lerpColor(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun darken(color: Int, factor: Float): Int {
        fun channel(shift: Int): Int =
            (((color ushr shift) and 0xFF) * factor).roundToInt().coerceIn(0, 255)
        return (color and 0xFF000000.toInt()) or
            (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

    fun compositeOver(top: Int, base: Int): Int {
        val alpha = ((top ushr 24) and 0xFF) / 255f
        fun channel(shift: Int): Int {
            val t = (top ushr shift) and 0xFF
            val b = (base ushr shift) and 0xFF
            return (b + (t - b) * alpha).roundToInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** Three step red to green ramp used by the bucketed designs. */
    fun healthBucketColor(entity: LivingEntity): Int {
        val pct = entity.health / entity.maxHealth.coerceAtLeast(0.001f)
        return when {
            pct > 0.6f -> 0xFF55FF55.toInt()
            pct > 0.3f -> 0xFFFFAA00.toInt()
            else       -> 0xFFFF5555.toInt()
        }
    }

    /** Smooth red to yellow to green ramp, brightened the way Exhibition draws it. */
    fun healthGradientColor(fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        val blended = if (t < 0.5f) {
            lerpColor(0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), t * 2f)
        } else {
            lerpColor(0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), (t - 0.5f) * 2f)
        }
        fun channel(shift: Int): Int =
            (((blended ushr shift) and 0xFF) * 1.25f).roundToInt().coerceIn(0, 255)
        return 0xFF000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun readableTextColor(argb: Int): Int =
        if (isLightColor(argb)) 0xFF171717.toInt() else 0xFFD7D7E4.toInt()

    fun mutedTextColor(argb: Int): Int =
        if (isLightColor(argb)) 0xFF565656.toInt() else 0xFFAAAAAA.toInt()

    fun barBackColor(argb: Int): Int =
        if (isLightColor(argb)) 0xFF1B1B1B.toInt() else 0xFF202020.toInt()

    private fun isLightColor(argb: Int): Boolean {
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) > 150.0
    }

    /** Truncates [name] by pixel width so it fits within [availW]. */
    fun fitName(name: String, availW: Int, font: net.minecraft.client.gui.Font): String {
        if (availW <= 0 || font.width(Font.styledText(name)) <= availW) return name
        var s = name
        while (s.isNotEmpty() && font.width(Font.styledText("$s…")) > availW) s = s.dropLast(1)
        return if (s.length < name.length) "$s…" else name
    }

    fun entityAvatar(ctx: TargetHudContext, bounds: UiRect, entityScale: Float) {
        val sx0 = (ctx.absX + bounds.x * ctx.scale).roundToInt()
        val sy0 = (ctx.absY + bounds.y * ctx.scale).roundToInt()
        val sx1 = (ctx.absX + bounds.right * ctx.scale).roundToInt()
        val sy1 = (ctx.absY + bounds.bottom * ctx.scale).roundToInt()
        val state = Minecraft.getInstance().entityRenderDispatcher.extractEntity(ctx.target, 1f)
        state.shadowPieces.clear()
        state.outlineColor = 0
        state.nameTag = null
        state.scoreText = null
        state.displayFireAnimation = false

        if (state is LivingEntityRenderState) {
            state.bodyRot = 180f
            state.yRot = 0f
            state.xRot = 0f
            if (state.scale != 0f) {
                state.boundingBoxWidth /= state.scale
                state.boundingBoxHeight /= state.scale
                state.scale = 1f
            }
        }

        ctx.g.entity(
            state,
            entityScale * ctx.scale,
            Vector3f(0f, state.boundingBoxHeight / 2f, 0f),
            Quaternionf().rotateZ(Math.PI.toFloat()),
            Quaternionf(),
            sx0,
            sy0,
            sx1,
            sy1,
        )
    }

    fun playerFace(
        g: GuiGraphicsExtractor,
        tgt: LivingEntity,
        x: Int,
        y: Int,
        size: Int,
        radius: Int,
        hurtPulse: Float,
    ) {
        val texture = when (tgt) {
            is AbstractClientPlayer -> tgt.skin.body().texturePath()
            is Player -> DefaultPlayerSkin.get(tgt.gameProfile).body().texturePath()
            else -> DefaultPlayerSkin.getDefaultTexture()
        }
        faceLayer(g, texture, x, y, size, radius, 8f, 8f)
        faceLayer(g, texture, x, y, size, radius, 40f, 8f)
        if (hurtPulse > 0f) {
            val alpha = (75 * hurtPulse).roundToInt().coerceIn(0, 90)
            roundedFill(g, x, y, size, radius, (alpha shl 24) or 0x00FF3030)
        }
    }

    private fun faceLayer(
        g: GuiGraphicsExtractor,
        texture: Identifier,
        x: Int,
        y: Int,
        size: Int,
        radius: Int,
        u: Float,
        v: Float,
    ) {
        if (radius <= 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, size, size, 8, 8, 64, 64)
            return
        }
        for (row in 0 until size) {
            val inset = rowInset(row, size, radius)
            g.enableScissor(x + inset, y + row, x + size - inset, y + row + 1)
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, size, size, 8, 8, 64, 64)
            g.disableScissor()
            faceEdgePixel(g, texture, x, y, size, radius, row, u, v)
        }
    }

    private fun rowInset(row: Int, size: Int, radius: Int): Int {
        val edgeDistance = when {
            row < radius -> radius - row - 0.5
            row >= size - radius -> row - (size - radius) + 0.5
            else -> return 0
        }
        val keepWidth = Math.sqrt(radius * radius - edgeDistance * edgeDistance)
        return Math.ceil(radius - keepWidth).toInt().coerceIn(0, radius)
    }

    private fun edgeCoverage(row: Int, size: Int, radius: Int): Pair<Int, Float>? {
        val edgeDistance = when {
            row < radius -> radius - row - 0.5
            row >= size - radius -> row - (size - radius) + 0.5
            else -> return null
        }
        val boundary = radius - Math.sqrt(radius * radius - edgeDistance * edgeDistance)
        val edgePixel = kotlin.math.floor(boundary).toInt().coerceIn(0, radius - 1)
        val coverage = (edgePixel + 1 - boundary).toFloat().coerceIn(0f, 1f)
        return edgePixel to coverage
    }

    private fun faceEdgePixel(
        g: GuiGraphicsExtractor,
        texture: Identifier,
        x: Int,
        y: Int,
        size: Int,
        radius: Int,
        row: Int,
        u: Float,
        v: Float,
    ) {
        val (edgePixel, coverage) = edgeCoverage(row, size, radius) ?: return
        val alpha = (255 * coverage).roundToInt().coerceIn(0, 255)
        if (alpha <= 0 || alpha >= 255) return
        val tint = (alpha shl 24) or 0x00FFFFFF
        g.enableScissor(x + edgePixel, y + row, x + edgePixel + 1, y + row + 1)
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, size, size, 8, 8, 64, 64, tint)
        g.disableScissor()
        g.enableScissor(x + size - edgePixel - 1, y + row, x + size - edgePixel, y + row + 1)
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, size, size, 8, 8, 64, 64, tint)
        g.disableScissor()
    }

    private fun roundedFill(g: GuiGraphicsExtractor, x: Int, y: Int, size: Int, radius: Int, color: Int) {
        for (row in 0 until size) {
            val inset = rowInset(row, size, radius)
            g.fill(x + inset, y + row, x + size - inset, y + row + 1, color)
        }
    }

    fun equipment(g: GuiGraphicsExtractor, tgt: LivingEntity, bounds: UiRect) {
        val stacks = equipmentStacks(tgt)
        if (stacks.isEmpty()) return
        val x0 = bounds.x.roundToInt()
        val y = bounds.y.roundToInt()

        for ((index, entry) in stacks.withIndex()) {
            val x = x0 + index * 16
            if (x + 16 > bounds.right.roundToInt()) break
            g.item(entry.stack, x, y)
            g.itemDecorations(Font.getFont(), entry.stack, x, y)
            drawEnchantTags(g, entry, x, y)
        }
    }

    private fun drawEnchantTags(g: GuiGraphicsExtractor, entry: EquipmentEntry, x: Int, y: Int) {
        val tags = enchantTags(entry)
        if (tags.isEmpty()) return
        val font = Font.getFont()
        val scale = 0.5f
        Font.withRenderScale(scale) {
            g.pose().pushMatrix()
            g.pose().translate(x.toFloat(), y.toFloat())
            g.pose().scale(scale, scale)
            for ((row, tag) in tags.withIndex()) {
                g.Text(font, Font.styledText(tag.text), 0, row * (font.lineHeight + 1), tag.color, true)
            }
            g.pose().popMatrix()
        }
    }

    private fun enchantTags(entry: EquipmentEntry): List<EnchantTag> {
        val stack = entry.stack
        val tags = mutableListOf<EnchantTag>()

        fun add(prefix: String, key: ResourceKey<Enchantment>) {
            val level = enchantLevel(stack, key)
            if (level > 0) tags += EnchantTag("$prefix$level", enchantLevelColor(level))
        }

        when {
            entry.armor -> {
                add("P", Enchantments.PROTECTION)
                add("T", Enchantments.THORNS)
                add("U", Enchantments.UNBREAKING)
            }
            stack.`is`(ItemTags.SWORDS) -> {
                add("S", Enchantments.SHARPNESS)
                add("F", Enchantments.FIRE_ASPECT)
                add("K", Enchantments.KNOCKBACK)
                add("U", Enchantments.UNBREAKING)
            }
            stack.`is`(Items.BOW) || stack.`is`(Items.CROSSBOW) -> {
                add("Pow", Enchantments.POWER)
                add("Pun", Enchantments.PUNCH)
                add("F", Enchantments.FLAME)
            }
            stack.rarity == Rarity.EPIC -> tags += EnchantTag("God", 0xFFFFFF55.toInt())
        }
        return tags
    }

    private fun enchantLevelColor(level: Int): Int = when {
        level >= 5 -> 0xFFFF5555.toInt()
        level >= 4 -> 0xFFFFAA00.toInt()
        level >= 3 -> 0xFFFFFF55.toInt()
        else       -> 0xFFFFFFFF.toInt()
    }

    private fun enchantLevel(stack: ItemStack, key: ResourceKey<Enchantment>): Int {
        val level = Minecraft.getInstance().level ?: return 0
        return try {
            stack.enchantments.getLevel(level.registryAccess().getOrThrow(key))
        } catch (_: Exception) {
            0
        }
    }

    private fun equipmentStacks(tgt: LivingEntity): List<EquipmentEntry> {
        val entries = mutableListOf<EquipmentEntry>()
        for (slot in listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            val stack = tgt.getItemBySlot(slot)
            if (!stack.isEmpty) entries += EquipmentEntry(stack, armor = true)
        }
        val held = tgt.mainHandItem
        if (!held.isEmpty) entries += EquipmentEntry(held, armor = false)
        return entries
    }

    private data class EquipmentEntry(val stack: ItemStack, val armor: Boolean)

    private data class EnchantTag(val text: String, val color: Int)
}
