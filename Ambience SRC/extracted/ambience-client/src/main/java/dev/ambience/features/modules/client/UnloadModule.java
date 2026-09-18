package dev.ambience.features.modules.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ambience.Ambience;
import dev.ambience.features.modules.Module;
import net.minecraft.text.Text;

public class UnloadModule extends Module {
   public UnloadModule() {
      super("Unload", "Unload Ambience from RAM (inject also strips mixins; toggle off to load again)", Module.Category.CLIENT);
   }

   @Override
   public void onEnable() {
      Ambience.unloadFromMemory("module");
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("Ambience unloaded from memory"), false);
      }
   }

   @Override
   public void onDisable() {
      Ambience.reloadIntoMemory();
      if (mc.player != null && Ambience.isInMemory()) {
         mc.player.sendMessage(Text.literal("Ambience loaded back into memory"), false);
      }
   }

   @Override
   public void fromJson(JsonElement var1) {
      if (var1 != null && !var1.isJsonNull() && var1.isJsonObject()) {
         JsonObject var2 = var1.getAsJsonObject().deepCopy();
         var2.addProperty("Enabled", false);
         super.fromJson(var2);
      }
   }
}
