package dev.ambience.util;

import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.FreelookHook;
import dev.ambience.inject.Access;
import dev.ambience.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class SilentAim implements Util {
   private static float camYaw;
   private static float camPitch;
   private static boolean active;
   private static SilentAim.a mode = SilentAim.a.NONE;
   private static boolean deferred;
   private static SilentAim.a deferredMode = SilentAim.a.NONE;

   private SilentAim() {
   }

   public static boolean a() {
      return active;
   }

   public static SilentAim.a b() {
      return mode;
   }

   public static boolean a(SilentAim.a var0) {
      return active && mode == var0;
   }

   public static float c() {
      return camYaw;
   }

   public static float d() {
      return camPitch;
   }

   public static void a(float var0, float var1) {
      a(var0, var1, SilentAim.a.COMBAT);
   }

   public static void a(float var0, float var1, SilentAim.a var2) {
      if (active) {
         if (var2 != SilentAim.a.OMNI || mode != SilentAim.a.COMBAT) {
            if (var2 == SilentAim.a.COMBAT) {
               mode = SilentAim.a.COMBAT;
            }
         }
      } else {
         if (FreecamHook.isActive()) {
            camYaw = FreecamHook.getYaw();
            camPitch = FreecamHook.getPitch();
         } else if (FreelookHook.isActive()) {
            camYaw = FreelookHook.getYaw();
            camPitch = FreelookHook.getPitch();
         } else {
            camYaw = var0;
            camPitch = var1;
         }

         active = true;
         mode = var2;
         j();
         DebugLogger.a("begin", var2.name() + String.format(" camYaw=%.10f camPitch=%.10f", var0, var1));
      }
   }

   public static void a(double var0, double var2) {
      if (active) {
         camYaw += (float)var0 * 0.15F;
         camPitch += (float)var2 * 0.15F;
         camYaw = MathHelper.wrapDegrees(camYaw);
         camPitch = MathHelper.clamp(camPitch, -90.0F, 90.0F);
      }
   }

   public static void b(float var0, float var1) {
      if (active) {
         camYaw = MathHelper.wrapDegrees(var0);
         camPitch = MathHelper.clamp(var1, -90.0F, 90.0F);
      }
   }

   public static void e() {
      b(SilentAim.a.COMBAT);
   }

   public static void f() {
      if (deferred) {
         SilentAim.a var0 = deferredMode;
         deferred = false;
         deferredMode = SilentAim.a.NONE;
         b(var0);
      }
   }

   public static void b(SilentAim.a var0) {
      if (!d(var0)) {
         deferred = false;
         deferredMode = SilentAim.a.NONE;
      } else if (RotationManager.e()) {
         deferred = true;
         deferredMode = var0;
         DebugLogger.a("end-defer", var0.name());
      } else {
         deferred = false;
         deferredMode = SilentAim.a.NONE;
         if (!FreelookHook.isActive() && !FreecamHook.isActive()) {
            h();
            DebugLogger.a("end-restore", var0.name());
         } else {
            DebugLogger.a("end-keep-body", var0.name());
         }

         i();
      }
   }

   public static void c(SilentAim.a var0) {
      if (d(var0)) {
         DebugLogger.a("clear", var0.name());
         i();
      }
   }

   public static void g() {
      if (active) {
         DebugLogger.a("force-clear", mode.name());
         active = false;
         mode = SilentAim.a.NONE;
         deferred = false;
         deferredMode = SilentAim.a.NONE;
      }
   }

   private static boolean d(SilentAim.a var0) {
      if (!active) {
         return false;
      } else {
         return var0 == SilentAim.a.COMBAT && mode == SilentAim.a.OMNI ? false : var0 != SilentAim.a.OMNI || mode != SilentAim.a.COMBAT;
      }
   }

   private static void h() {
      ClientPlayerEntity var0 = mc.player;
      if (var0 != null) {
         float var1 = Access.yRot(var0);
         float var2 = Access.xRot(var0);
         float var3 = GcdFix.a(var1, camYaw);
         float var4 = GcdFix.b(var2, camPitch);
         Access.setYRot(var0, var3);
         Access.setXRot(var0, var4);
         camYaw = var3;
         camPitch = var4;
         Access.setYRotO(var0, var3);
         Access.setXRotO(var0, var4);
      }
   }

   private static void i() {
      FreelookHook.absorbCamera(camYaw, camPitch);
      active = false;
      mode = SilentAim.a.NONE;
      deferred = false;
      deferredMode = SilentAim.a.NONE;
   }

   private static void j() {
      ClientPlayerEntity var0 = mc.player;
      if (var0 != null) {
         Access.setYRotO(var0, var0.getYaw());
         Access.setXRotO(var0, var0.getPitch());
      }
   }

   public enum a {
      NONE,
      COMBAT,
      OMNI;
   }
}
