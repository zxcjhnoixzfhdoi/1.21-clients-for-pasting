package we.devs.opium.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import we.devs.opium.Opium;
import we.devs.opium.client.events.EventRender2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow @Final private ChatHud chatHud;

    @Shadow @Final private MinecraftClient client;

    @Shadow public abstract void tick(boolean paused);

    @Shadow private int ticks;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventRender2D event = new EventRender2D(tickCounter.getTickDelta(true), context);
        Opium.EVENT_MANAGER.call(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(CallbackInfo ci) {
        if (!Opium.MODULE_MANAGER.isModuleEnabled("Crosshair"))
            return;

        ci.cancel();
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    void renderChat(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

    }
}
