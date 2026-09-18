package hack.echo.client.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import hack.echo.client.Echo;
import hack.echo.client.auth.MathProt;
import hack.echo.client.event.impl.*;
import hack.echo.client.handlers.RotationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
    @Shadow
    @Final
    public ClientPacketListener connection;
    @Shadow
    private double xLast;
    @Shadow
    private double yLast;
    @Shadow
    private double zLast;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean lastHorizontalCollision;
    @Shadow
    private boolean crouching;
    @Shadow
    private int positionReminder;
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Shadow
    private boolean autoJumpEnabled = true;

    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    protected abstract boolean isControlledCamera();
    @Shadow
    protected abstract void sendIsSprintingIfNeeded();

    @Unique
    private EventMove echo$currentMoveEvent;

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void echo$onSendMovementPacketsHead(CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        float enforcedPitch = MathProt.getEnforcedPitch(this.getXRot());
        // Update server rotations every tick to keep them in sync (prevents AimModulo360 flags I think)
        if (!RotationHandler.isHasSilentRotation()) {
            RotationHandler.updateServerRotations(this.getYRot(), enforcedPitch);
        }
        
        EventMove event = new EventMove.Pre(this.getX(), this.getY(), this.getZ(), this.onGround(), this.getYRot(), enforcedPitch);
        event.call();
        if (event.cancelled) {
            this.echo$currentMoveEvent = null;
            ci.cancel();
            return;
        }
        this.echo$currentMoveEvent = event;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void echo$onSendMovementPacketsReturn(CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        EventMove postEvent = new EventMove.Post(this.getX(), this.getY(), this.getZ(), this.onGround(), this.getYRot(), this.getXRot());
        postEvent.call();
        this.echo$currentMoveEvent = null;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double echo$useEventX(double original) {
        if (Echo.isDestroyed) return original;
        return this.echo$currentMoveEvent != null ? this.echo$currentMoveEvent.getX() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double echo$useEventY(double original) {
        if (Echo.isDestroyed) return original;
        return this.echo$currentMoveEvent != null ? this.echo$currentMoveEvent.getY() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double echo$useEventZ(double original) {
        if (Echo.isDestroyed) return original;
        return this.echo$currentMoveEvent != null ? this.echo$currentMoveEvent.getZ() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float echo$useEventYaw(float original) {
        if (Echo.isDestroyed) return original;
        return this.echo$currentMoveEvent != null ? (float) this.echo$currentMoveEvent.getYaw() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float echo$useEventPitch(float original) {
        if (Echo.isDestroyed) return original;
        float pitchValue = this.echo$currentMoveEvent != null ? (float) this.echo$currentMoveEvent.getPitch() : original;
        return MathProt.getEnforcedPitch(pitchValue);
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean echo$useEventOnGround(boolean original) {
        if (Echo.isDestroyed) return original;
        return this.echo$currentMoveEvent != null ? this.echo$currentMoveEvent.isOnGround() : original;
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 echo$useEventEntityPos(Vec3 original) {
        if (Echo.isDestroyed) return original;
        if (this.echo$currentMoveEvent != null) {
            return new Vec3(this.echo$currentMoveEvent.getX(), this.echo$currentMoveEvent.getY(), this.echo$currentMoveEvent.getZ());
        }
        return original;
    }



    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(InteractionHand hand, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        EventSwingHand swingHand = new EventSwingHand(player, hand);
        swingHand.call();
        if (swingHand.cancelled) {
            ci.cancel();
        }
    }

    // Totem pop chams
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        EventClientPlayerTick e = new EventClientPlayerTick();
        e.call();
        if (e.cancelled) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"
        )
    )
    private boolean echo$noSlow(boolean original) {
        if (Echo.isDestroyed) return original;
        EventSlowdown event = new EventSlowdown(1.0f, original, false, false);
        event.call();
        if (event.cancelled) {
            return false;
        }
        return event.isSlowDueToItem();
    }

    @ModifyExpressionValue(
        method = "modifyInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"
        )
    )
    private float echo$noSlowItemUseSpeedMultiplier(float original) {
        if (Echo.isDestroyed) return original;
        EventSlowdown event = new EventSlowdown(original, false, false, false);
        event.call();
        if (event.cancelled) {
            return 1.0f;
        }
        return event.getSlowdownMultiplier();
    }

    @ModifyExpressionValue(
        method = "modifyInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isMovingSlowly()Z"
        )
    )
    private boolean echo$noSlowIsMovingSlowly(boolean original) {
        if (Echo.isDestroyed) return original;
        EventSlowdown event = new EventSlowdown(1.0f, false, original, false);
        event.call();
        if (event.cancelled) {
            return false;
        }
        return event.isMovingSlowly();
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z",
            ordinal = 1
        )
    )
    private boolean echo$noSlowIsPassengerAiStep(LocalPlayer instance) {
        if (Echo.isDestroyed) return instance.isPassenger();
        if (instance.isPassenger()) {
            return true;
        }
        EventSlowdown event = new EventSlowdown(1.0f, false, false, false);
        event.call();
        return event.isSpoofPassenger();
    }

    @Redirect(
        method = "modifyInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z"
        )
    )
    private boolean echo$noSlowIsPassengerModifyInput(LocalPlayer instance) {
        if (Echo.isDestroyed) return instance.isPassenger();
        if (instance.isPassenger()) {
            return true;
        }
        EventSlowdown event = new EventSlowdown(1.0f, false, false, false);
        event.call();
        return event.isSpoofPassenger();
    }

    @Inject(method = "move",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            cancellable = true)
    private void echo$onMoveHook(MoverType moverType, Vec3 vec3, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        
        EventMovePos event = new EventMovePos(moverType, vec3.x, vec3.y, vec3.z);
        event.call();
        
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
