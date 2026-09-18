package hack.echo.client.mixin.render.gui;

//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.event.impl.EventRenderEffects;
import hack.echo.client.event.impl.EventRenderNauseaOverlay;
import hack.echo.client.event.impl.EventRenderPortalOverlay;
import hack.echo.client.event.impl.EventRenderScoreboardSidebar;
import hack.echo.client.event.impl.EventRenderSpyglassOverlay;
import hack.echo.client.event.impl.EventRenderVignette;
import hack.echo.client.features.impl.render.hud.ArrayListModule;
import hack.echo.client.mixininterface.IGameRenderer;
import hack.echo.client.api.GuiGraphicsCompat;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.VulkanUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;

@Mixin(Gui.class)
public class MixinGui_1_21_11 {

    @Inject(method = "render", at = @At("TAIL"))
    private void echo$renderGui2D(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (mc.level == null) {
            return;
        }

        if (!VulkanUtil.isVulkanLoaded()) {
            ((IGameRenderer) mc.gameRenderer).echo$flushGuiState();
        }

        var draw = Echo.draw2D;
        draw.beginFrame(RenderUtil.getProjectionMatrix());
        CrossTexture blurTexture = draw.getBlurResult();

        EventRender2DGui event = new EventRender2DGui(
                new PoseStack(),
                deltaTracker.getGameTimeDeltaPartialTick(true),
                GuiGraphicsCompat.of(guiGraphics),
                draw,
                blurTexture
        );
        event.call();
        draw.endFrame();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    public void echo$renderVignette(GuiGraphics context, Entity entity, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderVignette event = new EventRenderVignette();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderNauseaOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderNauseaOverlay event = new EventRenderNauseaOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderSpyglassOverlay(GuiGraphics context, float scale, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderSpyglassOverlay event = new EventRenderSpyglassOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void echo$renderPortalOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderPortalOverlay event = new EventRenderPortalOverlay();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "renderEffects", at = @At("STORE"), ordinal = 3)
    private int modifyStatusEffectY(int y) {
        if (Echo.isDestroyed) return y;

        if (ArrayListModule.arrayListBottom > 0) {
            return y + (int) ArrayListModule.arrayListBottom;
        }

        return y;
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void echo$renderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderEffects event = new EventRenderEffects();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void echo$renderScoreboardSidebar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
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
//?} else {
/*public final class MixinGui_1_21_11 {
}
*///?}
