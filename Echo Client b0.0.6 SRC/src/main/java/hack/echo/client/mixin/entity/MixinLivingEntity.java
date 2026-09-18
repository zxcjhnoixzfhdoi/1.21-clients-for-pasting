package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventShieldBreak;
import hack.echo.client.features.impl.movement.MoveFix;
import hack.echo.client.features.impl.render.SwingSpeedModule;
import hack.echo.client.handlers.RotationHandler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static hack.echo.client.utils.Imports.mc;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    @Unique
    private ItemStack echo$previousActiveItem = ItemStack.EMPTY;

    public MixinLivingEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    // Movefix sprint jump fix (I do have another method too :skull:)
    @ModifyArg(
        method = "jumpFromGround",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private Vec3 adjustSprintJumpVelocity(Vec3 original) {
        if (Echo.isDestroyed) return original;

        if (mc.player == null || (Object) this != mc.player) {
            return original;
        }
        if (Echo.featureManager == null) return original;

        MoveFix moveFix = Echo.featureManager.getFeatureByClass(MoveFix.class);
        if (moveFix == null || !moveFix.shouldApplyRotationFix() || !RotationHandler.isHasSilentRotation()) {
            return original;
        }

        double magnitude = Math.sqrt(original.x * original.x + original.z * original.z);
        if (magnitude < 1.0E-6) {
            return original;
        }

        float serverYawRad = RotationHandler.getServerYaw() * (float) (Math.PI / 180.0);
        double sin = -Mth.sin(serverYawRad) * magnitude;
        double cos = Mth.cos(serverYawRad) * magnitude;
        return new Vec3(sin, original.y, cos);
    }

    // Elytra rotation fixes
    @Redirect(
        method = "updateFallFlyingMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"
        )
    )
    private float redirectElytraPitch(LivingEntity instance) {
        if (Echo.isDestroyed) return instance.getXRot();

        if (instance == mc.player && RotationHandler.isHasSilentRotation()) {
            if (Echo.featureManager == null) return instance.getXRot();
            MoveFix moveFix = Echo.featureManager.getFeatureByClass(MoveFix.class);
            if (moveFix != null && moveFix.shouldApplyRotationFix()) {
                return RotationHandler.getServerPitch();
            }
        }
        return instance.getXRot();
    }

    @Redirect(
        method = "updateFallFlyingMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectElytraRotationVector(LivingEntity instance) {
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

    @Inject(method = "onSyncedDataUpdated", at = @At("HEAD"))
    private void onTrackedDataSet(EntityDataAccessor<?> data, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!self.level().isClientSide()) return;

        if (!(self instanceof Player)) return;

        ItemStack currentActiveItem = self.getUseItem();

        this.echo$previousActiveItem = currentActiveItem.copy();
    }

    // Useless as it only works in singleplayer
    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void onTrackedDataSetTail(EntityDataAccessor<?> data, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        LivingEntity self = (LivingEntity) (Object) this;

        if (!self.level().isClientSide()) return;

        if (!(self instanceof Player player)) return;

        ItemStack currentActiveItem = self.getUseItem();

        if (!this.echo$previousActiveItem.isEmpty() &&
            this.echo$previousActiveItem.getItem() instanceof ShieldItem &&
            currentActiveItem.isEmpty() &&
            !self.isUsingItem()) {

            EventShieldBreak event = new EventShieldBreak(player);
            event.call();
        }
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void Echo$getCurrentSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (Echo.featureManager == null) return;
        if (Echo.isDestroyed) return;
        SwingSpeedModule swingSpeedModule = Echo.featureManager.getFeatureByClass(SwingSpeedModule.class);
        if (swingSpeedModule != null && swingSpeedModule.isEnabled()) {
            int swingSpeed = SwingSpeedModule.swingSpeed.getValue();
            cir.setReturnValue(swingSpeed);
        }
    }
}
