package dev.ambience.manager;

import dev.ambience.hooks.GuiRuntime;
import dev.ambience.util.ColorUtil;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ColorManager {
   private final Map<String, Supplier<Color>> a = new HashMap<>();

   public void a(String var1, Supplier<Color> var2) {
      this.a.put(var1, var2);
   }

   public Color a(String var1) {
      Supplier var2 = this.a.get(var1);
      return var2 != null ? (Color)var2.get() : Color.RED;
   }

   public int b(String var1) {
      return ColorUtil.a(this.a(var1));
   }

   public int c(String var1) {
      Color var2 = this.a(var1);
      return ColorUtil.a(new Color(var2.getRed(), var2.getGreen(), var2.getBlue(), 255));
   }

   public int a(String var1, float var2, int var3) {
      if (GuiRuntime.rainbowTheme()) {
         return ColorUtil.a((int)(var2 / 10.0F * GuiRuntime.rainbowHue())).getRGB();
      }

      Color var4 = this.a(var1);
      return new Color(var4.getRed(), var4.getGreen(), var4.getBlue(), var3).getRGB();
   }
}
