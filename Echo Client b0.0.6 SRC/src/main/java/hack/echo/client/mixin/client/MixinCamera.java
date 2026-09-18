package hack.echo.client.mixin.client;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;


@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    protected abstract void setPosition(Vec3 vec3);
    @Shadow
    protected abstract void setRotation(float f, float g);



    @Inject(method = "setup", at = @At("RETURN"), cancellable = true)
    private void echo$onSetup(Level level, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (level == null || entity == null) return;
        if (mc.player == null) return;
        Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            Vec3 pos = freecam.getPosition();
            if (pos == null) return;
            this.setRotation(freecam.getYaw(), freecam.getPitch());
            this.setPosition(pos);
        }
    }
}
