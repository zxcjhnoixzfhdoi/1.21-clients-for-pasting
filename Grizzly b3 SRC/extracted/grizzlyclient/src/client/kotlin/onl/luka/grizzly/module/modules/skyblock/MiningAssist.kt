package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult

object MiningAssist : Module(
    "Mining Assist",
    "Keeps breaking progress active and shows whether the targeted block is in reach",
    Category.SKYBLOCK,
) {
    private val maintainProgress = boolean("maintain breaking progress", true)
    private val reach = double("reach indicator distance", 4.5, 2.0, 6.0)
    private val inReachColor = color("in reach color", Color(80, 255, 120, 180))
    private val outOfReachColor = color("out of reach color", Color(255, 70, 70, 210))

    private var targetBox: SkyBlockBox? = null
    private var progress = 0

    override fun onDisabled() {
        targetBox = null
        progress = 0
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val hit = client.hitResult as? BlockHitResult ?: run {
            targetBox = null
            progress = 0
            return
        }
        val level = client.level ?: return
        if (level.getBlockState(hit.blockPos).isAir) {
            targetBox = null
            progress = 0
            return
        }

        val distance = player.eyePosition.distanceTo(hit.location)
        targetBox = SkyBlockBox(
            AABB(hit.blockPos).deflate(0.004),
            if (distance <= reach.value) inReachColor.value else outOfReachColor.value,
        )
        progress = (client.gameMode?.destroyStage ?: 0).coerceIn(0, 10) * 10

        if (maintainProgress.value && client.options.keyAttack.isDown && distance <= reach.value) {
            client.gameMode?.continueDestroyBlock(hit.blockPos, hit.direction)
        }
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        targetBox?.let { renderSkyBlockBoxes(ctx, listOf(it), 2f) }
    }

    override fun hudInfo(): String = if (progress > 0) "$progress%" else ""
}
