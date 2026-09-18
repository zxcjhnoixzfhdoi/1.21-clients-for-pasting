package onl.luka.grizzly.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import onl.luka.grizzly.module.modules.hud.ScoreboardHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudScoreboardMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void grizzly$replaceScoreboardSidebar(
            GuiGraphicsExtractor g,
            Objective objective,
            CallbackInfo ci
    ) {
        if (ScoreboardHud.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
