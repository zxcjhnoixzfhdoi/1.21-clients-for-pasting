package dev.ambience.util;

import dev.ambience.inject.Access;
import dev.ambience.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

public final class InventoryUtil implements Util {
   private InventoryUtil() {
   }

   public static int a(ClientPlayerEntity var0, Item var1) {
      int var2 = var0.getInventory().getSelectedSlot();
      if (var0.getInventory().getStack(var2).isOf(var1)) {
         return var2;
      }

      for (int var3 = 0; var3 < 9; var3++) {
         if (var0.getInventory().getStack(var3).isOf(var1)) {
            return var3;
         }
      }

      return -1;
   }

   public static boolean a(ClientPlayerEntity var0, int var1) {
      if (var1 >= 0 && var1 <= 8 && mc.interactionManager != null) {
         if (var0.getInventory().getSelectedSlot() != var1) {
            int var2 = var0.getInventory().getSelectedSlot();
            var0.getInventory().setSelectedSlot(var1);
            DebugLogger.a(var2, var1, var0.getInventory().getStack(var1).getItem().toString());
         }

         Access.ensureHasSentCarriedItem(mc.interactionManager);
         return var0.getInventory().getSelectedSlot() == var1;
      } else {
         return false;
      }
   }

   public static boolean b(ClientPlayerEntity var0, Item var1) {
      return var0.getMainHandStack().isOf(var1);
   }

   public static boolean c(ClientPlayerEntity var0, Item var1) {
      int var2 = a(var0, var1);
      return var2 == -1 ? true : var0.getItemCooldownManager().isCoolingDown(var0.getInventory().getStack(var2));
   }

   public static boolean a(ClientPlayerEntity var0) {
      if (mc.interactionManager == null) {
         return false;
      }

      RotationManager.b();
      mc.interactionManager.interactItem(var0, Hand.MAIN_HAND);
      var0.swingHand(Hand.MAIN_HAND);
      return true;
   }
}
