package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.math.TimerUtil;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public abstract class RenderTickCounterDynamicMixin {
    @Shadow
    private float deltaTicks;

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;deltaTicks:F", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER), method = "advanceGameTime(J)I")
    public void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> ci) {
        if (TimerUtil.timerSpeed > 0)
            deltaTicks *= TimerUtil.timerSpeed;
    }
}
