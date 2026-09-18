package we.devs.opium.client.modules.visuals;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueNumber;
import we.devs.opium.client.values.impl.ValueEnum;

import java.awt.*;

import static me.x150.renderer.util.RendererUtils.worldSpaceToScreenSpace;

@RegisterModule(name = "BurrowESP", description = "Display if a player is burrowed", tag = "BurrowESP", category = Module.Category.VISUALS)
public class ModuleBurrowESP extends Module {

    private final ValueNumber scale = new ValueNumber("Scale", "Scale", "Scale", 0.68f, 0.1f, 2f);
    private final ValueNumber minScale = new ValueNumber("MinScale", "MinScale", "MinScale", 0.2f, 0.1f, 1f);
    private final ValueNumber yOff = new ValueNumber("YOffset", "YOffset", "Text y offset", 0.1f, -0.5, 0.5);
    private final ValueColor color = new ValueColor("TextColor", "TextColor", "TextColor", Color.WHITE);
    private final ValueEnum renderPosition = new ValueEnum("RenderPosition", "RenderPosition", "Determines if the Text is being rendered on the Block or the Player.", renderModes.BlockPos);

    @Override
    public void onRender2D(EventRender2D context) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        assert mc.world != null;

        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (!isBurrowBlock(ent.getBlockPos())) continue;

            // Interpolated Position Calculation
            double x = ent.prevX + (ent.getX() - ent.prevX) * tickDelta;
            double y = ent.prevY + (ent.getY() - ent.prevY) * tickDelta + yOff.getValue().doubleValue();
            double z = ent.prevZ + (ent.getZ() - ent.prevZ) * tickDelta;
            Vec3d worldPos = new Vec3d(x, y, z);

            Vec3d screenPos = worldSpaceToScreenSpace(worldPos);
            if (screenPos.z <= 0 || screenPos.z >= 1) continue; // Only render visible players

            Vector4d position = new Vector4d(screenPos.x, screenPos.y, screenPos.z, 0);
            String text = "Burrowed";
            float textWidth = mc.textRenderer.getWidth(text);

            // Center text horizontally
            float centerX = (float) (position.x - textWidth / 2);

            // Distance-Based Scaling
            assert mc.cameraEntity != null;
            float distance = (float) mc.cameraEntity.squaredDistanceTo(worldPos);
            float scaleFactor = MathHelper.clamp(scale.getValue().floatValue() / (distance * 0.1f), minScale.getValue().floatValue(), scale.getValue().floatValue());

            context.getContext().getMatrices().push();
            context.getContext().getMatrices().translate(position.x, position.y - 13f, 0);
            context.getContext().getMatrices().scale(scaleFactor, scaleFactor, 1f);

            // Draw the text
            context.getContext().drawText(mc.textRenderer, text, (int) -(textWidth / 2), 0, color.getValue().getRGB(), true);
            context.getContext().getMatrices().pop();
        }
    }

    private boolean isBurrowBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.BEDROCK;
    }

    public enum renderModes {
        BlockPos,
        PlayerPos
    }
}
