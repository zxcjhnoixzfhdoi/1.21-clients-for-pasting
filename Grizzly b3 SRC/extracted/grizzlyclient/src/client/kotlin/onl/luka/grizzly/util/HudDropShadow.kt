package onl.luka.grizzly.util

import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.config.entry.EnumEntry
import onl.luka.grizzly.config.entry.IntEntry
import onl.luka.grizzly.mixin.client.RenderPipelinesAccessor
import kotlin.math.roundToInt

enum class HudShadowShape { STEPPED, PER_ROW, TRIANGULAR }

data class HudDropShadowSettings(
    val enabled: BooleanEntry,
    val color: ColorEntry,
    val softness: IntEntry,
    val spread: IntEntry,
    val offsetX: IntEntry,
    val offsetY: IntEntry,
    val shape: EnumEntry<HudShadowShape>? = null,
) {
    fun draw(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
    ) {
        if (!enabled.value) return
        HudDropShadow.draw(
            g = g,
            x = x,
            y = y,
            width = width,
            height = height,
            radius = radius,
            color = color.liveColor(color.value).argb,
            softness = softness.value,
            spread = spread.value,
            offsetX = offsetX.value,
            offsetY = offsetY.value,
        )
    }

    fun drawStepped(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        rowWidths: List<Int>,
        rowHeight: Int,
        alignRight: Boolean,
        radius: Int,
    ) {
        if (!enabled.value || rowWidths.isEmpty()) return

        if ((shape?.value ?: HudShadowShape.STEPPED) == HudShadowShape.PER_ROW) {
            val totalWidth = rowWidths.max()
            rowWidths.forEachIndexed { index, width ->
                val rowX = if (alignRight) x + totalWidth - width else x
                draw(g, rowX, y + index * rowHeight, width, rowHeight, radius)
            }
            return
        }

        val widths = if (shape?.value == HudShadowShape.TRIANGULAR) rampWidths(rowWidths) else rowWidths
        HudDropShadow.drawStepped(
            g = g,
            x = x,
            y = y,
            rowWidths = widths,
            rowHeight = rowHeight,
            alignRight = alignRight,
            radius = radius,
            color = color.liveColor(color.value).argb,
            softness = softness.value,
            spread = spread.value,
            offsetX = offsetX.value,
            offsetY = offsetY.value,
        )
    }

    // Straight-line fit through the row widths. One outlier row no longer pushes a step out on
    // its own, and the silhouette stays a single wedge instead of a staircase.
    private fun rampWidths(rowWidths: List<Int>): List<Int> {
        val count = rowWidths.size
        if (count < 3) return rowWidths

        val meanIndex = (count - 1) / 2.0
        val meanWidth = rowWidths.average()
        var covariance = 0.0
        var variance = 0.0
        for (index in 0 until count) {
            val offset = index - meanIndex
            covariance += offset * (rowWidths[index] - meanWidth)
            variance += offset * offset
        }

        val slope = if (variance == 0.0) 0.0 else covariance / variance
        val lowest = rowWidths.min()
        val highest = rowWidths.max()
        return rowWidths.indices.map {
            (meanWidth + slope * (it - meanIndex)).roundToInt().coerceIn(lowest, highest)
        }
    }
}

object HudDropShadow {
    private data class PipelineKey(
        val width: Int,
        val height: Int,
        val radius: Int,
        val softness: Int,
        val spread: Int,
    )

    private data class SteppedPipelineKey(
        val width: Int,
        val height: Int,
        val rowHeight: Int,
        val rowCount: Int,
        val alignRight: Boolean,
        val radius: Int,
        val softness: Int,
        val spread: Int,
    )

    private val pipelines = mutableMapOf<PipelineKey, RenderPipeline>()
    private val steppedPipelines = mutableMapOf<SteppedPipelineKey, RenderPipeline>()
    private val whiteTexture = Identifier.withDefaultNamespace("textures/block/white_concrete.png")
    private val steppedProfileId = Identifier.fromNamespaceAndPath("medved", "ui/hud_shadow_profile")
    private var steppedProfileTexture: DynamicTexture? = null
    private var steppedProfile: List<Int> = emptyList()

    private const val MAX_STEPPED_ROWS = 128

    fun draw(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        color: Int,
        softness: Int,
        spread: Int,
        offsetX: Int,
        offsetY: Int,
    ) {
        if (width <= 0 || height <= 0 || color ushr 24 == 0) return

        val blur = softness.coerceIn(1, 16)
        val expansion = spread.coerceIn(0, 8)
        val cornerRadius = radius.coerceIn(0, minOf(width, height) / 2)
        val extent = expansion + blur * 3
        val drawWidth = width + extent * 2
        val drawHeight = height + extent * 2
        val pipeline = pipeline(width, height, cornerRadius, blur, expansion)

        g.blit(
            pipeline,
            whiteTexture,
            x + offsetX - extent,
            y + offsetY - extent,
            0f,
            0f,
            drawWidth,
            drawHeight,
            drawWidth,
            drawHeight,
            color,
        )
    }

