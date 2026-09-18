package dev.ambience.util.render;

import net.minecraft.client.gui.DrawContext;

public class ScissorUtil {
   private static int X_1;
   private static int Y_1;
   private static int X_2;
   private static int Y_2;

   public static void enable(DrawContext var0, int var1, int var2, int var3, int var4) {
      var0.enableScissor(var1, var2, var3, var4);
      X_1 = var1;
      Y_1 = var2;
      X_2 = var3;
      Y_2 = var4;
   }

   public static void disable(DrawContext var0) {
      var0.disableScissor();
   }

   public static void enable(DrawContext var0) {
      var0.enableScissor(X_1, Y_1, X_2, Y_2);
   }
}
