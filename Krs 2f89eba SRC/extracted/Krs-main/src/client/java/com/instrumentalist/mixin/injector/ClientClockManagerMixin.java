package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.render.WorldTime;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {

    @Inject(at = @At("HEAD"), method = "getTotalTicks", cancellable = true)
    private void worldTimeHook(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        Long timeTicks = WorldTime.getActiveTimeTicks();
        if (timeTicks != null)
            cir.setReturnValue(timeTicks);
    }
}