    fun drawStepped(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        rowWidths: List<Int>,
        rowHeight: Int,
        alignRight: Boolean,
        radius: Int,
        color: Int,
        softness: Int,
        spread: Int,
        offsetX: Int,
        offsetY: Int,
    ) {
        val widths = rowWidths.take(MAX_STEPPED_ROWS)
        val width = widths.maxOrNull() ?: return
        if (width <= 0 || rowHeight <= 0 || color ushr 24 == 0) return

        updateSteppedProfile(widths)
        val height = widths.size * rowHeight
        val blur = softness.coerceIn(1, 16)
        val expansion = spread.coerceIn(0, 8)
        val cornerRadius = radius.coerceIn(0, minOf(width, rowHeight) / 2)
        val extent = expansion + blur * 3
        val drawWidth = width + extent * 2
        val drawHeight = height + extent * 2
        val pipeline = steppedPipeline(
            width,
            height,
            rowHeight,
            widths.size,
            alignRight,
            cornerRadius,
            blur,
            expansion,
        )

        g.blit(
            pipeline,
            steppedProfileId,
            x + offsetX - extent,
            y + offsetY - extent,
            0f,
            0f,
            drawWidth,
            drawHeight,
            drawWidth,
            drawHeight,
            color,
        )
    }

    private fun pipeline(
        width: Int,
        height: Int,
        radius: Int,
        softness: Int,
        spread: Int,
    ): RenderPipeline {
        val key = PipelineKey(width, height, radius, softness, spread)
        return pipelines.getOrPut(key) {
            RenderPipelinesAccessor.`grizzly$register`(
                RenderPipeline.builder(RenderPipelinesAccessor.`grizzly$guiSnippet`())
                    .withLocation(
                        Identifier.fromNamespaceAndPath(
                            "medved",
                            "hud_drop_shadow_${width}_${height}_${radius}_${softness}_${spread}",
                        ),
                    )
                    .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("medved", "core/hud_drop_shadow"))
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withShaderDefine("PANEL_WIDTH", width.toFloat())
                    .withShaderDefine("PANEL_HEIGHT", height.toFloat())
                    .withShaderDefine("PANEL_RADIUS", radius.toFloat())
                    .withShaderDefine("SHADOW_SOFTNESS", softness.toFloat())
                    .withShaderDefine("SHADOW_SPREAD", spread.toFloat())
                    .build(),
            )
        }
    }

    private fun steppedPipeline(
        width: Int,
        height: Int,
        rowHeight: Int,
        rowCount: Int,
        alignRight: Boolean,
        radius: Int,
        softness: Int,
        spread: Int,
    ): RenderPipeline {
        val key = SteppedPipelineKey(
            width,
            height,
            rowHeight,
            rowCount,
            alignRight,
            radius,
            softness,
            spread,
        )
        return steppedPipelines.getOrPut(key) {
            RenderPipelinesAccessor.`grizzly$register`(
                RenderPipeline.builder(RenderPipelinesAccessor.`grizzly$guiSnippet`())
                    .withLocation(
                        Identifier.fromNamespaceAndPath(
                            "medved",
                            "hud_stepped_shadow_${width}_${height}_${rowHeight}_${rowCount}_${if (alignRight) 1 else 0}_${radius}_${softness}_${spread}",
                        ),
                    )
                    .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("medved", "core/hud_stepped_drop_shadow"))
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withShaderDefine("PANEL_WIDTH", width.toFloat())
                    .withShaderDefine("PANEL_HEIGHT", height.toFloat())
                    .withShaderDefine("ROW_HEIGHT", rowHeight.toFloat())
                    .withShaderDefine("ROW_COUNT", rowCount)
                    .withShaderDefine("ALIGN_RIGHT", if (alignRight) 1 else 0)
                    .withShaderDefine("PANEL_RADIUS", radius.toFloat())
                    .withShaderDefine("SHADOW_SOFTNESS", softness.toFloat())
                    .withShaderDefine("SHADOW_SPREAD", spread.toFloat())
                    .build(),
            )
        }
    }

    private fun updateSteppedProfile(widths: List<Int>) {
        if (widths == steppedProfile) return

        val existing = steppedProfileTexture
        val image = existing?.getPixels() ?: NativeImage(MAX_STEPPED_ROWS, 1, false)
        for (index in 0 until MAX_STEPPED_ROWS) {
            val width = widths.getOrElse(index) { 0 }.coerceIn(0, 0xFFFF)
            val encodedAbgr = 0xFF000000.toInt() or
                ((width ushr 8 and 0xFF) shl 8) or
                (width and 0xFF)
            image.setPixelABGR(index, 0, encodedAbgr)
        }

        if (existing == null) {
            steppedProfileTexture = DynamicTexture({ "Grizzly HUD shadow profile" }, image).also {
                Minecraft.getInstance().textureManager.register(steppedProfileId, it)
            }
        } else {
            existing.upload()
        }
        steppedProfile = widths.toList()
    }
}
