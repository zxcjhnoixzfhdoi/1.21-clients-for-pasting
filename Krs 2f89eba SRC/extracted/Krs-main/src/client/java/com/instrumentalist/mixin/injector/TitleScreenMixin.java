package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.screen.CustomTitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void customTitleScreenHook(CallbackInfo ci) {
        ci.cancel();
        this.minecraft.gui.setScreen(new CustomTitleScreen(Component.nullToEmpty("Title Screen")));
    }
}
