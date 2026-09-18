package dev.ambience.util.render;

import dev.ambience.features.modules.Module;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public final class GuiIcons {
   public static final String SWORDS = "swords";
   public static final String GLOBE = "globe";
   public static final String IMAGE = "image";
   public static final String EYE = "eye";
   public static final String MOVE_RIGHT = "move_right";
   public static final String USER = "user";
   public static final String SPARKLES = "sparkles";
   public static final String SLIDERS = "sliders_horizontal";
   public static final String LAYOUT = "layout_dashboard";
   public static final String FOLDER = "folder";
   public static final String FIRE = "fire";
   public static final String SEARCH = "search";
   public static final String CLOSE = "x";
   public static final String BACK = "chevron_left";
   private static final Map<String, GuiIcons.IconTex> CACHE = new HashMap<>();
   private static final EnumMap<Module.Category, String> CATEGORY = new EnumMap<>(Module.Category.class);

   private GuiIcons() {
   }

   public static void category(DrawContext var0, Module.Category var1, float var2, float var3, float var4, int var5) {
      String var6 = var1 == null ? "sliders_horizontal" : CATEGORY.getOrDefault(var1, "sliders_horizontal");
      draw(var0, var6, var2, var3, var4, var5);
   }

   public static void draw(DrawContext var0, String var1, float var2, float var3, float var4, int var5) {
      draw(var0, var1, var2, var3, var4, var4, var5);
   }

   public static void draw(DrawContext var0, String var1, float var2, float var3, float var4, float var5, int var6) {
      GuiIcons.IconTex var7 = texture(var1);
      if (var7 != null) {
         int var8 = Math.max(1, Math.round(var4));
         int var9 = Math.max(1, Math.round(var5));
         var0.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            var7.id,
            Math.round(var2),
            Math.round(var3),
            0.0F,
            0.0F,
            var8,
            var9,
            var7.width,
            var7.height,
            var7.width,
            var7.height,
            GuiFade.apply(var6)
         );
      }
   }

   private static GuiIcons.IconTex texture(String var0) {
      GuiIcons.IconTex var1 = CACHE.get(var0);
      if (var1 != null) {
         return var1;
      }

      if (MinecraftClient.getInstance() == null) {
         return null;
      }

      String var2 = "/assets/ambience/textures/gui/icons/" + var0 + ".png";

      try (InputStream var3 = GuiIcons.class.getResourceAsStream(var2)) {
         if (var3 == null) {
            return null;
         }

         NativeImage var4 = LinearTextures.readPng(var3);
         int var5 = var4.getWidth();
         int var6 = var4.getHeight();
         GuiIcons.IconTex var7 = new GuiIcons.IconTex(LinearTextures.register("icon_" + var0, var4), var5, var6);
         CACHE.put(var0, var7);
         return var7;
      } catch (Exception var11) {
         return null;
      }
   }

   static {
      CATEGORY.put(Module.Category.COMBAT, "swords");
      CATEGORY.put(Module.Category.WORLD, "globe");
      CATEGORY.put(Module.Category.RENDER, "image");
      CATEGORY.put(Module.Category.VISUALS, "eye");
      CATEGORY.put(Module.Category.MOVEMENT, "move_right");
      CATEGORY.put(Module.Category.PLAYER, "user");
      CATEGORY.put(Module.Category.FUNNY, "sparkles");
      CATEGORY.put(Module.Category.CLIENT, "sliders_horizontal");
      CATEGORY.put(Module.Category.CONFIGS, "folder");
      CATEGORY.put(Module.Category.HUD, "layout_dashboard");
   }

   private record IconTex(Identifier id, int width, int height) {
   }
}
