package dev.ambience.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;

public final class CrystalTracker {
   private static final int a = 20;
   private static final Set<BlockPos> b = new HashSet<>();
   private static final Set<Integer> c = new HashSet<>();
   private static final Map<BlockPos, Integer> d = new HashMap<>();
   private static final Map<BlockPos, Integer> e = new HashMap<>();

   private CrystalTracker() {
   }

   public static void a() {
      e.replaceAll((var0, var1) -> var1 - 1);
      e.entrySet().removeIf(var0 -> var0.getValue() <= 0);
   }

   public static void a(BlockPos var0) {
      BlockPos var1 = var0.toImmutable();
      b.add(var1);
      e.put(var1, 20);
   }

   public static void a(EndCrystalEntity var0) {
      BlockPos var1 = c(var0);
      if (b.contains(var1)) {
         c.add(var0.getId());
         d.put(var1, var0.getId());
         e.remove(var1);
      }
   }

   public static boolean b(EndCrystalEntity var0) {
      if (var0 == null) {
         return false;
      }

      if (c.contains(var0.getId())) {
         return true;
      }

      BlockPos var1 = c(var0);
      return b.contains(var1) || c(var1) || d.containsKey(var1);
   }

   public static boolean b(BlockPos var0) {
      return b.contains(var0.toImmutable());
   }

   public static boolean c(BlockPos var0) {
      Integer var1 = e.get(var0.toImmutable());
      return var1 != null && var1 > 0;
   }

   public static void d(BlockPos var0) {
      e(var0);
   }

   public static void e(BlockPos var0) {
      BlockPos var1 = var0.toImmutable();
      b.remove(var1);
      e.remove(var1);
      Integer var2 = d.remove(var1);
      if (var2 != null) {
         c.remove(var2);
      }
   }

   public static void b() {
      b.clear();
      c.clear();
      d.clear();
      e.clear();
   }

   public static BlockPos c(EndCrystalEntity var0) {
      return BlockPos.ofFloored(var0.getX(), var0.getY() - 1.0, var0.getZ());
   }
}
