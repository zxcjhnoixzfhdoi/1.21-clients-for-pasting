package dev.ambience.util;

public class Stopwatch {
   private long a = System.currentTimeMillis();

   public boolean a(long var1) {
      return System.currentTimeMillis() - this.a >= var1;
   }

   public void a() {
      this.a = System.currentTimeMillis();
   }

   public long b() {
      return System.currentTimeMillis() - this.a;
   }
}
