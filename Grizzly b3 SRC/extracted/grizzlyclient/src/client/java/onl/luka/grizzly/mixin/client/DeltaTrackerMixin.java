package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.combat.Criticals;
import onl.luka.grizzly.module.modules.movement.Speed;
import onl.luka.grizzly.module.modules.movement.Flight;
import onl.luka.grizzly.module.modules.movement.Timer;
import onl.luka.grizzly.module.modules.player.FastEat;
import onl.luka.grizzly.module.modules.player.TimerRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts DeltaTracker$Timer.advanceGameTime(long millis) to multiply the
 * elapsed milliseconds by the Timer module's speed factor.  When Timer is
 * disabled the multiplier is 1.0 so nothing changes.
 */
@Mixin(targets = "net.minecraft.client.DeltaTracker$Timer")
public class DeltaTrackerMixin {

    @Shadow
    private long lastMs;

    @Unique
    private long medved$lastRealMs = Long.MIN_VALUE;

    @ModifyVariable(method = "advanceGameTime", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private long medved$scaleTime(long millis) {
        if (this.medved$lastRealMs == Long.MIN_VALUE) {
            this.medved$lastRealMs = millis;
            return millis;
        }

        long elapsed = millis - this.medved$lastRealMs;
        this.medved$lastRealMs = millis;
        if (elapsed < 0L) {
            return this.lastMs;
        }

        long adjusted;
        if (TimerRange.INSTANCE.isControllingTime()) {
            adjusted = TimerRange.INSTANCE.transformElapsedTime(elapsed);
        } else {
            float factor = Criticals.timerSpeedMultiplier();
            factor *= Speed.timerSpeedMultiplier();
            factor *= FastEat.timerSpeedMultiplier();
            factor *= Flight.timerSpeedMultiplier();
            if (Timer.INSTANCE.isEnabled()) {
                factor *= Timer.INSTANCE.speed.getValue();
            }
            adjusted = factor == 1.0f ? elapsed : (long) (elapsed * factor);
        }

        return this.lastMs + Math.max(0L, adjusted);
    }
}
