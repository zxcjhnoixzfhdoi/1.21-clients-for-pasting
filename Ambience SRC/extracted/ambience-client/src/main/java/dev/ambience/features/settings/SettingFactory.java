package dev.ambience.features.settings;

import java.awt.Color;
import org.joml.Vector2f;

public interface SettingFactory {
   <T extends Setting<?>> T register(T var1);

   default Setting<Boolean> bool(String var1, boolean var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default <T extends Number> Setting<T> num(String var1, T var2, T var3, T var4) {
      return this.register(new Setting<>(var1, (T)var2, (T)var3, (T)var4));
   }

   default Setting<String> str(String var1, String var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default <T extends Enum<?>> Setting<T> mode(String var1, T var2) {
      return this.register(new Setting<>(var1, (T)var2));
   }

   default Setting<Bind> key(String var1, Bind var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default Setting<Color> color(String var1, Color var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default Setting<Color> color(String var1, int var2, int var3, int var4, int var5) {
      return this.register(new Setting<>(var1, new Color(var2, var3, var4, var5)));
   }

   default Setting<Vector2f> vec2f(String var1, Vector2f var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default Setting<Vector2f> vec2f(String var1, float var2, float var3) {
      return this.register(new Setting<>(var1, new Vector2f(var2, var3)));
   }

   default Setting<BezierCurve> curve(String var1, BezierCurve var2) {
      return this.register(new Setting<>(var1, var2));
   }

   default Setting<BezierCurve> curve(String var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return this.register(new Setting<>(var1, new BezierCurve(var2, var3, var4, var5, var6, var7)));
   }
}
