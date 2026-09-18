package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.render.WorldTime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor, AutoCloseable {

    @Inject(at = @At("HEAD"), method = "getRainLevel(F)F", cancellable = true)
    private void worldTimeHook1(float delta, CallbackInfoReturnable<Float> cir) {
        if (WorldTime.shouldClearWeather())
            cir.setReturnValue(0F);
    }

    @Inject(at = @At("HEAD"), method = "getThunderLevel(F)F", cancellable = true)
    private void worldTimeHook2(float delta, CallbackInfoReturnable<Float> cir) {
        if (WorldTime.shouldClearWeather())
            cir.setReturnValue(0F);
    }

    @Inject(at = @At("HEAD"), method = "isRaining", cancellable = true)
    private void worldTimeHook3(CallbackInfoReturnable<Boolean> cir) {
        if (WorldTime.shouldClearWeather())
            cir.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "isThundering", cancellable = true)
    private void worldTimeHook4(CallbackInfoReturnable<Boolean> cir) {
        if (WorldTime.shouldClearWeather())
            cir.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "isRainingAt", cancellable = true)
    private void worldTimeHook5(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (WorldTime.shouldClearWeather())
            cir.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = {"getDefaultClockTime", "getOverworldClockTime"}, cancellable = true)
    private void worldTimeHook6(CallbackInfoReturnable<Long> cir) {
        Long timeTicks = WorldTime.getActiveTimeTicks();
        if (timeTicks != null)
            cir.setReturnValue(timeTicks);
    }
}
