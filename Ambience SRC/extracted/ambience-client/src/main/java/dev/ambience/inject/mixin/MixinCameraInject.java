package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.FreelookHook;
import dev.ambience.inject.Access;
import dev.ambience.util.SilentAim;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Mixin(Camera.class)
public class MixinCameraInject {
   @Inject(method = "update", at = "RETURN", captureArgs = true)
   public static void afterSetup(Camera var0, World var1, Entity var2, boolean var3, boolean var4, float var5) {
      if (var2 instanceof ClientPlayerEntity) {
         if (FreecamHook.isActive()) {
            Access.cameraSetRotation(var0, FreecamHook.getYaw(), FreecamHook.getPitch());
            Access.cameraSetPosition(var0, FreecamHook.getCameraPos(var5));
         } else if (SilentAim.a()) {
            Access.cameraSetRotation(var0, SilentAim.c(), SilentAim.d());
         } else {
            if (FreelookHook.isActive()) {
               Access.cameraSetRotation(var0, FreelookHook.getYaw(), FreelookHook.getPitch());
            }
         }
      }
   }
}
