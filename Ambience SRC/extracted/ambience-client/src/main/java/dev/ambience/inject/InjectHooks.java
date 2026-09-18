package dev.ambience.inject;

import dev.ambience.Ambience;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.util.RaytraceUtil;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import x.Render2DEvent;
import x.Render3DEvent;

public final class InjectHooks {
   private InjectHooks() {
   }

   public static void fireRender3D(float var0) {
      if (Ambience.e != null) {
         try {
            Ambience.e.onRender3D(new Render3DEvent(null, var0));
         } catch (Throwable var2) {
         }
      }
   }

   public static void fireRender2D(DrawContext var0, float var1) {
      if (Ambience.e != null && var0 != null) {
         try {
            Ambience.e.onRender2D(new Render2DEvent(var0, var1));
         } catch (Throwable var3) {
         }
      }
   }

   public static void cameraPick(float var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.player != null && var1.world != null) {
         if (FreecamHook.isActive()) {
            applyPick(var1, FreecamHook.getCameraPos(var0), FreecamHook.getPitch(), FreecamHook.getYaw());
         } else if (SilentAim.a()) {
            applyPick(var1, var1.player.getCameraPosVec(var0), SilentAim.d(), SilentAim.c());
         }
      }
   }

   private static void applyPick(MinecraftClient var0, Vec3d var1, float var2, float var3) {
      double var4 = var0.player.getBlockInteractionRange();
      double var6 = var0.player.getEntityInteractionRange();
      Object var8 = RaytraceUtil.a(var0.world, var0.player, var1, var2, var3, var4, var6);
      if (var8 == null) {
         Vec3d var9 = RaytraceUtil.a(var2, var3);
         Vec3d var10 = var1.add(var9.multiply(var4));
         var8 = BlockHitResult.createMissed(var10, Direction.getFacing(var9.x, var9.y, var9.z), BlockPos.ofFloored(var10));
      }

      var0.crosshairTarget = (HitResult)var8;
      var0.targetedEntity = var8 instanceof EntityHitResult var11 ? var11.getEntity() : null;
   }
}
