package dev.ambience.hooks;

import net.minecraft.client.gui.screen.Screen;

public final class GuiRuntime {
   private static volatile GuiRuntime.Host host;

   private GuiRuntime() {
   }

   public static void bind(GuiRuntime.Host var0) {
      host = var0;
   }

   public static GuiRuntime.Host get() {
      return host;
   }

   public static boolean modernBlur() {
      GuiRuntime.Host var0 = host;
      return var0 != null && var0.modernBlur();
   }

   public static boolean isClickGui(Screen var0) {
      GuiRuntime.Host var1 = host;
      return var1 != null && var1.isClickGui(var0);
   }

   public static boolean handleBindMouse(int var0, int var1) {
      GuiRuntime.Host var2 = host;
      return var2 != null && var2.handleBindMouse(var0, var1);
   }

   public static boolean clickGuiFont() {
      GuiRuntime.Host var0 = host;
      return var0 != null && var0.clickGuiFont();
   }

   public static boolean hudFont() {
      GuiRuntime.Host var0 = host;
      return var0 != null && var0.hudFont();
   }

   public static String fontName() {
      GuiRuntime.Host var0 = host;
      return var0 == null ? "" : var0.fontName();
   }

   public static float rainbowSaturation() {
      GuiRuntime.Host var0 = host;
      return var0 == null ? 140.0F : var0.rainbowSaturation();
   }

   public static float rainbowBrightness() {
      GuiRuntime.Host var0 = host;
      return var0 == null ? 200.0F : var0.rainbowBrightness();
   }

   public static int rainbowHue() {
      GuiRuntime.Host var0 = host;
      return var0 == null ? 240 : var0.rainbowHue();
   }

   public static boolean rainbowTheme() {
      GuiRuntime.Host var0 = host;
      return var0 != null && var0.rainbowTheme();
   }

   public interface Host {
      boolean modernBlur();

      boolean isClickGui(Screen var1);

      boolean handleBindMouse(int var1, int var2);

      boolean clickGuiFont();

      boolean hudFont();

      String fontName();

      float rainbowSaturation();

      float rainbowBrightness();

      int rainbowHue();

      boolean rainbowTheme();
   }
}
