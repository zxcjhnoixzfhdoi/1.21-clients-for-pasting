package dev.ambience.hooks;

import dev.ambience.features.modules.Module;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PayloadRuntime {
   private static final Logger LOGGER = LoggerFactory.getLogger("PayloadLoader");
   private static volatile ClassLoader modules;
   private static volatile PayloadRuntime.Fetcher fetcher;

   private PayloadRuntime() {
   }

   public static void bindLoader(ClassLoader var0) {
      modules = var0;
   }

   public static void bindFetcher(PayloadRuntime.Fetcher var0) {
      fetcher = var0;
   }

   public static void clear() {
      modules = null;
      fetcher = null;
   }

   public static void fetchModule(String var0) {
      PayloadRuntime.Fetcher var1 = fetcher;
      if (var1 != null && var0 != null && !var0.isBlank()) {
         var1.fetch(var0);
      }
   }

   public static void fetchModules(List<String> var0) {
      PayloadRuntime.Fetcher var1 = fetcher;
      if (var1 != null && var0 != null && !var0.isEmpty()) {
         var1.fetchMany(var0);
      }
   }

   public static Module createModule(String var0) {
      if (var0 != null && !var0.isBlank()) {
         ClassLoader var1 = modules;
         if (var1 == null) {
            var1 = PayloadRuntime.class.getClassLoader();
         }

         try {
            Class var2 = Class.forName(var0, true, var1);
            return (Module)var2.getDeclaredConstructor().newInstance();
         } catch (Throwable var3) {
            LOGGER.error("Failed to instantiate payload class {}", var0, var3);
            return null;
         }
      } else {
         return null;
      }
   }

   public static Module createModule(String var0, String var1) {
      fetchModule(var0);
      return createModule(var1);
   }

   public interface Fetcher {
      void fetch(String var1);

      void fetchMany(List<String> var1);
   }
}
