package dev.ambience.inject;

import dev.ambience.Ambience;
import dev.ambience.loader.PayloadLoader;
import dev.ambience.manager.ColorManager;
import dev.ambience.manager.CommandManager;
import dev.ambience.manager.ConfigManager;
import dev.ambience.manager.EventManager;
import dev.ambience.manager.ModuleManager;
import dev.ambience.util.LowFirePack;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InjectBootstrap {
   private static final AtomicBoolean STARTED = new AtomicBoolean(false);

   private InjectBootstrap() {
   }

   public static void start() {
      if (STARTED.compareAndSet(false, true)) {
         Ambience.a.info("Starting {} via InjectBootstrap", "Ambience");
         long var0 = System.nanoTime();

         try {
            Ambience.b = new ColorManager();
            Ambience.d = new CommandManager();
            Ambience.c = new EventManager();
            Ambience.e = new ModuleManager();
            Ambience.f = new ConfigManager();

            try {
               LowFirePack.a();
            } catch (Throwable var6) {
               Ambience.a.warn("LowFirePack init skipped: {}", var6.toString());
            }

            Ambience.c.init();
            Ambience.e.init();
            PayloadLoader.ensureLoaded();
            Ambience.f.a();
            Ambience.e.onLoad();

            try {
               Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                  if (Ambience.f != null) {
                     Ambience.f.b();
                  }
               }));
            } catch (Throwable var5) {
               Ambience.a.warn("Shutdown hook skipped: {}", var5.toString());
            }

            int var2 = Ambience.e != null ? Ambience.e.getModules().size() : 0;
            long var8 = (System.nanoTime() - var0) / 1000000L;
            Ambience.a.info("Initialized {} in {}ms with {} modules (inject bootstrap). Right Shift opens ClickGui.", "Ambience", var8, var2);
            System.out.println("[MixinInject] Ambience ready — " + var2 + " modules. Press Right Shift for ClickGui.");
         } catch (Throwable var7) {
            STARTED.set(false);
            Ambience.a.error("InjectBootstrap failed", var7);
            System.out.println("[MixinInject] Ambience bootstrap FAILED: " + var7);
            throw var7 instanceof RuntimeException var3 ? var3 : new RuntimeException(var7);
         }
      }
   }

   public static boolean isStarted() {
      return STARTED.get();
   }
}
