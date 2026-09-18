package we.devs.opium.asm.mixins;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.api.manager.music.CustomSoundEngineManager;
import we.devs.opium.api.manager.music.MusicStateManager;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onWorldLoad(CallbackInfo ci) {
        // Stop the custom music when the player loads into a world
        if (MusicStateManager.isPlayingCustomMusic()) {
            CustomSoundEngineManager.stopMusic();
            MusicStateManager.setPlayingCustomMusic(false);
        }
    }
}