package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.gui.ui.HudUiResources
import onl.luka.grizzly.gui.ui.MinecraftUiRenderer
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.gui.ui.UiRuntime
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.modules.hud.targethud.AstolfoDesign
import onl.luka.grizzly.module.modules.hud.targethud.CamelDesign
import onl.luka.grizzly.module.modules.hud.targethud.ExhibitionDesign
import onl.luka.grizzly.module.modules.hud.targethud.GrizzlyDesign
import onl.luka.grizzly.module.modules.hud.targethud.NovolineDesign
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudContext
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudDesign
import onl.luka.grizzly.module.modules.hud.targethud.WurstDesign
import onl.luka.grizzly.module.modules.hud.targethud.TargetHudParts
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt
import net.minecraft.world.entity.LivingEntity

object TargetHud : HudModule("Target HUD", "Displays target info and fight prediction when in combat") {

    enum class PositionMode { STATIC, FLOATING }
    enum class Design { GRIZZLY, CAMEL, EXHIBITION, NOVOLINE, ASTOLFO, WURST }

    private val positionMode = enum("position", PositionMode.FLOATING)
    private val design       = enum("design", Design.GRIZZLY)
    private val bgColor      = color("bg color", Color(0, 0, 0, 160), allowAlpha = true)
    private val backgroundMode = enum("background mode", HudBackgroundMode.SOLID).also {
        it.visibleWhen = { impl().panelEffects }
    }
    private val panelShadow = hudDropShadow { impl().panelEffects }
    private val blurStrength = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = {
            impl().panelEffects && backgroundMode.value == HudBackgroundMode.BLUR
        }
    }
    private val accentColor = color("accent color", Color(120, 90, 255), allowAlpha = false).also {
        it.visibleWhen = { usesAccent() }
    }
    private val accentColorEnd = color("accent color 2", Color(255, 90, 200), allowAlpha = false).also {
        it.visibleWhen = { usesAccent() && impl().usesAccentEnd }
    }
    private val textShadow   = boolean("text shadow", false)

    private const val LINGER_MS = 3000L
    private const val GRIZZLY_ENTER_MS = 260f
    private const val GRIZZLY_EXIT_MS = 300f

    private var target: LivingEntity? = null
    private var displayTarget: LivingEntity? = null
    private var lastHitTime = 0L

    private var smoothX = -1f
    private var smoothY = -1f
    private var floatingSide = 0
    private var renderAbsX = 0f
    private var renderAbsY = 0f
    private var renderScale = 1f
    private var hudAnim = 0f
    private var ghostFraction = 1f
    private var animatedWidth = -1f

    private fun impl(): TargetHudDesign = when (design.value) {
        Design.GRIZZLY -> GrizzlyDesign
        Design.CAMEL -> CamelDesign
        Design.EXHIBITION -> ExhibitionDesign
        Design.NOVOLINE -> NovolineDesign
        Design.ASTOLFO -> AstolfoDesign
        Design.WURST -> WurstDesign
    }

    private fun usesAccent(): Boolean =
        design.value == Design.NOVOLINE || design.value == Design.ASTOLFO

    override fun onEnabled() {
        target = null
        displayTarget = null
        smoothX = -1f
        smoothY = -1f
        floatingSide = 0
        hudAnim = 0f
        ghostFraction = 1f
        animatedWidth = -1f
    }

    override fun onDisabled() {
        target = null
        displayTarget = null
        hudAnim = 0f
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val level  = client.level  ?: return
        val now = System.currentTimeMillis()

        val cur = target
        if (cur != null && (cur.isDeadOrDying || cur.isRemoved || player.distanceTo(cur) > 20f)) {
            target = null
        }

        val candidates = level.players()
            .filter { it !== player && !it.isDeadOrDying && player.distanceTo(it) <= 20f }
        if (candidates.isEmpty()) {
            target = null
            return
        }

        val lookVec = player.lookAngle
        val best = candidates.maxByOrNull { e ->
            val dx = e.x - player.x
            val dy = (e.y + e.bbHeight / 2) - player.eyeY
            val dz = e.z - player.z
            val len = Math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
            (lookVec.x * dx + lookVec.y * dy + lookVec.z * dz) / len
        }!!

        val bestDot = run {
            val dx = best.x - player.x
            val dy = (best.y + best.bbHeight / 2) - player.eyeY
            val dz = best.z - player.z
            val len = Math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
            (lookVec.x * dx + lookVec.y * dy + lookVec.z * dz) / len
        }
        val inView = bestDot > 0.5 || player.distanceTo(best) <= 4f

        if (inView) {
            if (target !== best) {
                smoothX = -1f
                smoothY = -1f
                floatingSide = 0
                ghostFraction = best.health / best.maxHealth.coerceAtLeast(0.001f)
            }
            target = best
            lastHitTime = now
        } else if (now - lastHitTime > LINGER_MS) {
            target = null
        }
    }

    override fun onHudRender(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        withFont { drawHud(extractor, delta) }
    }

    private fun drawHud(extractor: GuiGraphicsExtractor, delta: DeltaTracker) {
        val liveTarget = target
        if (liveTarget != null) displayTarget = liveTarget

        val tgt = displayTarget ?: return
        val design = impl()
        updateHudAnimation(delta, liveTarget != null, design)
        updateWidthAnimation(design, tgt)
        updateGhostHealth(tgt)
        if (hudAnim <= 0f && liveTarget == null) {
            displayTarget = null
            return
        }

        val mc  = Minecraft.getInstance()
        val sw  = mc.window.guiScaledWidth
        val sh  = mc.window.guiScaledHeight

        val sc = hudScale.value
        val curve = if (liveTarget != null) {
            easeOutBack(hudAnim)
        } else if (design.timedAnimation) {
            smootherStep(hudAnim)
        } else {
            easeInCubic(hudAnim)
        }
        val animScale = design.popInScale + (1f - design.popInScale) * curve

        when (positionMode.value) {
            PositionMode.STATIC -> {
                val px = (hudX.value * sw).toInt()
                val py = (hudY.value * sh).toInt()
                renderAbsX = px + hudWidth() * sc * (1f - animScale) * 0.5f
                renderAbsY = py + hudHeight() * sc * (1f - animScale) * 0.5f
                renderScale = sc * animScale
                if (design.rendersEntity) {
                    Font.withRenderScale(renderScale) { renderHudElement(extractor) }
                    return
                }
                extractor.pose().pushMatrix()
                extractor.pose().translate(px + hudWidth() * sc * 0.5f, py + hudHeight() * sc * 0.5f)
                extractor.pose().scale(sc * animScale, sc * animScale)
                extractor.pose().translate(-hudWidth() * 0.5f, -hudHeight() * 0.5f)
                Font.withRenderScale(renderScale) { renderHudElement(extractor) }
                extractor.pose().popMatrix()
            }
            PositionMode.FLOATING -> {
                val sp = projectToScreen(tgt, mc, 0.58)
                val w  = hudWidth()
                val h  = hudHeight()
                val swf = sw.toFloat()
                val shf = sh.toFloat()
                val margin = 6f

                val followTarget = liveTarget != null || !design.timedAnimation
                if (sp != null && followTarget) {
                    val scaledW = w * sc
                    val scaledH = h * sc
                    val sideGap = 12f
                    val hitboxPad = tgt.bbWidth.toDouble() * 0.55 + 0.08
                    val rightEdge = projectToScreen(tgt, mc, 0.58, hitboxPad)?.first ?: sp.first
                    val leftEdge = projectToScreen(tgt, mc, 0.58, -hitboxPad)?.first ?: sp.first
                    val rightX = maxOf(rightEdge, leftEdge) + sideGap
                    val leftX = minOf(rightEdge, leftEdge) - scaledW - sideGap
                    val canUseRight = rightX + scaledW <= swf - margin
                    val canUseLeft = leftX >= margin
                    if (floatingSide == 0) {
                        floatingSide = if (sp.first < swf * 0.5f) 1 else -1
                    }
                    if (floatingSide > 0 && !canUseRight && canUseLeft) floatingSide = -1
                    if (floatingSide < 0 && !canUseLeft && canUseRight) floatingSide = 1

                    var tx = when {
                        floatingSide > 0 && canUseRight -> rightX
                        floatingSide < 0 && canUseLeft -> leftX
                        canUseRight -> rightX
                        canUseLeft -> leftX
                        else -> sp.first - scaledW * 0.5f
                    }
                    var ty = sp.second - scaledH * 0.5f
                    tx = tx.coerceIn(margin, swf - w * sc - margin)
                    ty = ty.coerceIn(margin, shf - h * sc - margin)

                    if (smoothX < 0f) {
                        smoothX = tx; smoothY = ty
                    } else {
                        smoothX += (tx - smoothX) * 0.2f
                        smoothY += (ty - smoothY) * 0.2f
                    }
                }

                if (smoothX < 0f) return
                renderAbsX = smoothX + hudWidth() * sc * (1f - animScale) * 0.5f
                renderAbsY = smoothY + hudHeight() * sc * (1f - animScale) * 0.5f
                renderScale = sc * animScale
                if (design.rendersEntity) {
                    Font.withRenderScale(renderScale) { renderHudElement(extractor) }
                    return
                }
                extractor.pose().pushMatrix()
                extractor.pose().translate(smoothX + hudWidth() * sc * 0.5f, smoothY + hudHeight() * sc * 0.5f)
                extractor.pose().scale(sc * animScale, sc * animScale)
                extractor.pose().translate(-hudWidth() * 0.5f, -hudHeight() * 0.5f)
                Font.withRenderScale(renderScale) { renderHudElement(extractor) }
                extractor.pose().popMatrix()
            }
        }
    }

    private fun projectToScreen(
        entity: LivingEntity,
        mc: Minecraft,
        heightFactor: Double,
        cameraRightOffset: Double = 0.0,
    ): Pair<Float, Float>? {
        val camera = mc.gameRenderer.mainCamera()
        val camPos = camera.position()
        val yawRad = Math.toRadians(camera.yRot().toDouble())
        val rightX = -Math.cos(yawRad) * cameraRightOffset
        val rightZ = -Math.sin(yawRad) * cameraRightOffset

        val wx = entity.x + rightX - camPos.x
        val wy = entity.y + entity.bbHeight.toDouble() * heightFactor - camPos.y
        val wz = entity.z + rightZ - camPos.z

        val pitchRad = Math.toRadians(camera.xRot().toDouble())
        val sinYaw = Math.sin(yawRad);   val cosYaw = Math.cos(yawRad)
        val sinPit = Math.sin(pitchRad); val cosPit = Math.cos(pitchRad)

        val viewX = (-wx * cosYaw               - wz * sinYaw).toFloat()
        val viewZ = (-wx * sinYaw * cosPit - wy * sinPit + wz * cosYaw * cosPit).toFloat()
        val viewY = ( wx * sinYaw * sinPit - wy * cosPit - wz * cosYaw * sinPit).toFloat()

        if (viewZ <= 0.1f) return null  // behind camera

        val swf = mc.window.guiScaledWidth.toFloat()
        val shf = mc.window.guiScaledHeight.toFloat()
        val fovRad = Math.toRadians(mc.options.fov().get().toDouble())
        val f = (swf * 0.5f / Math.tan(fovRad / 2.0)).toFloat()

        return (swf / 2f + viewX / viewZ * f) to (shf / 2f + viewY / viewZ * f)
    }

    override fun hudWidth(): Int {
        val design = impl()
        val goal = design.width(displayTarget ?: target)
        if (!design.animatesWidth) return goal
        if (animatedWidth < 0f) animatedWidth = goal.toFloat()
        return animatedWidth.roundToInt()
    }

    override fun hudHeight(): Int = impl().height(displayTarget ?: target)

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        val tgt = displayTarget ?: target ?: return
        val design = impl()
        val theme = Colour.accent.liveColor(Colour.accent.value)
        val ctx = TargetHudContext(
            g = g,
            target = tgt,
            templates = HudUiResources.templates(),
            background = bgColor.liveColor(bgColor.value).argb,
            accent = accentColor.liveColor(theme).argb,
            accentEnd = accentColorEnd.liveColor(theme).argb,
            textShadow = textShadow.value,
            blurBackground = design.panelEffects && backgroundMode.value == HudBackgroundMode.BLUR,
            absX = renderAbsX,
            absY = renderAbsY,
            scale = renderScale,
            ghostFraction = ghostFraction,
        )

        val document = design.document(ctx)
        val runtime = UiRuntime(MinecraftUiRenderer(g)) { node ->
            TargetHudParts.renderNode(ctx, node) || design.renderNode(ctx, node)
        }

        if (design.rendersEntity) {
            g.pose().pushMatrix()
            g.pose().translate(renderAbsX, renderAbsY)
            if (renderScale != 1f) g.pose().scale(renderScale, renderScale)
        } else if (design.panelEffects) {
            panelShadow.draw(g, 0, 0, hudWidth(), hudHeight(), 5)
            if (ctx.blurBackground) {
                HudBlur.draw(g, 0, 0, hudWidth(), hudHeight(), 5, ctx.background, blurStrength.value)
            }
        }
        runtime.layout(document, UiRect(0f, 0f, hudWidth().toFloat(), hudHeight().toFloat()))
        runtime.render(document, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        if (design.rendersEntity) g.pose().popMatrix()
    }

    private fun updateWidthAnimation(design: TargetHudDesign, tgt: LivingEntity) {
        val goal = design.width(tgt).toFloat()
        animatedWidth = when {
            !design.animatesWidth || animatedWidth < 0f -> goal
            else -> animatedWidth + (goal - animatedWidth) * 0.25f
        }
    }

    // Trails the real value so a hit leaves the old health visible for a moment.
    private fun updateGhostHealth(tgt: LivingEntity) {
        val fraction = (tgt.health / tgt.maxHealth.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        ghostFraction = if (fraction > ghostFraction) {
            fraction
        } else {
            ghostFraction + (fraction - ghostFraction) * 0.12f
        }
    }

    private fun easeOutBack(value: Float): Float {
        val t = value.coerceIn(0f, 1f) - 1f
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * t * t * t + c1 * t * t
    }

    private fun easeInCubic(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * t
    }

    private fun smootherStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * t * (t * (t * 6f - 15f) + 10f)
    }

    private fun updateHudAnimation(delta: DeltaTracker, visible: Boolean, design: TargetHudDesign) {
        if (!design.timedAnimation) {
            val targetValue = if (visible) 1f else 0f
            hudAnim += (targetValue - hudAnim) * if (visible) 0.32f else 0.26f
            if (!visible && hudAnim <= 0.02f) hudAnim = 0f
            return
        }

        // Realtime delta keeps the animation independent of FPS and game timer changes.
        val elapsedMs = delta.realtimeDeltaTicks.coerceIn(0f, 3f) * 50f
        hudAnim = if (visible) {
            (hudAnim + elapsedMs / GRIZZLY_ENTER_MS).coerceAtMost(1f)
        } else {
            (hudAnim - elapsedMs / GRIZZLY_EXIT_MS).coerceAtLeast(0f)
        }
    }

    init {
        hudX.value = 0.5f
        hudX.defaultValue = .5f
        hudY.value = 0.7f
        hudY.defaultValue = .7f
    }
}
