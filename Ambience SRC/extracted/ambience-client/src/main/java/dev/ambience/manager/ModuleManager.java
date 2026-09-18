package dev.ambience.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ambience.Ambience;
import dev.ambience.features.Feature;
import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.client.DebugModule;
import dev.ambience.features.modules.client.UnloadModule;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.CatalogModule;
import dev.ambience.hooks.PayloadRuntime;
import dev.ambience.loader.auth.PayloadGuard;
import dev.ambience.util.traits.JsonSerializable;
import dev.ambience.util.traits.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import x.Render2DEvent;
import x.Render3DEvent;

public class ModuleManager implements Util, JsonSerializable {
   private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
   private final List<Module> modules = new ArrayList<>();
   private final List<ModuleManager.b> pending = new ArrayList<>();
   private boolean restoredEnabled;

   public void init() {
      this.register(new DebugModule());
      this.register(new UnloadModule());
   }

   public void offerCatalog(List<ModuleManager.a> var1) {
      for (ModuleManager.a var3 : var1) {
         if (var3 != null && var3.b != null && !var3.b.isBlank() && this.getModuleByName(var3.b) == null && this.pendingNamed(var3.b) == null) {
            this.pending.add(new ModuleManager.b(var3));
         }
      }
   }

   public void register(Module var1) {
      this.modules.add(var1);
      this.fastRegistry.put((Class<? extends Module>)var1.getClass(), var1);
      this.pending.removeIf(var1x -> var1x.a.b.equalsIgnoreCase(var1.getName()));
   }

   public List<Module> guiModules(Module.Category var1, String var2) {
      boolean var3 = var2 != null && !var2.isBlank();
      String var4 = var3 ? var2.toLowerCase() : "";
      ArrayList<Module> var5 = new ArrayList<>();

      for (Module var7 : this.modules) {
         if (!var7.hidden && (var3 ? var7.getName().toLowerCase().contains(var4) : var7.getCategory() == var1)) {
            var5.add(var7);
         }
      }

      for (ModuleManager.b var9 : this.pending) {
         if (var3 ? var9.a.b.toLowerCase().contains(var4) : var9.a.c == var1) {
            var5.add(this.stubOf(var9));
         }
      }

      var5.sort((var0, var1x) -> var0.getName().compareToIgnoreCase(var1x.getName()));
      return var5;
   }

   public boolean pendingEnabled(String var1) {
      ModuleManager.b var2 = this.pendingNamed(var1);
      return var2 != null && enabledIn(var2.b);
   }

