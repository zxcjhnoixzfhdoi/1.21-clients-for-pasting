package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.level.Xray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class SodiumAbstractBlockRenderContextMixin {
    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true, require = 0)
    private void krs$xrayIsFaceCulled(Direction face, CallbackInfoReturnable<Boolean> cir) {
        Boolean shouldDrawSide = Xray.shouldDrawSide(state, pos);
        if (shouldDrawSide != null) {
            cir.setReturnValue(!shouldDrawSide);
        }
    }

    protected boolean krs$shouldApplyXrayOpacity() {
        return Xray.isOpacityMode() && state != null && !Xray.isVisible(state, pos);
    }
}
