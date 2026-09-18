package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.combat.Misplace;
import onl.luka.grizzly.module.modules.render.ESP3D;
import onl.luka.grizzly.module.modules.render.Nametags;
import onl.luka.grizzly.module.modules.utility.CheatDetector;
import onl.luka.grizzly.module.modules.utility.anticheat.CheatMarker;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void medved$applyPlayerEspGlow(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        Misplace.offsetRenderState(entity, state);

        int outlineColor = ESP3D.glowOutlineColor(entity);
        if (outlineColor != 0) {
            state.outlineColor = outlineColor;
        }

        if (Nametags.shouldHideVanilla(entity)) {
            state.nameTag = null;
            state.scoreText = null;
        }

        if (state.nameTag != null
                && entity instanceof Player
                && CheatDetector.INSTANCE.isEnabled()
                && CheatDetector.INSTANCE.getMarkNametags().getValue()) {
            state.nameTag = CheatMarker.INSTANCE.decorateName(entity.getUUID(), state.nameTag);
        }
    }
}
