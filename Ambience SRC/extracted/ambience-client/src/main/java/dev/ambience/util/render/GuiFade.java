package dev.ambience.util.render;

public class GuiFade {
   public static float alpha = 1.0F;

   public static int apply(int var0) {
      int var1 = (int)((var0 >> 24 & 0xFF) * alpha);
      return var0 & 16777215 | var1 << 24;
   }
}
