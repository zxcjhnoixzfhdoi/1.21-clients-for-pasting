package dev.ambience.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ambience.features.Feature;
import dev.ambience.util.traits.JsonSerializable;

public class CommandManager extends Feature implements JsonSerializable {
   private String a = ".";

   public CommandManager() {
      super("Commands");
   }

   public void a(String var1) {
      this.a = var1;
   }

   public String a() {
      return this.a;
   }

   public boolean b() {
      return false;
   }

   @Override
   public JsonElement toJson() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("Prefix", this.a);
      return var1;
   }

   @Override
   public void fromJson(JsonElement var1) {
      this.a = ".";
      if (var1 != null && !var1.isJsonNull()) {
         JsonObject var2 = var1.getAsJsonObject();
         if (var2.has("Prefix")) {
            this.a = var2.get("Prefix").getAsString();
         }
      }
   }
}
