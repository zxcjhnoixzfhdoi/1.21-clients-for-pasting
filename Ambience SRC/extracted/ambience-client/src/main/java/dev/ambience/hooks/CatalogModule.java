package dev.ambience.hooks;

import dev.ambience.Ambience;
import dev.ambience.features.modules.Module;

public final class CatalogModule extends Module {
   public CatalogModule(String var1, String var2, Module.Category var3) {
      super(var1, var2, var3);
   }

   @Override
   public boolean isEnabled() {
      if (Ambience.e != null) {
         Module var1 = Ambience.e.getModuleByName(this.getName());
         return var1 != null && var1 != this ? var1.isEnabled() : Ambience.e.pendingEnabled(this.getName());
      } else {
         return super.isEnabled();
      }
   }

   @Override
   public void enable() {
      Module var1 = this.materialize();
      if (var1 != null && var1 != this) {
         var1.enable();
      }
   }

   @Override
   public void disable() {
      Module var1 = this.materialize();
      if (var1 != null && var1 != this) {
         var1.disable();
      }
   }

   private Module materialize() {
      return Ambience.e == null ? null : Ambience.e.ensureByName(this.getName());
   }
}
