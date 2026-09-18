package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.Freecam;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
    
    @Inject(method = "extractRenderState", at = @At("TAIL"), cancellable = true)
    private void echo$forcePlayerNameTag(T entity, S entityRenderState, float f, CallbackInfo ci) {
        if (mc.player == null) return;
        if (mc.level == null) return;
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
        if (freecam == null) return;
        if (!freecam.isEnabled()) return;
        Entity player = mc.player;
        if (entity != player) return;
        entityRenderState.nameTag = player.getDisplayName();
        entityRenderState.nameTagAttachment = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getYRot(f));
        

    }
}
