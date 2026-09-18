package hack.echo.client.mixin.render;

import hack.echo.client.utils.player.ITimer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class MixinRenderTickCounterDynamic implements ITimer {
    @Shadow private float deltaTicks;
    @Unique
    private float timerSpeed = 1.0F;

    @Inject(
            method = "advanceGameTime(J)I",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J",
                    ordinal = 1
            )
    )
    private void modifyDeltaTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        deltaTicks = deltaTicks * getTimer();
    }

    @Override
    public float getTimer() {
        return this.timerSpeed;
    }

    @Override
    public void setTimer(float timer) {
        this.timerSpeed = timer;
    }
}