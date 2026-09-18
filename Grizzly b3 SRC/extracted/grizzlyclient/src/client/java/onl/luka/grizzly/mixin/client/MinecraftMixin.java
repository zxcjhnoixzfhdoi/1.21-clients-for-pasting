package onl.luka.grizzly.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import onl.luka.grizzly.module.modules.combat.AutoBlock;
import onl.luka.grizzly.module.modules.combat.HitSelect;
import onl.luka.grizzly.module.modules.combat.NoHitDelay;
import onl.luka.grizzly.module.modules.combat.Reach;
import onl.luka.grizzly.module.modules.minigames.Bedwars;
import onl.luka.grizzly.module.modules.world.Nuker;
import onl.luka.grizzly.module.modules.world.BedBreaker;
import onl.luka.grizzly.module.modules.combat.Misplace;
import onl.luka.grizzly.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 2000)
public class MinecraftMixin {

    @Mutable @Shadow public HitResult hitResult;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void medved$prepareAutoBlockAttack(CallbackInfo ci) {
        AutoBlock.beforeHandleKeybinds((Minecraft) (Object) this);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void medved$preventBedwarsMisplace(CallbackInfo ci) {
        if (Bedwars.shouldCancelUseItem((Minecraft) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void medved$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        Player player = mc.player;

        if (player == null) {
            return;
        }

        boolean isEntityHit = hitResult != null
                && hitResult.getType() == HitResult.Type.ENTITY;

        if (HitSelect.shouldCancelAttack()) {
            if (HitSelect.shouldFakeSwing()) {
                player.swing(InteractionHand.MAIN_HAND);
            }

            cir.setReturnValue(false);
            return;
        }

        if (!AutoBlock.beforeVanillaAttack(mc)) {
            cir.setReturnValue(false);
            return;
        }

        if (!isEntityHit) {
            HitSelect.notifyMissedSwing();
        }

        if (onl.luka.grizzly.module.modules.combat.HitSwap.INSTANCE.isEnabled()) {
            onl.luka.grizzly.module.modules.combat.HitSwap.INSTANCE.onStartAttack();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void medved$onContinueAttack(boolean leftClick, CallbackInfo ci) {
        HitResult nukerHit = Nuker.overrideVanillaHitResult();
        if (nukerHit != null) {
            this.hitResult = nukerHit;
        } else if (leftClick && Nuker.shouldSuppressVanillaAttack()) {
            ci.cancel();
            return;
        }
        if (leftClick && onl.luka.grizzly.module.modules.combat.HitSwap.INSTANCE.isEnabled()) {
            onl.luka.grizzly.module.modules.combat.HitSwap.INSTANCE.onStartAttack();
        }
    }

    // Crosshair picking runs outside GameRenderer.render, so the shift has to be applied a
    // second time around it or you would aim at a player the pick never sees.
    @Inject(method = "pick", at = @At("HEAD"))
    private void medved$misplaceBeforePick(float partialTick, CallbackInfo ci) {
        Misplace.applyForFrame();
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void medved$misplaceAfterPick(float partialTick, CallbackInfo ci) {
        Misplace.restoreAfterFrame();
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void medved$overrideHitResult(float partialTick, CallbackInfo ci) {
        if (RotationManager.isActive()) {
            RotationManager.updateHitResult();
        }

        BlockPos bbPos = BedBreaker.INSTANCE.isEnabled() ? BedBreaker.pendingHitPos : null;
        if (bbPos != null) {
            Direction bbFace = BedBreaker.pendingHitFace;
            Vec3 center = new Vec3(bbPos.getX() + 0.5, bbPos.getY() + 0.5, bbPos.getZ() + 0.5);
            this.hitResult = new BlockHitResult(center, bbFace, bbPos, false);
        }

        if (!Reach.INSTANCE.isEnabled()) return;
        double range = Reach.INSTANCE.getTickReach();
        if (range <= 0) return;

        Minecraft mc = (Minecraft)(Object) this;
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        Vec3 eye = player.getEyePosition(partialTick);

        if (this.hitResult instanceof EntityHitResult ehr) {
            double dist = eye.distanceTo(ehr.getEntity().getBoundingBox().getCenter());
            float lo = ((Number) Reach.INSTANCE.getCustomRange().getValue().component1()).floatValue();
            if (dist > 3.0 && dist < lo) {
                this.hitResult = BlockHitResult.miss(eye, Direction.UP, BlockPos.containing(eye));
                return;
            }
        }

        if (!Reach.INSTANCE.getHitThroughWalls().getValue()) return;
        Vec3 look = player.getLookAngle();
        AABB scanBox = player.getBoundingBox().inflate(range + 1.0);

        Entity best = null;
        double bestDot = 0.95; // cos ~18 deg
        for (Entity e : mc.level.getEntities(player, scanBox)) {
            if (!e.isPickable()) continue;
            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eye);
            double dist = toEntity.length();
            if (dist > range) continue;
            double dot = toEntity.normalize().dot(look);
            if (dot > bestDot) { bestDot = dot; best = e; }
        }
        if (best != null) this.hitResult = new EntityHitResult(best);
    }

    @Shadow
    protected int missTime;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V"))
    private void moveCooldownIncrement(CallbackInfo ci) {
        if (NoHitDelay.INSTANCE.isEnabled()) {
            if (this.missTime > 0) {
                --this.missTime;
            }
        }
    }

    @WrapOperation(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasMissTime()Z", ordinal = 1))
    private boolean removeHitPenalty(MultiPlayerGameMode instance, Operation<Boolean> original) {
        return !NoHitDelay.INSTANCE.isEnabled() && original.call(instance);
    }
}
