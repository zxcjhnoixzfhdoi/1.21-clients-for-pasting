package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.utility.CheatDetector;
import onl.luka.grizzly.module.modules.utility.anticheat.CheatMarker;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void medved$markKnownCheaters(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (!CheatDetector.INSTANCE.isEnabled() || !CheatDetector.INSTANCE.getMarkTabList().getValue()) return;
        Component name = cir.getReturnValue();
        if (name == null) return;
        Component decorated = CheatMarker.INSTANCE.decorateName(info.getProfile().id(), name);
        if (decorated != name) cir.setReturnValue(decorated);
    }
}
