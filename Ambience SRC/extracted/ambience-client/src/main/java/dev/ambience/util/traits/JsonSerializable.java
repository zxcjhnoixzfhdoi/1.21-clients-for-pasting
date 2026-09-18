package dev.ambience.util.traits;

import com.google.gson.JsonElement;

public interface JsonSerializable {
   JsonElement toJson();

   void fromJson(JsonElement var1);

   default String e() {
      return "";
   }
}
