package dev.ambience.util;

import dev.ambience.util.traits.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class BlockUtil implements Util {
   public static final int a = 6;

   private BlockUtil() {
   }

   public static Block a() {
      Block var0 = b();
      return var0 != null ? var0 : c();
   }

   public static Block b() {
      if (mc.world != null && mc.crosshairTarget != null) {
         if (mc.crosshairTarget instanceof BlockHitResult var1 && var1.getType() == Type.BLOCK) {
            BlockState var2 = mc.world.getBlockState(var1.getBlockPos());
            return var2.isAir() ? null : var2.getBlock();
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Block c() {
      if (mc.player == null) {
         return null;
      }

      for (Hand var3 : Hand.values()) {
         Block var4 = Block.getBlockFromItem(mc.player.getStackInHand(var3).getItem());
         if (!var4.getDefaultState().isAir()) {
            return var4;
         }
      }

      return null;
   }

   public static Set<Block> a(String var0) {
      LinkedHashSet var1 = new LinkedHashSet();
      if (var0 != null && !var0.isBlank()) {
         for (String var5 : var0.split("[,;\\n]")) {
            Block var6 = b(var5);
            if (var6 != null) {
               var1.add(var6);
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   public static String a(Collection<Block> var0) {
      StringBuilder var1 = new StringBuilder();

      for (Block var3 : var0) {
         if (var1.length() > 0) {
            var1.append(", ");
         }

         var1.append(b(var3));
      }

      return var1.toString();
   }

   public static Block b(String var0) {
      if (var0 == null) {
         return null;
      }

      String var1 = var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (var1.isEmpty()) {
         return null;
      }

      Identifier var2 = Identifier.tryParse(var1);
      if (var2 == null) {
         return null;
      }

      Block var3 = (Block)Registries.BLOCK.getOptionalValue(var2).orElse(null);
      return var3 != null && !var3.getDefaultState().isAir() ? var3 : null;
   }

   public static List<Block> a(String var0, int var1) {
      ArrayList var2 = new ArrayList();
      if (var0 != null && !var0.isBlank() && var1 > 0) {
         String var3 = var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
         String var4 = var0.trim().toLowerCase(Locale.ROOT);
         ArrayList<Block> var5 = new ArrayList<>();
         ArrayList<Block> var6 = new ArrayList<>();

         for (Block var8 : Registries.BLOCK) {
            if (!var8.getDefaultState().isAir()) {
               String var9 = b(var8);
               String var10 = a(var8).toLowerCase(Locale.ROOT);
               if (var9.equals(var3) || var10.equals(var4)) {
                  var2.add(var8);
                  if (var2.size() >= var1) {
                     return var2;
                  }
               } else if (var9.startsWith(var3) || var10.startsWith(var4)) {
                  var5.add(var8);
               } else if (var9.contains(var3) || var10.contains(var4)) {
                  var6.add(var8);
               }
            }
         }

         for (Block var13 : var5) {
            if (var2.size() >= var1) {
               break;
            }

            var2.add(var13);
         }

         for (Block var14 : var6) {
            if (var2.size() >= var1) {
               break;
            }

            var2.add(var14);
         }

         return var2;
      } else {
         return var2;
      }
   }

   public static String a(Block var0) {
      return var0.getName().getString();
   }

   public static String b(Block var0) {
      Identifier var1 = Registries.BLOCK.getId(var0);
      return "minecraft".equals(var1.getNamespace()) ? var1.getPath() : var1.toString();
   }

   public static ItemStack c(Block var0) {
      ItemStack var1 = new ItemStack(var0.asItem());
      return var1.isEmpty() ? ItemStack.EMPTY : var1;
   }

   public static int d() {
      return 6;
   }
}
