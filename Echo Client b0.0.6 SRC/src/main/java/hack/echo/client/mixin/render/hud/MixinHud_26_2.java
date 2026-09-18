package hack.echo.client.mixin.render.hud;

//? if >26.1.2 {
/*import hack.echo.client.api.GuiGraphicsCompat;
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRenderEffects;
import hack.echo.client.event.impl.EventRenderNauseaOverlay;
import hack.echo.client.event.impl.EventRenderPortalOverlay;
import hack.echo.client.event.impl.EventRenderScoreboardSidebar;
import hack.echo.client.event.impl.EventRenderSpyglassOverlay;
import hack.echo.client.event.impl.EventRenderVignette;
import hack.echo.client.features.impl.render.hud.ArrayListModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class MixinHud_26_2 {

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    public void echo$renderVignette(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderVignette event = new EventRenderVignette();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderNauseaOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderNauseaOverlay event = new EventRenderNauseaOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderSpyglassOverlay(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderSpyglassOverlay event = new EventRenderSpyglassOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderPortalOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderPortalOverlay event = new EventRenderPortalOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "extractEffects", at = @At("STORE"), ordinal = 3)
    private int modifyStatusEffectY(int y) {
        if (Echo.isDestroyed) return y;

        if (ArrayListModule.arrayListBottom > 0) {
            return y + (int) ArrayListModule.arrayListBottom;
        }

        return y;
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void echo$renderEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderEffects event = new EventRenderEffects();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void echo$renderScoreboardSidebar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderScoreboardSidebar.Pre event = new EventRenderScoreboardSidebar.Pre(
                GuiGraphicsCompat.of(guiGraphics),
                deltaTracker
        );
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
*///?} else {
public final class MixinHud_26_2 {
}
//?}
