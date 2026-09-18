package dev.ambience.util;

import dev.ambience.util.traits.Util;
import net.minecraft.util.math.MathHelper;

public final class GcdFix implements Util {
   private GcdFix() {
   }

   public static float a(float var0, float var1) {
      float var2 = MathHelper.wrapDegrees(var1 - var0);
      return var0 + (float)a(var2);
   }

   public static float b(float var0, float var1) {
      float var2 = var1 - var0;
      return MathHelper.clamp(var0 + (float)a(var2), -90.0F, 90.0F);
   }

   private static double a(double var0) {
      double var2 = a();
      return var2 < 1.0E-4 ? var0 : Math.round(var0 / var2) * var2;
   }

   private static double a() {
      double var0 = (Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      return var0 * var0 * var0 * 8.0 * 0.15;
   }
}
