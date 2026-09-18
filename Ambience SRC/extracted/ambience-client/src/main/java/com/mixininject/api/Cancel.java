package com.mixininject.api;

public final class Cancel {
   private static final Object VOID = new Object();
   private static final Object NULL_VALUE = new Object();
   private static final ThreadLocal<Object> PENDING = new ThreadLocal<>();

   private Cancel() {
   }

   public static void cancel() {
      PENDING.set(VOID);
   }

   public static void cancel(Object var0) {
      PENDING.set(var0 == null ? NULL_VALUE : var0);
   }

   public static boolean has() {
      return PENDING.get() != null;
   }

   public static Object consume() {
      Object var0 = PENDING.get();
      PENDING.remove();
      return var0;
   }

   public static boolean isVoid(Object var0) {
      return var0 == VOID;
   }

   public static boolean isNullValue(Object var0) {
      return var0 == NULL_VALUE;
   }
}
