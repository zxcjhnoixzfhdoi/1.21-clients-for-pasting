package hack.echo.client.mixin.render.vulkan;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.OutlineBufferSource$EntityOutlineGenerator", priority = 1500)
public abstract class MixinEntityOutlineGenerator implements ExtendedVertexBuilder {


    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private int color;

    @Override
    public boolean canUseFastVertex() {
        return delegate instanceof ExtendedVertexBuilder;
    }

    @Override
    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int normal) {
        if (delegate instanceof ExtendedVertexBuilder evb) {
            evb.vertex(x, y, z, color, u, v, overlay, light, normal);
        }
    }
}
