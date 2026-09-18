package dev.ambience.event.system;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public record Listener(Object host, int priority, Consumer<Object> consumer) {

   public static Listener of(Object var0, int var1, Method var2) {
      return new Listener(var0, var1, buildLambdaMetafactory(var0, var2));
   }

   public void invoke(Object var1) {
      this.consumer.accept(var1);
   }

   public Object getHost() {
      return this.host;
   }

   private static Consumer<Object> buildLambdaMetafactory(Object var0, Method var1) {
      if (var0.getClass().getClassLoader() != Listener.class.getClassLoader()) {
         return bindInvoke(var0, var1);
      }

      try {
         Lookup var2 = MethodHandles.privateLookupIn(var0.getClass(), MethodHandles.lookup());
         CallSite var3 = LambdaMetafactory.metafactory(
            var2,
            "accept",
            MethodType.methodType(Consumer.class, var0.getClass()),
            MethodType.methodType(void.class, Object.class),
            var2.unreflect(var1),
            MethodType.methodType(void.class, var1.getParameterTypes()[0])
         );
         MethodHandle var4 = var3.getTarget();
         return (Consumer)var4.invoke((Object)var0);
      } catch (Throwable var5) {
         return bindInvoke(var0, var1);
      }
   }

   private static Consumer<Object> bindInvoke(Object var0, Method var1) {
      var1.setAccessible(true);
      return var2 -> {
         try {
            var1.invoke(var0, var2);
         } catch (ReflectiveOperationException var4) {
            throw new RuntimeException("Failed to invoke %s".formatted(var1.getName()), var4);
         }
      };
   }
}
