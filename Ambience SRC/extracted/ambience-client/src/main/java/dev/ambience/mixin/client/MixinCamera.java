package dev.ambience.mixin.client;

import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.FreelookHook;
import dev.ambience.util.SilentAim;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {
   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract void setPos(Vec3d var1);

   @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private boolean ambience$freecamDetach(boolean var1) {
      return var1 || FreecamHook.isActive();
   }

   @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4184;setRotation(FF)V"))
   private void ambience$silentCamera(Args var1, World var2, Entity var3, boolean var4, boolean var5, float var6) {
      if (var3 instanceof ClientPlayerEntity) {
         if (FreecamHook.isActive()) {
            var1.set(0, FreecamHook.getYaw());
            var1.set(1, FreecamHook.getPitch());
         } else if (SilentAim.a()) {
            var1.set(0, SilentAim.c());
            var1.set(1, SilentAim.d());
         } else {
            if (FreelookHook.isActive()) {
               var1.set(0, FreelookHook.getYaw());
               var1.set(1, FreelookHook.getPitch());
            }
         }
      }
   }

   @Inject(method = "update", at = @At("RETURN"))
   private void ambience$freecamPosition(World var1, Entity var2, boolean var3, boolean var4, float var5, CallbackInfo var6) {
      if (FreecamHook.isActive()) {
         this.setRotation(FreecamHook.getYaw(), FreecamHook.getPitch());
         this.setPos(FreecamHook.getCameraPos(var5));
      }
   }
}
