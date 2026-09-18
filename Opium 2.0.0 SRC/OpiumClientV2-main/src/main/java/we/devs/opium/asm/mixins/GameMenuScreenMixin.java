package we.devs.opium.asm.mixins;

import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.api.manager.music.MusicStateManager;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "initWidgets()V", at = @At("TAIL"))
    private void onWorldExit(CallbackInfo ci) {
        // Setze das Flag zurück, wenn der Spieler das Menü besucht (indirekt beim Verlassen)
        MusicStateManager.setPlayingCustomMusic(false);
    }
}