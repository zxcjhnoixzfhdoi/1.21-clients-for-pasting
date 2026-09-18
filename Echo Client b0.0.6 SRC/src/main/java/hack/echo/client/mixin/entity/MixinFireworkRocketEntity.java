package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.movement.MoveFix;
import hack.echo.client.handlers.RotationHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static hack.echo.client.utils.Imports.mc;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity extends Entity {

    public MixinFireworkRocketEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectFireworkRotation(LivingEntity instance) {
        if (Echo.isDestroyed) return instance.getLookAngle();

        if (instance == mc.player && RotationHandler.isHasSilentRotation()) {
            if (Echo.featureManager == null) return instance.getLookAngle();
            MoveFix moveFix = Echo.featureManager.getFeatureByClass(MoveFix.class);
            if (moveFix != null && moveFix.shouldApplyRotationFix()) {
                return calculateViewVector(RotationHandler.getServerPitch(), RotationHandler.getServerYaw());
            }
        }
        return instance.getLookAngle();
    }
}

