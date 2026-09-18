package we.devs.opium.asm.mixins;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Random;

@Mixin(TitleScreen.class)
public class TitleScreenSplashMixin {

    @Shadow
    private SplashTextRenderer splashText;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo info) {
        Random random = new Random();
        String[] customSplashes = {
                "Cxiy",
                "Heedi",
                "Opium",
                "",
                "VoidMatter",
                "Qweru",
                "Crystal",
                "Otterware"

        };
        splashText = new SplashTextRenderer(customSplashes[random.nextInt(customSplashes.length)]);
    }
}
