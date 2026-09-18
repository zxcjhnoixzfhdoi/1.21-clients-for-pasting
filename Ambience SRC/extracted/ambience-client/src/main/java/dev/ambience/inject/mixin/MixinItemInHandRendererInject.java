package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.ViewModelHook;
import dev.ambience.inject.Access;
import dev.ambience.inject.InjectState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(HeldItemRenderer.class)
public class MixinItemInHandRendererInject {
   @Inject(method = "renderItem", at = "HEAD", cancellable = true, captureArgs = true)
   public static void hideHandsInFreecam(HeldItemRenderer var0, float var1, MatrixStack var2, OrderedRenderCommandQueue var3, ClientPlayerEntity var4, int var5) {
      if (FreecamHook.isActive()) {
         Cancel.cancel();
      } else if (ViewModelHook.noSway() && var4 != null) {
         InjectState.ITEM_NO_SWAY.set(new float[]{Access.xBob(var4), Access.xBobO(var4), Access.yBob(var4), Access.yBobO(var4)});
         float var6 = var4.getPitch();
         float var7 = var4.getYaw();
         Access.setXBob(var4, var6);
         Access.setXBobO(var4, var6);
         Access.setYBob(var4, var7);
         Access.setYBobO(var4, var7);
      }
   }

   @Inject(method = "renderItem", at = "RETURN", captureArgs = true)
   public static void noSwayPost(HeldItemRenderer var0, float var1, MatrixStack var2, OrderedRenderCommandQueue var3, ClientPlayerEntity var4, int var5) {
      float[] var6 = InjectState.ITEM_NO_SWAY.get();
      InjectState.ITEM_NO_SWAY.remove();
      if (var6 != null && !FreecamHook.isActive() && var4 != null) {
         Access.setXBob(var4, var6[0]);
         Access.setXBobO(var4, var6[1]);
         Access.setYBob(var4, var6[2]);
         Access.setYBobO(var4, var6[3]);
      }
   }

   @Inject(method = "shouldSkipHandAnimationOnSwap", at = "HEAD", cancellable = true, captureArgs = true)
   public static void noSwapAnimation(HeldItemRenderer var0, ItemStack var1, ItemStack var2) {
      if (ViewModelHook.noSwapAnimation()) {
         Cancel.cancel(Boolean.TRUE);
      }
   }

   @Inject(method = "updateHeldItems", at = "RETURN", captureArgs = true)
   public static void oldAnimation(HeldItemRenderer var0) {
      if (ViewModelHook.oldAnimation()) {
         Access.setMainHandHeight(var0, 1.0F);
         Access.setOMainHandHeight(var0, 1.0F);
         Access.setOffHandHeight(var0, 1.0F);
         Access.setOOffHandHeight(var0, 1.0F);
      }
   }

   @Inject(method = "renderFirstPersonItem", at = "HEAD", captureArgs = true)
   public static void viewModelPush(
      HeldItemRenderer var0,
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10
   ) {
      InjectState.ITEM_VIEW_SCALED.set(ViewModelHook.applyArm(var1, var4, var8));
   }

   @Inject(method = "renderFirstPersonItem", at = "RETURN", captureArgs = true)
   public static void viewModelPop(
      HeldItemRenderer var0,
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10
   ) {
      if (Boolean.TRUE.equals(InjectState.ITEM_VIEW_SCALED.get())) {
         InjectState.ITEM_VIEW_SCALED.set(Boolean.FALSE);
         var8.pop();
      }
   }
}
