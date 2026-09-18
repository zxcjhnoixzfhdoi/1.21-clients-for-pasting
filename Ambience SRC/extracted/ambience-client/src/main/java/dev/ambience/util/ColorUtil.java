package dev.ambience.util;

import dev.ambience.hooks.GuiRuntime;
import java.awt.Color;

public class ColorUtil {
   public static int a(int var0, int var1, int var2, int var3) {
      return new Color(var0, var1, var2, var3).getRGB();
   }

   public static int a(int var0, int var1, int var2) {
      return b(var0, var1, var2, 255);
   }

   public static int b(int var0, int var1, int var2, int var3) {
      return (var0 << 16) + (var1 << 8) + var2 + (var3 << 24);
   }

   public static int a(float var0, float var1, float var2, float var3) {
      return b((int)(var0 * 255.0F), (int)(var1 * 255.0F), (int)(var2 * 255.0F), (int)(var3 * 255.0F));
   }

   public static Color a(int var0) {
      double var1 = Math.ceil((System.currentTimeMillis() + var0) / 20.0);
      return Color.getHSBColor((float)(var1 % 360.0 / 360.0), GuiRuntime.rainbowSaturation() / 255.0F, GuiRuntime.rainbowBrightness() / 255.0F);
   }

   public static int a(float[] var0) {
      if (var0.length != 4) {
         throw new IllegalArgumentException("colors[] must have a length of 4!");
      } else {
         return a(var0[0], var0[1], var0[2], var0[3]);
      }
   }

   public static int a(double[] var0) {
      if (var0.length != 4) {
         throw new IllegalArgumentException("colors[] must have a length of 4!");
      } else {
         return a((float)var0[0], (float)var0[1], (float)var0[2], (float)var0[3]);
      }
   }

   public static int a(Color var0) {
      return b(var0.getRed(), var0.getGreen(), var0.getBlue(), var0.getAlpha());
   }
}
