package we.devs.opium.asm.mixins;

import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public class MusicTickerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preventDefaultMusic(CallbackInfo ci) {
        // Blockiere die Methode, um das Abspielen der Standardmusik zu verhindern
        ci.cancel();
    }
}