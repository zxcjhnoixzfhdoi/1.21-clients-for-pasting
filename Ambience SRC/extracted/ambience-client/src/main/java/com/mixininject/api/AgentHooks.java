package com.mixininject.api;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class AgentHooks {
   private static final AtomicReference<BooleanSupplier> UNLOAD = new AtomicReference<>();
   private static final AtomicReference<BooleanSupplier> REAPPLY = new AtomicReference<>();
   private static final AtomicReference<BooleanSupplier> ACTIVE = new AtomicReference<>();

   private AgentHooks() {
   }

   public static void bind(BooleanSupplier var0, BooleanSupplier var1, BooleanSupplier var2) {
      UNLOAD.set(var0);
      REAPPLY.set(var1);
      ACTIVE.set(var2);
   }

   public static boolean unload() {
      BooleanSupplier var0 = UNLOAD.get();
      return var0 != null && var0.getAsBoolean();
   }

   public static boolean reapply() {
      BooleanSupplier var0 = REAPPLY.get();
      return var0 != null && var0.getAsBoolean();
   }

   public static boolean isActive() {
      BooleanSupplier var0 = ACTIVE.get();
      return var0 != null && var0.getAsBoolean();
   }
}
