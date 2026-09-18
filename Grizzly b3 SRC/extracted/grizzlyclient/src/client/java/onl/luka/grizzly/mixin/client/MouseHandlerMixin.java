package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.movement.InvMove;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // A screen the player cannot see should not take their cursor away.
    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private void medved$keepMouseGrabbedForHiddenScreens(CallbackInfo ci) {
        if (InvMove.INSTANCE.shouldKeepMouseGrabbed()) {
            ci.cancel();
        }
    }
}
