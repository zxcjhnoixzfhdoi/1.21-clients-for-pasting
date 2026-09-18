package dev.ambience.features;

import dev.ambience.features.settings.Setting;
import dev.ambience.features.settings.SettingFactory;
import dev.ambience.util.traits.Util;
import java.util.ArrayList;
import java.util.List;

public class Feature implements SettingFactory, Util {
   public List<Setting<?>> settings = new ArrayList<>();
   private String name;

   public Feature() {
   }

   public Feature(String var1) {
      this.name = var1;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }

   public String getName() {
      return this.name;
   }

   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   public boolean isEnabled() {
      return false;
   }

   public boolean isDisabled() {
      return !this.isEnabled();
   }

   @Override
   public <T extends Setting<?>> T register(T var1) {
      var1.setFeature(this);
      this.settings.add(var1);
      return (T)var1;
   }

   public Setting<?> getSettingByName(String var1) {
      for (Setting var3 : this.settings) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public void reset() {
      for (Setting var2 : this.settings) {
         var2.reset();
      }
   }
}
