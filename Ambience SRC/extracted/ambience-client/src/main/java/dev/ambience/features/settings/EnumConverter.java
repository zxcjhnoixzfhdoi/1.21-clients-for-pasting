package dev.ambience.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumConverter<T extends Enum<T>> extends Converter<T, JsonElement> {
   private final Class<T> clazz;

   public EnumConverter(Class<T> var1) {
      this.clazz = var1;
   }

   public static <T extends Enum<?>> int currentEnum(T var0) {
      for (int var1 = 0; var1 < ((Enum[])var0.getDeclaringClass().getEnumConstants()).length; var1++) {
         Enum var2 = ((Enum[])var0.getDeclaringClass().getEnumConstants())[var1];
         if (var2.name().equalsIgnoreCase(var0.name())) {
            return var1;
         }
      }

      return -1;
   }

   public static <T extends Enum<?>> T increaseEnum(T var0) {
      int var1 = currentEnum((T)var0);

      for (int var2 = 0; var2 < ((Enum[])var0.getDeclaringClass().getEnumConstants()).length; var2++) {
         Enum var3 = ((Enum[])var0.getDeclaringClass().getEnumConstants())[var2];
         if (var2 == var1 + 1) {
            return (T)var3;
         }
      }

      return (T)var0.getDeclaringClass().getEnumConstants()[0];
   }

   public static <T extends Enum<?>> String getProperName(T var0) {
      String[] var1 = var0.name().toLowerCase().split("_");
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1) {
         if (!var6.isEmpty()) {
            if (var2.length() > 0) {
               var2.append(' ');
            }

            var2.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }
      }

      return var2.toString();
   }

   public JsonElement doForward(Enum var1) {
      return new JsonPrimitive(var1.toString());
   }

   public T doBackward(JsonElement var1) {
      try {
         String var2 = var1.getAsString();
         if ("HOMOVORE".equalsIgnoreCase(var2) && this.clazz.getSimpleName().equals("Theme")) {
            var2 = "AMBIENCE";
         }

         return Enum.valueOf(this.clazz, var2);
      } catch (IllegalArgumentException var3) {
         return null;
      }
   }
}