   public Module ensureByName(String var1) {
      Module var2 = this.getModuleByName(var1);
      if (var2 != null) {
         return var2;
      }

      ModuleManager.b var3 = this.pendingNamed(var1);
      return var3 == null ? null : this.ensure(var3);
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public Stream<Module> stream() {
      return this.modules.stream();
   }

   public <T extends Module> T getModuleByClass(Class<T> var1) {
      return (T)this.fastRegistry.get(var1);
   }

   public Module getModuleByName(String var1) {
      return this.stream().filter(var1x -> var1x.getName().equalsIgnoreCase(var1)).findFirst().orElse(null);
   }

   public Module getModuleByDisplayName(String var1) {
      return this.stream().filter(var1x -> var1x.getDisplayName().equalsIgnoreCase(var1)).findFirst().orElse(null);
   }

   public List<Module> getModulesByCategory(Module.Category var1) {
      return this.stream().filter(var1x -> var1x.getCategory() == var1).toList();
   }

   public List<Module.Category> getCategories() {
      return Arrays.asList(Module.Category.values());
   }

   public void onLoad() {
      this.modules.forEach(Module::onLoad);
   }

   private Module ensure(ModuleManager.b var1) {
      Module var2 = this.getModuleByName(var1.a.b);
      if (var2 != null) {
         this.pending.remove(var1);
         return var2;
      }

      Module var3;
      try {
         var3 = PayloadRuntime.createModule(var1.a.b, var1.a.a);
      } catch (RuntimeException var5) {
         return null;
      }

      if (var3 == null) {
         return null;
      }

      this.register(var3);
      if (var1.b != null) {
         var3.fromJson(var1.b);
      }

      var3.onLoad();
      return var3;
   }

   private CatalogModule stubOf(ModuleManager.b var1) {
      if (var1.c == null) {
         var1.c = new CatalogModule(var1.a.b, var1.a.d, var1.a.c);
      }

      return var1.c;
   }

   private ModuleManager.b pendingNamed(String var1) {
      for (ModuleManager.b var3 : this.pending) {
         if (var3.a.b.equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   private void restoreEnabledInWorld() {
      if (!this.restoredEnabled && !Feature.nullCheck() && PayloadGuard.authorized()) {
         this.restoredEnabled = true;
         ArrayList var1 = new ArrayList();

         for (ModuleManager.b var3 : this.pending) {
            if (enabledIn(var3.b)) {
               var1.add(var3.a.b);
            }
         }

         if (!var1.isEmpty()) {
            try {
               PayloadRuntime.fetchModules(var1);
            } catch (RuntimeException var4) {
               return;
            }
         }

         for (ModuleManager.b var6 : List.copyOf(this.pending)) {
            if (enabledIn(var6.b)) {
               this.ensure(var6);
            }
         }
      }
   }

   private void forEnabled(Consumer<Module> var1) {
      for (Module var3 : this.stream().filter(Feature::isEnabled).toList()) {
         try {
            var1.accept(var3);
         } catch (NoClassDefFoundError var7) {
            Ambience.a.error("Module {} missing payload class", var3.getName(), var7);

            try {
               var3.disable();
            } catch (RuntimeException var6) {
            }
         } catch (LinkageError var8) {
            Ambience.a.error("Module {} linkage error", var3.getName(), var8);
         }
      }
   }

   private static boolean enabledIn(JsonElement var0) {
      if (var0 != null && var0.isJsonObject() && var0.getAsJsonObject().has("Enabled")) {
         try {
            return var0.getAsJsonObject().get("Enabled").getAsBoolean();
         } catch (RuntimeException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int bindIn(JsonElement var0) {
      if (var0 != null && var0.isJsonObject() && var0.getAsJsonObject().has("Keybind")) {
         try {
            return var0.getAsJsonObject().get("Keybind").getAsInt();
         } catch (RuntimeException var2) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public void onTick() {
      this.restoreEnabledInWorld();
      if (!PayloadGuard.authorized()) {
         this.stream().filter(var0 -> var0.getCategory() != Module.Category.CLIENT && var0.isEnabled()).toList().forEach(Module::disable);
      }

      this.forEnabled(Module::onTick);
      this.stream()
         .filter(var0 -> var0.isEnabled() && var0.getBindMode() == Module.BindMode.HOLD && !var0.getBind().isDown())
         .toList()
         .forEach(Module::disable);
   }

   public void onPreTick() {
      this.restoreEnabledInWorld();
      if (!PayloadGuard.authorized()) {
         this.stream().filter(var0 -> var0.getCategory() != Module.Category.CLIENT && var0.isEnabled()).toList().forEach(Module::disable);
      }

      this.forEnabled(Module::onPreTick);
   }

   public void onPostPositionTick() {
      this.forEnabled(Module::onPostPositionTick);
   }

   public void onRender2D(Render2DEvent var1) {
      this.forEnabled(var1x -> var1x.onRender2D(var1));
   }

   public void onRender3D(Render3DEvent var1) {
      this.forEnabled(var1x -> var1x.onRender3D(var1));
   }

   public void onUnload() {
      this.modules.forEach(EVENT_BUS::unregister);
      this.modules.forEach(Module::onUnload);
   }

   public void evictPayloadFromMemory() {
      this.restoredEnabled = false;

      for (Module var2 : List.copyOf(this.modules)) {
         if (!(var2 instanceof DebugModule) && !(var2 instanceof UnloadModule)) {
            if (var2.isEnabled()) {
               try {
                  var2.disable();
               } catch (Throwable var4) {
               }
            }

            try {
               EVENT_BUS.unregister(var2);
            } catch (Throwable var6) {
            }

            try {
               var2.onUnload();
            } catch (Throwable var5) {
            }

            this.modules.remove(var2);
            this.fastRegistry.remove(var2.getClass());
         }
      }

      this.pending.clear();
   }

   public void onKeyPressed(int var1) {
      if (var1 > 0) {
         for (ModuleManager.b var3 : List.copyOf(this.pending)) {
            if (bindIn(var3.b) == var1) {
               this.ensure(var3);
            }
         }

         this.stream().filter(var1x -> var1x.getBind().getKey() == var1).forEach(var0 -> {
            if (mc.currentScreen == null || var0.getCategory() == Module.Category.CLIENT) {
               if (var0.getBindMode() == Module.BindMode.HOLD) {
                  if (!var0.isEnabled()) {
                     var0.enable();
                  }
               } else {
                  var0.d();
               }
            }
         });
      }
   }

   public void onKeyReleased(int var1) {
      if (var1 > 0) {
         this.stream()
            .filter(var1x -> var1x.getBindMode() == Module.BindMode.HOLD && var1x.getBind().getKey() == var1 && var1x.isEnabled())
            .forEach(Module::disable);
      }
   }

   public void onMousePressed(int var1) {
      if (mc.currentScreen == null) {
         int var2 = 1000 + var1;
         this.onKeyPressed(var2);
      }
   }

   public void onMouseReleased(int var1) {
      int var2 = 1000 + var1;
      this.onKeyReleased(var2);
   }

   @Override
   public JsonElement toJson() {
      JsonObject var1 = new JsonObject();

      for (Module var3 : this.modules) {
         var1.add(var3.getName(), var3.toJson());
      }

      for (ModuleManager.b var5 : this.pending) {
         if (!var1.has(var5.a.b) && var5.b != null) {
            var1.add(var5.a.b, var5.b);
         }
      }

      return var1;
   }

   @Override
   public void fromJson(JsonElement var1) {
      if (var1 != null && var1.isJsonObject()) {
         JsonObject var2 = var1.getAsJsonObject();

         for (Module var4 : this.modules) {
            resetModule(var4);
            if (var2.has(var4.getName())) {
               var4.fromJson(var2.get(var4.getName()));
            }
         }

         for (ModuleManager.b var6 : this.pending) {
            var6.b = var2.has(var6.a.b) ? var2.get(var6.a.b) : null;
         }
      }
   }

   private static void resetModule(Module var0) {
      if (var0.isEnabled()) {
         var0.disable();
      }

      for (Setting var2 : var0.getSettings()) {
         resetSetting(var2);
      }
   }

   private static void resetSetting(Setting var0) {
      var0.setValueNoEvent(var0.getDefaultValue());
   }

   public static final class a {
      public final String a;
      public final String b;
      public final Module.Category c;
      public final String d;

      public a(String var1, String var2, Module.Category var3, String var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
      }
   }

   private static final class b {
      private final ModuleManager.a a;
      private JsonElement b;
      private CatalogModule c;

      private b(ModuleManager.a var1) {
         this.a = var1;
      }
   }
}
