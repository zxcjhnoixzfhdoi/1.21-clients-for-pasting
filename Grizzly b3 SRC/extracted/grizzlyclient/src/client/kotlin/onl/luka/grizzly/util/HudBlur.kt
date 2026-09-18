package onl.luka.grizzly.util

import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.Identifier
import onl.luka.grizzly.mixin.client.GuiGraphicsExtractorInvoker
import onl.luka.grizzly.mixin.client.RenderPipelinesAccessor
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class HudBackgroundMode { SOLID, BLUR }

object HudBlur {
    const val MAX_STRENGTH = 32

    private data class PipelineKey(val strength: Int, val tintAlpha: Int)
    private data class CornerScanline(
        val y: Int,
        val firstFull: Int,
        val aaIndex: Int,
        val coverage: Float,
    )

    private val pipelines = mutableMapOf<PipelineKey, RenderPipeline>()
    private val cornerCache = mutableMapOf<Int, List<CornerScanline>>()
    private var captureTarget: TextureTarget? = null
    private var requestedThisFrame = false
    private var captureActiveThisRender = false

    @JvmStatic
    fun captureFrame() {
        captureActiveThisRender = requestedThisFrame
        requestedThisFrame = false
        if (!captureActiveThisRender) return

        copyMainRenderTarget()
    }

    @JvmStatic
    fun refreshAfterVanillaBlur() {
        if (!captureActiveThisRender) return
        copyMainRenderTarget()
    }

