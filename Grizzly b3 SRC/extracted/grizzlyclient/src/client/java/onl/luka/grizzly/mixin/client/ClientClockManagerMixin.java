package onl.luka.grizzly.mixin.client;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import onl.luka.grizzly.module.modules.render.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void medved$overrideClockTime(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
        long override = Ambience.clockTimeOverride();
        if (override >= 0L) cir.setReturnValue(override);
    }
}
