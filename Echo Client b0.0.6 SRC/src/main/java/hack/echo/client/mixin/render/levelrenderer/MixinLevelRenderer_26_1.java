package hack.echo.client.mixin.render.levelrenderer;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer_26_1 {

    @Shadow
    protected abstract EntityRenderState extractEntity(Entity entity, float f);

    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void echo$onExtractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (mc.player == null) return;
        if (mc.level == null) return;
        if (Echo.isDestroyed) return;

        Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
        if (!freecam.isEnabled()) return;

        Entity player = mc.player;
        TickRateManager tickRateManager = mc.level.tickRateManager();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(player));
        EntityRenderState entityRenderState = this.extractEntity(player, partialTick);
        levelRenderState.entityRenderStates.add(entityRenderState);
    }
}
*///?} else {
public final class MixinLevelRenderer_26_1 {
}
//?}
