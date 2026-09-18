package we.devs.opium.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Unique;
import we.devs.opium.Opium;
import we.devs.opium.client.events.EventRender3D;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static we.devs.opium.api.utilities.IMinecraft.mc;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Unique
    MatrixStack current = null;

    @ModifyExpressionValue(method = "render", at = @At(value = "NEW", target = "net/minecraft/client/util/math/MatrixStack"))
    private MatrixStack setMatrixStack(MatrixStack matrixStack) {
        current = matrixStack;
        return matrixStack;
    }

    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0), cancellable = true)
    private void afterEntities(CallbackInfo ci) {
        EventRender3D event = new EventRender3D(mc.getRenderTickCounter().getTickDelta(true), current);
        Opium.EVENT_MANAGER.call(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

}
