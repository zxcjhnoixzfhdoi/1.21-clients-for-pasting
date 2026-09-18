package dev.ambience.mixin.client;

import dev.ambience.hooks.LowFireHook;
import dev.ambience.hooks.TotemAnimationHook;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameOverlayRenderer.class)
public class MixinScreenEffectRenderer {
   @Inject(method = "renderFireOverlay", at = @At("HEAD"))
   private static void ambience$lowFireTransform(MatrixStack var0, VertexConsumerProvider var1, Sprite var2, CallbackInfo var3) {
      if (LowFireHook.enabled()) {
         var0.push();
         var0.translate(0.0F, LowFireHook.fireYOffset(), 0.0F);
         var0.scale(1.0F, LowFireHook.fireHeightScale(), 1.0F);
      }
   }

   @Inject(method = "renderFireOverlay", at = @At("RETURN"))
   private static void ambience$lowFirePop(MatrixStack var0, VertexConsumerProvider var1, Sprite var2, CallbackInfo var3) {
      if (LowFireHook.enabled()) {
         var0.pop();
      }
   }

   @Redirect(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1058;getMinV()F"))
   private static float ambience$clipFireUv(Sprite var0) {
      float var1 = var0.getMinV();
      float var2 = var0.getMaxV();
      return !LowFireHook.enabled() ? var1 : var2 - LowFireHook.fireUvSlice(var1, var2);
   }

   @ModifyArgs(
      method = "renderFireOverlay",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4588;vertex(Lorg/joml/Matrix4fc;FFF)Lnet/minecraft/class_4588;")
   )
   private static void ambience$squashFireVertices(Args var0) {
      if (LowFireHook.enabled()) {
         float var1 = (Float)var0.get(2);
         if (var1 > 0.0F) {
            var0.set(2, -0.499F);
         }
      }
   }

   @Inject(method = "setFloatingItem", at = @At("TAIL"))
   private void ambience$totemActivationLength(ItemStack var1, Random var2, CallbackInfo var3) {
      if (TotemAnimationHook.enabled() && var1.isOf(Items.TOTEM_OF_UNDYING)) {
         ((ScreenEffectRendererAccessor)this).ambience$setItemActivationTicks(TotemAnimationHook.animationLength());
      }
   }

   @Inject(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4587;scale(FFF)V", shift = Shift.AFTER))
   private void ambience$smallerTotem(MatrixStack var1, float var2, OrderedRenderCommandQueue var3, CallbackInfo var4) {
      if (TotemAnimationHook.enabled()) {
         float var5 = TotemAnimationHook.animationScale() / 0.8F;
         var1.scale(var5, var5, var5);
      }
   }

   @ModifyConstant(method = "renderFloatingItem", constant = @Constant(intValue = 40))
   private int ambience$totemAnimationLengthInt(int var1) {
      return !TotemAnimationHook.enabled() ? var1 : TotemAnimationHook.animationLength();
   }

   @ModifyConstant(method = "renderFloatingItem", constant = @Constant(floatValue = 40.0F))
   private float ambience$totemAnimationLengthFloat(float var1) {
      return !TotemAnimationHook.enabled() ? var1 : TotemAnimationHook.animationLength();
   }
}
