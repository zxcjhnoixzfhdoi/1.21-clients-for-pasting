package hack.echo.client.mixin.input;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventKey;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("HEAD"))
    public void onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (input.key() == 1) return;
        new EventKey(input.key(), action).call();
    }
}