    private fun copyMainRenderTarget() {
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val sourceTexture = main.colorTexture ?: return
        val capture = ensureCaptureTarget(main.width, main.height, sourceTexture.getFormat())
        val destinationTexture = capture.colorTexture ?: return
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            sourceTexture,
            destinationTexture,
            0,
            0,
            0,
            0,
            0,
            main.width,
            main.height,
        )
    }

    @JvmStatic
    fun close() {
        captureTarget?.destroyBuffers()
        captureTarget = null
        requestedThisFrame = false
        captureActiveThisRender = false
    }

    fun draw(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        tint: Int,
        strength: Int,
        corners: Int = CORNERS_ALL,
    ) {
        if (width <= 0 || height <= 0) return

        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val sourceTexture = main.colorTexture ?: return
        val capture = ensureCaptureTarget(main.width, main.height, sourceTexture.getFormat())
        val captureView = capture.colorTextureView ?: return
        requestedThisFrame = true

        val tintAlpha = (tint ushr 24) and 0xFF
        val pipeline = pipeline(strength.coerceIn(1, MAX_STRENGTH), tintAlpha)
        val textureSetup = TextureSetup.singleTexture(
            captureView,
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
        )
        drawRoundedSurface(g, pipeline, textureSetup, x, y, width, height, radius, tint, corners)
    }

    private fun ensureCaptureTarget(
        width: Int,
        height: Int,
        format: com.mojang.blaze3d.GpuFormat,
    ): TextureTarget {
        val current = captureTarget
        if (current == null || current.colorTexture?.getFormat() != format) {
            current?.destroyBuffers()
            return TextureTarget("Grizzly HUD blur", width, height, false, format).also {
                captureTarget = it
            }
        }
        if (current.width != width || current.height != height) current.resize(width, height)
        return current
    }

    private fun pipeline(strength: Int, tintAlpha: Int): RenderPipeline {
        val key = PipelineKey(strength, tintAlpha)
        return pipelines.getOrPut(key) {
            val location = Identifier.fromNamespaceAndPath(
                "medved",
                "hud_blur_${strength}_${tintAlpha}",
            )
            RenderPipelinesAccessor.`grizzly$register`(
                RenderPipeline.builder(RenderPipelinesAccessor.`grizzly$guiSnippet`())
                    .withLocation(location)
                    .withFragmentShader(Identifier.fromNamespaceAndPath("medved", "core/hud_blur"))
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withShaderDefine("BLUR_RADIUS", strength.toFloat())
                    .withShaderDefine("BLUR_TAPS", tapsFor(strength))
                    .withShaderDefine("TINT_ALPHA", tintAlpha / 255f)
                    .build(),
            )
        }
    }

    private fun tapsFor(strength: Int): Int =
        ceil(strength / 4.0).toInt().coerceIn(2, 6)

    private fun drawRoundedSurface(
        g: GuiGraphicsExtractor,
        pipeline: RenderPipeline,
        textureSetup: TextureSetup,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        tint: Int,
        corners: Int,
    ) {
        val rr = radius.coerceAtMost(minOf(width, height)).coerceAtLeast(0)
        if (rr == 0) {
            fill(g, pipeline, textureSetup, x, y, x + width, y + height, tint, 1f)
            return
        }

        val doTL = corners and CORNER_TL != 0
        val doTR = corners and CORNER_TR != 0
        val doBL = corners and CORNER_BL != 0
        val doBR = corners and CORNER_BR != 0

        fill(g, pipeline, textureSetup, x + rr, y, x + width - rr, y + height, tint, 1f)
        if (width >= 2 * rr) {
            val leftTop = if (doTL) y + rr else y
            val leftBottom = if (doBL) y + height - rr else y + height
            if (leftBottom > leftTop) {
                fill(g, pipeline, textureSetup, x, leftTop, x + rr, leftBottom, tint, 1f)
            }

            val rightTop = if (doTR) y + rr else y
            val rightBottom = if (doBR) y + height - rr else y + height
            if (rightBottom > rightTop) {
                fill(g, pipeline, textureSetup, x + width - rr, rightTop, x + width, rightBottom, tint, 1f)
            }
        } else {
            val top = if (doTL || doTR) y + rr else y
            val bottom = if (doBL || doBR) y + height - rr else y + height
            if (bottom > top) fill(g, pipeline, textureSetup, x, top, x + width, bottom, tint, 1f)
        }

        if (!doTL && !doTR && !doBL && !doBR) return
        drawCornerPixels(
            g,
            pipeline,
            textureSetup,
            x,
            y,
            width,
            height,
            rr,
            tint,
            doTL,
            doTR,
            doBL,
            doBR,
        )
    }

    private fun drawCornerPixels(
        g: GuiGraphicsExtractor,
        pipeline: RenderPipeline,
        textureSetup: TextureSetup,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        tint: Int,
        doTL: Boolean,
        doTR: Boolean,
        doBL: Boolean,
        doBR: Boolean,
    ) {
        val scale = Minecraft.getInstance().window.guiScale.toFloat().coerceAtLeast(1f)
        val radiusPx = (radius * scale).roundToInt().coerceAtLeast(1)
        val scanlines = cornerCache.getOrPut(radiusPx) { createCornerScanlines(radiusPx) }
        val scaled = scale != 1f
        if (scaled) {
            g.pose().pushMatrix()
            g.pose().scale(1f / scale, 1f / scale)
        }

        fun guiToPixel(value: Int) = (value * scale).roundToInt()

        fun drawCorner(originX: Int, originY: Int, flipX: Boolean, flipY: Boolean) {
            for (line in scanlines) {
                val rowY = if (flipY) originY + radiusPx - 1 - line.y else originY + line.y
                if (line.firstFull < radiusPx) {
                    val x0 = if (flipX) originX else originX + line.firstFull
                    val x1 = if (flipX) originX + radiusPx - line.firstFull else originX + radiusPx
                    fill(g, pipeline, textureSetup, x0, rowY, x1, rowY + 1, tint, 1f)
                }
                if (line.aaIndex >= 0 && line.coverage > 0f) {
                    val rowX = if (flipX) {
                        originX + radiusPx - 1 - line.aaIndex
                    } else {
                        originX + line.aaIndex
                    }
                    fill(
                        g,
                        pipeline,
                        textureSetup,
                        rowX,
                        rowY,
                        rowX + 1,
                        rowY + 1,
                        tint,
                        line.coverage,
                    )
                }
            }
        }

        val left = guiToPixel(x)
        val right = guiToPixel(x + width - radius)
        val top = guiToPixel(y)
        val bottom = guiToPixel(y + height - radius)
        if (doTL) drawCorner(left, top, flipX = false, flipY = false)
        if (doTR) drawCorner(right, top, flipX = true, flipY = false)
        if (doBL) drawCorner(left, bottom, flipX = false, flipY = true)
        if (doBR) drawCorner(right, bottom, flipX = true, flipY = true)

        if (scaled) g.pose().popMatrix()
    }

    private fun createCornerScanlines(radiusPx: Int): List<CornerScanline> {
        val radius = radiusPx.toFloat()
        return List(radiusPx) { y ->
            val yDistance = radius - (y + 0.5f)
            val boundary = radius - sqrt(
                (radius * radius - yDistance * yDistance).coerceAtLeast(0f).toDouble(),
            ).toFloat()
            val firstFull = ceil(boundary + 0.5f).toInt().coerceIn(0, radiusPx)
            val aaIndex = firstFull - 1
            val coverage = if (aaIndex >= 0) {
                (aaIndex + 1f - boundary).coerceIn(0f, 1f)
            } else {
                0f
            }
            CornerScanline(y, firstFull, aaIndex, coverage)
        }
    }

    private fun fill(
        g: GuiGraphicsExtractor,
        pipeline: RenderPipeline,
        textureSetup: TextureSetup,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        tint: Int,
        coverage: Float,
    ) {
        if (x1 <= x0 || y1 <= y0 || coverage <= 0f) return
        val alpha = (coverage.coerceIn(0f, 1f) * 255f).roundToInt()
        val color = (alpha shl 24) or (tint and 0x00FFFFFF)
        (g as GuiGraphicsExtractorInvoker).`grizzly$innerFill`(
            pipeline,
            textureSetup,
            x0,
            y0,
            x1,
            y1,
            color,
            null,
        )
    }
}
