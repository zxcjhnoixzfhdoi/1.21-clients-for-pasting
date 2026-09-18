package dev.ambience.mixin.entity;

import dev.ambience.hooks.AutoSprintHook;
import dev.ambience.hooks.OmniSprintHook;
import dev.ambience.util.DebugLogger;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import x.PostTickEvent;
import x.PreTickEvent;
import x.TickEvent;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
   @Inject(method = "tick", at = @At("HEAD"))
   private void ambience$preTickHook(CallbackInfo var1) {
      Util.EVENT_BUS.post(new PreTickEvent());
   }

   @Inject(method = "canStartSprinting", at = @At("HEAD"))
   private void ambience$autoSprint(CallbackInfoReturnable<Boolean> var1) {
      ClientPlayerEntity var2 = ((ClientPlayerEntity)(Object)this);
      OmniSprintHook.apply(var2);
      AutoSprintHook.apply(var2);
   }

   @Inject(method = "applyMovementSpeedFactors", at = @At("RETURN"), cancellable = true)
   private void ambience$omniVector(Vec2f var1, CallbackInfoReturnable<Vec2f> var2) {
      if (SilentAim.a(SilentAim.a.OMNI)) {
         Vec2f var3 = (Vec2f)var2.getReturnValue();
         if (var3 != null && !(var3.lengthSquared() < 1.0E-8F)) {
            if (Math.abs(var3.x) > 1.0E-5F) {
               var2.setReturnValue(new Vec2f(0.0F, var3.length()));
            }
         }
      }
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;sendMovementPackets()V", shift = Shift.BEFORE))
   private void ambience$tickHook(CallbackInfo var1) {
      Util.EVENT_BUS.post(new TickEvent());
   }

   @Inject(method = "sendMovementPackets", at = @At("HEAD"))
   private void ambience$flying(CallbackInfo var1) {
      RotationManager.a(((ClientPlayerEntity)(Object)this));
      DebugLogger.f();
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;sendMovementPackets()V", shift = Shift.AFTER))
   private void ambience$postPositionHook(CallbackInfo var1) {
      Util.EVENT_BUS.post(new PostTickEvent());
   }
}
