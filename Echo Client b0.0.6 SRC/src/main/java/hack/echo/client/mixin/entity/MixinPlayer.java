package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.movement.MoveFix;
import hack.echo.client.features.impl.movement.KeepSprintModule;
import hack.echo.client.handlers.RotationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import static hack.echo.client.utils.Imports.mc;

@Mixin(Player.class)
public class MixinPlayer {

    @Redirect(
            method = "travel",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
        )
        private Vec3 redirectSwimmingLookVector(Player entity) {
            if (Echo.isDestroyed) return entity.getLookAngle();
            if (entity == mc.player && RotationHandler.isHasSilentRotation() && Echo.featureManager != null) {
                MoveFix moveFix = Echo.featureManager.getFeatureByClass(MoveFix.class);
                if (moveFix != null && moveFix.shouldApplyRotationFix()) {
                    return RotationHandler.getRotationVector(RotationHandler.getServerPitch(), RotationHandler.getServerYaw());
                }
            }
            return entity.getLookAngle();
        }

    @Redirect(
            method = {"causeExtraKnockback", "doSweepAttack"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
            )
    )
    private float redirectAttackYaw(Player entity) {
        if (Echo.isDestroyed) return entity.getYRot();
        if (entity == mc.player && RotationHandler.isHasSilentRotation()) {
            return RotationHandler.getServerYaw();
        }
        return entity.getYRot();
    }

    @ModifyVariable(
            method = "causeExtraKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private Vec3 modifyExtraKnockbackVelocity(Vec3 velocity) {
        if (Echo.isDestroyed) return velocity;
        if (Echo.featureManager != null) {
            KeepSprintModule keepSprint = Echo.featureManager.getFeatureByClass(KeepSprintModule.class);
            if (keepSprint != null && keepSprint.isEnabled()) {
                float speedKeptVelocity = keepSprint.speedKeptVelocity.getValue();
                return velocity.multiply(speedKeptVelocity, 1.0, speedKeptVelocity);
            }
        }
        return velocity;
    }

    @Redirect(
        method = "causeExtraKnockback",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"
        )
    )
    private void redirectSetSprinting(Player player, boolean sprinting) {
        if (Echo.isDestroyed) {
            player.setSprinting(sprinting);
            return;
        }

        if (Echo.featureManager != null) {
            KeepSprintModule keepSprint = Echo.featureManager.getFeatureByClass(KeepSprintModule.class);
            if (keepSprint != null && keepSprint.isEnabled() && keepSprint.dontModifySprint()) {
                return;
            }
        }

        player.setSprinting(sprinting);
    }
}
