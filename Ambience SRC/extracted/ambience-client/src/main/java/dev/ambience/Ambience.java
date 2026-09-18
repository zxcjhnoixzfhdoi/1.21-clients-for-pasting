package dev.ambience;

import dev.ambience.hooks.GuiRuntime;
import dev.ambience.hooks.HookClear;
import dev.ambience.inject.InjectBootstrap;
import dev.ambience.loader.PayloadLoader;
import dev.ambience.loader.auth.PayloadGuard;
import dev.ambience.manager.ColorManager;
import dev.ambience.manager.CommandManager;
import dev.ambience.manager.ConfigManager;
import dev.ambience.manager.EventManager;
import dev.ambience.manager.ModuleManager;
import dev.ambience.util.SilentAim;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ambience implements ClientModInitializer, ModInitializer {
   public static final Logger a = LogManager.getLogger("Ambience");
   public static ColorManager b;
   public static EventManager c;
   public static CommandManager d;
   public static ModuleManager e;
   public static ConfigManager f;
   private static final AtomicBoolean g = new AtomicBoolean(true);

   public static boolean isInMemory() {
      return g.get();
   }

   public static void a(String var0) {
      a.warn("Authorization revoked ({}) — terminating game.", var0);
      PayloadGuard.revoke();
      Runtime.getRuntime().halt(0);
   }

   public static void unloadFromMemory(String var0) {
      if (g.compareAndSet(true, false)) {
         a.info("Unloading {} from memory ({})", "Ambience", var0 == null ? "manual" : var0);

         try {
            MinecraftClient var1 = MinecraftClient.getInstance();
            if (var1 != null && var1.currentScreen != null && GuiRuntime.isClickGui(var1.currentScreen)) {
               var1.setScreen(null);
            }
         } catch (Throwable var7) {
         }

         try {
            if (f != null) {
               f.b();
            }
         } catch (Throwable var6) {
            a.warn("Config save during unload failed: {}", var6.toString());
         }

         try {
            if (e != null) {
               e.evictPayloadFromMemory();
            }
         } catch (Throwable var5) {
            a.warn("Module evict failed: {}", var5.toString());
         }

         try {
            PayloadLoader.unloadFromMemory();
         } catch (Throwable var4) {
            a.warn("Payload unload failed: {}", var4.toString());
         }

         try {
            HookClear.clearAll();
         } catch (Throwable var3) {
            a.warn("Hook clear failed: {}", var3.toString());
         }

         try {
            SilentAim.g();
         } catch (Throwable var2) {
         }

         boolean var8 = a();
         a.info("{} features dropped from memory{} (disable Unload to reload).", "Ambience", var8 ? " and inject mixins stripped" : "");
      }
   }

   public static void reloadIntoMemory() {
      if (g.compareAndSet(false, true)) {
         a.info("Loading {} back into memory", "Ambience");

         try {
            b();
            PayloadLoader.ensureLoaded();
            if (f != null) {
               f.a();
            }

            if (e != null) {
               e.onLoad();
            }
         } catch (Throwable var1) {
            g.set(false);
            a.error("Failed to reload {} into memory", "Ambience", var1);
         }
      }
   }

   private static boolean a() {
      try {
         Class var0 = Class.forName("com.mixininject.api.AgentHooks");
         return var0.getMethod("unload").invoke(null) instanceof Boolean var2 && var2;
      } catch (ClassNotFoundException var3) {
         return false;
      } catch (Throwable var4) {
         a.warn("AgentHooks.unload failed: {}", var4.toString());
         return false;
      }
   }

   private static void b() {
      try {
         Class var0 = Class.forName("com.mixininject.api.AgentHooks");
         var0.getMethod("reapply").invoke(null);
      } catch (ClassNotFoundException var1) {
      } catch (Throwable var2) {
         a.warn("AgentHooks.reapply failed: {}", var2.toString());
      }
   }

   public void onInitialize() {
      InjectBootstrap.start();
   }

   public void onInitializeClient() {
      InjectBootstrap.start();
   }
}
