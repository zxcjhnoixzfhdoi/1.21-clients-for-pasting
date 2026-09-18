package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.LowFireHook;
import dev.ambience.hooks.TotemAnimationHook;
import dev.ambience.inject.Access;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

@Mixin(InGameOverlayRenderer.class)
public class MixinScreenEffectInject {
   @Inject(method = "renderFireOverlay", at = "HEAD", captureArgs = true)
   public static void lowFireTransform(MatrixStack var0, VertexConsumerProvider var1, Sprite var2) {
      if (LowFireHook.enabled()) {
         var0.push();
         var0.translate(0.0F, LowFireHook.fireYOffset(), 0.0F);
         var0.scale(1.0F, LowFireHook.fireHeightScale(), 1.0F);
      }
   }

   @Inject(method = "renderFireOverlay", at = "RETURN", captureArgs = true)
   public static void lowFirePop(MatrixStack var0, VertexConsumerProvider var1, Sprite var2) {
      if (LowFireHook.enabled()) {
         var0.pop();
      }
   }

   @Inject(method = "setFloatingItem", at = "RETURN", captureArgs = true)
   public static void totemActivationLength(InGameOverlayRenderer var0, ItemStack var1, Random var2) {
      Access.setItemActivationTicks(var0, TotemAnimationHook.animationLength());
   }

   @Inject(method = "renderFloatingItem", at = "HEAD", captureArgs = true)
   public static void smallerTotem(InGameOverlayRenderer var0, MatrixStack var1, float var2, OrderedRenderCommandQueue var3) {
      float var4 = TotemAnimationHook.animationScale();
      if (var4 != 1.0F) {
         var1.scale(var4, var4, var4);
      }
   }
}
