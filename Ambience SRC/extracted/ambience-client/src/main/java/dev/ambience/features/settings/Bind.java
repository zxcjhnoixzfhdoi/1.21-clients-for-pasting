package dev.ambience.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.ambience.util.traits.Util;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Bind implements Util {
   public static final int MOUSE_BUTTON_OFFSET = 1000;
   private int key;

   public Bind(int var1) {
      this.key = var1;
   }

   public static Bind none() {
      return new Bind(-1);
   }

   public static Bind fromMouseButton(int var0) {
      return new Bind(1000 + var0);
   }

   public static boolean isMouseButton(int var0) {
      return var0 >= 1000;
   }

   public int getKey() {
      return this.key;
   }

   public void setKey(int var1) {
      this.key = var1;
   }

   public boolean isEmpty() {
      return this.key < 0;
   }

   @Override
   public String toString() {
      if (this.isEmpty()) {
         return "None";
      }

      if (isMouseButton(this.key)) {
         return "Mouse " + (this.key - 1000 + 1);
      }

      String var1 = InputUtil.fromKeyCode(new KeyInput(this.key, 0, 0)).getTranslationKey();
      String var2 = var1.replace("key.keyboard.", "").replace("key.mouse.", "").replace('.', ' ').trim();
      StringBuilder var3 = new StringBuilder(var2.length());

      for (String var7 : var2.split(" ")) {
         if (!var7.isEmpty()) {
            if (var3.length() > 0) {
               var3.append(' ');
            }

            var3.append(this.capitalise(var7));
         }
      }

      return var3.toString();
   }

   public boolean isDown() {
      if (this.isEmpty()) {
         return false;
      } else {
         return isMouseButton(this.key)
            ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), this.key - 1000) == 1
            : GLFW.glfwGetKey(mc.getWindow().getHandle(), this.getKey()) == 1;
      }
   }

   private String capitalise(String var1) {
      return var1.isEmpty() ? "" : Character.toUpperCase(var1.charAt(0)) + (var1.length() != 1 ? var1.substring(1).toLowerCase() : "");
   }

   public static class BindConverter extends Converter<Bind, JsonElement> {
      public JsonElement doForward(Bind var1) {
         return new JsonPrimitive(var1.toString());
      }

      public Bind doBackward(JsonElement var1) {
         String var2 = var1.getAsString();
         if (var2.equalsIgnoreCase("None")) {
            return Bind.none();
         }

         String var3 = var2.toUpperCase().replace("MOUSE", "M").replace(" ", "");
         if (var3.startsWith("M") && var3.length() >= 2 && Character.isDigit(var3.charAt(1))) {
            try {
               int var4 = Integer.parseInt(var3.substring(1)) - 1;
               if (var4 >= 0) {
                  return Bind.fromMouseButton(var4);
               }
            } catch (NumberFormatException var7) {
            }
         }

         int var8 = 0;
         try {
            var8 = InputUtil.fromTranslationKey(var2.toUpperCase()).getCode();
         } catch (Exception var6) {
         }

         return var8 == 0 ? Bind.none() : new Bind(var8);
      }
   }
}
