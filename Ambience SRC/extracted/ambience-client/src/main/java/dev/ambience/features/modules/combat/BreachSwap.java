package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.BreachSwapHook;
import dev.ambience.util.InventoryUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

public class BreachSwap extends Module {
   private static BreachSwap INSTANCE;
   private int savedSlot;

   public BreachSwap() {
      super("BreachSwap",
            "On a jump crit, attribute-swaps the hit onto a Breach mace then back to the sword.",
            Module.Category.COMBAT);
      this.savedSlot = -1;
      INSTANCE = this;

      BreachSwapHook.bind(new BreachSwapHook.Impl() {
         @Override public void beforeAttack(PlayerEntity attacker, Entity target) { BreachSwap.onBefore(attacker, target); }
         @Override public void afterAttack()                                       { BreachSwap.onAfter(); }
      });
   }

   public static BreachSwap get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.savedSlot != -1; }

   public static void onBefore(PlayerEntity attacker, Entity target) {
      if (INSTANCE == null || !INSTANCE.isEnabled()) return;
      INSTANCE.trySwap(attacker, target);
   }

   public static void onAfter() {
      if (INSTANCE != null) INSTANCE.restore();
   }

   @Override public void onDisable() { restore(); }

   private void trySwap(PlayerEntity attacker, Entity target) {
      if (this.savedSlot != -1) return;  // already swapped
      if (!nullCheck() || mc.interactionManager == null) return;
      if (attacker != mc.player || !(target instanceof LivingEntity)) return;
      // jump crit only
      if (!mc.player.isGliding() && !mc.player.isOnGround()) return;

      int maceSlot = findBreachMace();
      if (maceSlot == -1) return;
      int cur = mc.player.getInventory().getSelectedSlot();
      if (maceSlot == cur) return;

      this.savedSlot = cur;
      InventoryUtil.a(mc.player, maceSlot);
   }

   private void restore() {
      if (this.savedSlot < 0) return;
      if (nullCheck() && mc.interactionManager != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         dev.ambience.inject.Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      this.savedSlot = -1;
   }

   private int findBreachMace() {
      if (mc.world == null) return -1;
      var reg = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
      if (reg.isEmpty()) return -1;
      RegistryEntry<net.minecraft.enchantment.Enchantment> breach = null; try { breach = reg.get().getOrThrow(net.minecraft.registry.RegistryKey.of(RegistryKeys.ENCHANTMENT, net.minecraft.util.Identifier.of("breach"))); } catch (Exception _bre) {}
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isOf(Items.MACE)) continue;
         var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
         if (enchants != null && breach != null && enchants.getLevel(breach) > 0) return i;
      }
      return -1;
   }
}
