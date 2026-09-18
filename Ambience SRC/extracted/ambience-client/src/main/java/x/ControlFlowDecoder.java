package x;

public class ControlFlowDecoder {
   public static int a(int var0) {
      return var0 != 0 ? (var0 * 31 >>> 4) % var0 ^ var0 >>> 16 : 0;
   }

   public static int b(int var0) {
      return (var0 & 7 << 29) >> 29 | var0 << 3;
   }

   public static int c(int var0) {
      return (var0 & 7 << 29) >> 29 | var0 << 3;
   }
}
