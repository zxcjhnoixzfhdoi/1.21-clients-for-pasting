package onl.luka.grizzly.module.modules.render

import com.mojang.blaze3d.vertex.PoseStack
import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.RenderUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import org.joml.Quaternionf
import org.joml.Vector3f

object Skeletons : Module(
    name = "Skeletons",
    description = "Draws bone lines over player models, through walls",
    category = Category.RENDER,
) {
    enum class ColorMode { TEAM, HEALTH, STATIC }

    private val colorMode = enum("color mode", ColorMode.STATIC)
    private val renderSelf = boolean("render self", true)
    private val playersOnly = boolean("players only", true)
    private val ignoreTargetFilter = boolean("ignore target filter", true).also {
        it.visibleWhen = { playersOnly.value }
    }
    private val teamOnly = boolean("team only", false).also {
        it.visibleWhen = { colorMode.value == ColorMode.TEAM }
    }
    private val lineWidth = float("line width", 1.0f, 0.5f, 5.0f)
    private val skeletonColor = color("color", Color(255, 255, 255, 255), allowAlpha = true).also {
        it.pickerMode = ColorEntry.PickerMode.THEME
        it.visibleWhen = { colorMode.value != ColorMode.HEALTH }
    }

    // Vanilla part geometry: the arm cube hangs 10 units below its pivot, the leg 12, and the
    // head reaches 8 up from the neck.
    private const val MODEL_SCALE = 1f / 16f
    private const val ARM_LENGTH = 10f
    private const val LEG_LENGTH = 12f
    private const val SKULL_LENGTH = -6f

    @JvmStatic
    fun submit(
        model: Model<*>,
        renderState: Any,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
    ) {
        if (!isEnabled()) return
        val humanoid = model as? HumanoidModel<*> ?: return
        val state = renderState as? LivingEntityRenderState ?: return
        val target = targetFor(state) ?: return

        val color = resolveColor(target.player) ?: return
        val r = color.r / 255f
        val g = color.g / 255f
        val b = color.b / 255f
        val a = color.a / 255f
        val width = lineWidth.value

        val head = pivot(humanoid.head)
        val rightArm = pivot(humanoid.rightArm)
        val leftArm = pivot(humanoid.leftArm)
        val rightLeg = pivot(humanoid.rightLeg)
        val leftLeg = pivot(humanoid.leftLeg)
        val shoulders = midpoint(rightArm, leftArm)
        val hips = midpoint(rightLeg, leftLeg)

        val bones = listOf(
            head to tip(humanoid.head, SKULL_LENGTH),
            shoulders to head,
            shoulders to hips,
            rightArm to leftArm,
            rightLeg to leftLeg,
            rightArm to tip(humanoid.rightArm, ARM_LENGTH),
            leftArm to tip(humanoid.leftArm, ARM_LENGTH),
            rightLeg to tip(humanoid.rightLeg, LEG_LENGTH),
            leftLeg to tip(humanoid.leftLeg, LEG_LENGTH),
        )

        collector.submitCustomGeometry(poseStack, RenderUtil.ESP_LINES) { pose, vc ->
            for ((from, to) in bones) {
                RenderUtil.line(
                    vc, pose,
                    from.x * MODEL_SCALE, from.y * MODEL_SCALE, from.z * MODEL_SCALE,
                    to.x * MODEL_SCALE, to.y * MODEL_SCALE, to.z * MODEL_SCALE,
                    r, g, b, a, width,
                )
            }
        }
    }

    // Null means the entity is not drawn at all; a null player inside means it is a humanoid mob,
    // which has no team or health colour to read.
    private class Target(val player: Player?)

    private fun targetFor(state: LivingEntityRenderState): Target? {
        if (state.isInvisibleToPlayer) return null
        if (state !is AvatarRenderState) return if (playersOnly.value) null else Target(null)

        val mc = Minecraft.getInstance()
        val viewer = mc.player ?: return null
        if (state.id == viewer.id) return if (renderSelf.value) Target(viewer) else null

        val player = mc.level?.getEntity(state.id) as? Player ?: return null
        if (!ignoreTargetFilter.value && !TargetFilter.isValidTarget(viewer, player)) return null
        return Target(player)
    }

    private fun resolveColor(target: Player?): Color? {
        val live = skeletonColor.liveColor(skeletonColor.value)
        if (target == null) return live

        return when (colorMode.value) {
            ColorMode.STATIC -> live

            ColorMode.HEALTH -> {
                val ratio = (target.health / target.maxHealth).coerceIn(0f, 1f)
                val red = if (ratio < 0.5f) 1f else 2f * (1f - ratio)
                val green = if (ratio > 0.5f) 1f else 2f * ratio
                Color((red * 255).toInt(), (green * 255).toInt(), 0, live.a)
            }

            ColorMode.TEAM -> {
                val helmet = target.getItemBySlot(EquipmentSlot.HEAD)
                val rgb = helmet.get(DataComponents.DYED_COLOR)?.rgb()
                when {
                    rgb != null -> Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, live.a)
                    teamOnly.value -> null
                    else -> live
                }
            }
        }
    }

    private fun pivot(part: ModelPart) = Vector3f(part.x, part.y, part.z)

    // Model space runs Y-down, so a positive length points from the joint towards the feet.
    private fun tip(part: ModelPart, length: Float): Vector3f =
        Quaternionf()
            .rotationZYX(part.zRot, part.yRot, part.xRot)
            .transform(Vector3f(0f, length, 0f))
            .add(part.x, part.y, part.z)

    private fun midpoint(a: Vector3f, b: Vector3f) =
        Vector3f(a).add(b).mul(0.5f)
}
