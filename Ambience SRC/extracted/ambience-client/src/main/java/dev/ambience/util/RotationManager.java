package dev.ambience.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationManager {
   private static final double a = 0.003;
   private static final float b = 0.02F;
   private static final float c = 0.01F;
   private static boolean d;
   private static boolean e;
   private static boolean f;
   private static float g;
   private static float h;

   private RotationManager() {
   }

   public static void a() {
      d = false;
      SilentAim.f();
   }

   public static void a(ClientPlayerEntity var0) {
      if (var0 != null) {
         g = var0.getYaw();
         h = var0.getPitch();
         f = true;
         e = false;
      }
   }

   public static void b() {
      d = true;
      DebugLogger.b("markAction", "");
   }

   public static void c() {
      e = true;
      b();
   }

   public static boolean d() {
      return d;
   }

   public static boolean e() {
      return e;
   }

   public static boolean f() {
      return f;
   }

   public static float g() {
      return g;
   }

   public static float h() {
      return h;
   }

   public static boolean b(ClientPlayerEntity var0) {
      return f && var0 != null ? Math.abs(MathHelper.wrapDegrees(var0.getYaw() - g)) <= 0.01F && Math.abs(var0.getPitch() - h) <= 0.01F : false;
   }

   public static boolean a(ClientPlayerEntity var0, BlockPos var1, double var2) {
      return f && var0 != null && var1 != null && RaytraceUtil.a(var0, h, g, var1, var2);
   }

   public static boolean b(ClientPlayerEntity var0, BlockPos var1, double var2) {
      return var0 != null && var1 != null && RaytraceUtil.a(var0, var0.getPitch(), var0.getYaw(), var1, var2);
   }

   public static boolean c(ClientPlayerEntity var0, BlockPos var1, double var2) {
      return a(var0, var1, var2) && b(var0, var1, var2);
   }

   public static boolean a(ClientPlayerEntity var0, Entity var1, double var2) {
      return f && var0 != null && var1 != null && RaytraceUtil.a(var0, h, g, var1, var2);
   }

   public static boolean a(ClientPlayerEntity var0, Box var1, double var2) {
      if (f && var0 != null && var1 != null) {
         Vec3d var4 = var0.getEyePos();
         if (var1.contains(var4)) {
            return true;
         }

         Vec3d var5 = var4.add(RaytraceUtil.a(h, g).multiply(var2));
         return var1.raycast(var4, var5).isPresent();
      } else {
         return false;
      }
   }

   public static boolean b(ClientPlayerEntity var0, Entity var1, double var2) {
      return var0 != null && var1 != null && RaytraceUtil.a(var0, var0.getPitch(), var0.getYaw(), var1, var2);
   }

   public static boolean c(ClientPlayerEntity var0, Entity var1, double var2) {
      return a(var0, var1, var2) && b(var0, var1, var2);
   }

   public static boolean c(ClientPlayerEntity var0) {
      if (var0 == null) {
         return false;
      } else if (!var0.isOnGround()) {
         return true;
      } else {
         return var0.input != null && var0.input.getMovementInput().lengthSquared() > 0.02F ? true : var0.getVelocity().horizontalLengthSquared() > 0.003;
      }
   }

   public static boolean d(ClientPlayerEntity var0) {
      if (var0 == null) {
         return true;
      } else {
         return !var0.isSprinting() || var0.isSwimming() && var0.isOnGround() ? e(var0) : true;
      }
   }

   public static boolean e(ClientPlayerEntity var0) {
      if (var0 != null && var0.input != null) {
         PlayerInput var1 = var0.input.playerInput;
         return var1.forward() || var1.backward() || var1.left() || var1.right() || var1.jump();
      } else {
         return false;
      }
   }

   public static boolean a(float var0, float var1, float var2, float var3) {
      return !f ? false : Math.abs(MathHelper.wrapDegrees(var0 - g)) <= var2 && Math.abs(var1 - h) <= var3;
   }

   public static float[] a(ClientPlayerEntity var0, Vec3d var1) {
      return var0 == null ? null : a(var0.getEyePos(), var1);
   }

   public static float[] a(Vec3d var0, Vec3d var1) {
      if (var0 != null && var1 != null) {
         double var2 = var1.x - var0.x;
         double var4 = var1.y - var0.y;
         double var6 = var1.z - var0.z;
         double var8 = Math.sqrt(var2 * var2 + var6 * var6);
         if (var8 < 1.0E-4 && Math.abs(var4) < 1.0E-4) {
            return null;
         }

         float var10 = MathHelper.wrapDegrees((float)(Math.toDegrees(Math.atan2(var6, var2)) - 90.0));
         float var11 = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(var4, var8))), -90.0F, 90.0F);
         return new float[]{var10, var11};
      } else {
         return null;
      }
   }

   public static boolean a(ClientPlayerEntity var0, float var1, float var2) {
      return var0 == null ? false : Math.abs(MathHelper.wrapDegrees(var1 - var0.getYaw())) <= 0.01F && Math.abs(var2 - var0.getPitch()) <= 0.01F;
   }

   public static void a(ClientPlayerEntity var0, float var1, float var2, boolean var3) {
      a(var0, var1, var2, var3, false);
   }

   public static void a(ClientPlayerEntity var0, float var1, float var2, boolean var3, boolean var4) {
      if (var0 != null) {
         if (!e || var4) {
            if (var3) {
               float var5 = SilentAim.a() ? SilentAim.c() : var0.getYaw();
               float var6 = SilentAim.a() ? SilentAim.d() : var0.getPitch();
               SilentAim.a(var5, var6, SilentAim.a.COMBAT);
            }

            if (!a(var0, var1, var2)) {
               RaytraceUtil.a(var0, var1, var2);
            }
         }
      }
   }

   public static void i() {
      e = false;
   }
}
