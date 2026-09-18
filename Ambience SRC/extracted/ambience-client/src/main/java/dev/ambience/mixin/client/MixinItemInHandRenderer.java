package dev.ambience.mixin.client;

import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.ViewModelHook;
import dev.ambience.mixin.entity.EntityRotationAccessor;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HeldItemRenderer.class)
public class MixinItemInHandRenderer {
   @Unique
   private boolean viewModel$scaled;
   @Unique
   private float noSway$savedXBob;
   @Unique
   private float noSway$savedXBobO;
   @Unique
   private float noSway$savedYBob;
   @Unique
   private float noSway$savedYBobO;
   @Shadow
   private float equipProgressMainHand;
   @Shadow
   private float lastEquipProgressMainHand;
   @Shadow
   private float equipProgressOffHand;
   @Shadow
   private float lastEquipProgressOffHand;

   @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
   private void ambience$hideHandsInFreecam(CallbackInfo var1) {
      if (FreecamHook.isActive()) {
         var1.cancel();
      }
   }

   @Inject(method = "renderItem", at = @At("HEAD"))
   private void ambience$noSwayPre(CallbackInfo var1) {
      if (!FreecamHook.isActive()) {
         if (ViewModelHook.noSway()) {
            ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
            if (var2 != null) {
               EntityRotationAccessor var3 = (EntityRotationAccessor)var2;
               this.noSway$savedXBob = var3.ambience$getXBob();
               this.noSway$savedXBobO = var3.ambience$getXBobO();
               this.noSway$savedYBob = var3.ambience$getYBob();
               this.noSway$savedYBobO = var3.ambience$getYBobO();
               float var4 = var2.getPitch();
               float var5 = var2.getYaw();
               var3.ambience$setXBob(var4);
               var3.ambience$setXBobO(var4);
               var3.ambience$setYBob(var5);
               var3.ambience$setYBobO(var5);
            }
         }
      }
   }

   @Inject(method = "renderItem", at = @At("RETURN"))
   private void ambience$noSwayPost(CallbackInfo var1) {
      if (!FreecamHook.isActive()) {
         if (ViewModelHook.noSway()) {
            ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
            if (var2 != null) {
               EntityRotationAccessor var3 = (EntityRotationAccessor)var2;
               var3.ambience$setXBob(this.noSway$savedXBob);
               var3.ambience$setXBobO(this.noSway$savedXBobO);
               var3.ambience$setYBob(this.noSway$savedYBob);
               var3.ambience$setYBobO(this.noSway$savedYBobO);
            }
         }
      }
   }

   @Inject(method = "shouldSkipHandAnimationOnSwap", at = @At("HEAD"), cancellable = true)
   private void ambience$noSwapAnimation(ItemStack var1, ItemStack var2, CallbackInfoReturnable<Boolean> var3) {
      if (ViewModelHook.noSwapAnimation()) {
         var3.setReturnValue(true);
      }
   }

   @Inject(method = "updateHeldItems", at = @At("TAIL"))
   private void ambience$oldAnimation(CallbackInfo var1) {
      if (ViewModelHook.oldAnimation()) {
         this.equipProgressMainHand = 1.0F;
         this.lastEquipProgressMainHand = 1.0F;
         this.equipProgressOffHand = 1.0F;
         this.lastEquipProgressOffHand = 1.0F;
      }
   }

   @ModifyVariable(
      method = "renderFirstPersonItem(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 1
   )
   private float ambience$silentHandPitch(float var1) {
      return SilentAim.a() ? SilentAim.d() : var1;
   }

   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V",
      at = @At("HEAD")
   )
   private void ambience$viewModelPush(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10,
      CallbackInfo var11
   ) {
      this.viewModel$scaled = ViewModelHook.applyArm(var1, var4, var8);
   }

   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V",
      at = @At("RETURN")
   )
   private void ambience$viewModelPop(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10,
      CallbackInfo var11
   ) {
      if (this.viewModel$scaled) {
         this.viewModel$scaled = false;
         var8.pop();
      }
   }
}
