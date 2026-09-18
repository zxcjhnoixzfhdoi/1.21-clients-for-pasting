package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.level.CaveFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public abstract class SodiumDefaultChunkRendererMixin {
    private static final int ALL_MODEL_FACES = 0x7F;

    @Inject(method = "getVisibleFaces(IIIIII)I", at = @At("HEAD"), cancellable = true, require = 0)
    private static void krs$caveFinderDrawAllFaces(int cameraX, int cameraY, int cameraZ,
                                                   int sectionX, int sectionY, int sectionZ,
                                                   CallbackInfoReturnable<Integer> cir) {
        if (CaveFinder.isActive())
            cir.setReturnValue(ALL_MODEL_FACES);
    }
}
