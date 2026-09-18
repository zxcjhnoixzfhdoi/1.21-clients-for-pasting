package dev.ambience.features.modules.movement;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AnchorMacro;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;

public class LungeSwap extends Module {
   private static LungeSwap INSTANCE;
   private int savedSlot;   // slot before lunge
   private int lungeSlot;   // hotbar slot of lunge spear
   private boolean restoring;    // waiting to restore after throw
   private boolean stayingOnSpear; // in "on spear" riding mode

   public LungeSwap() {
      super("LungeSwap", "Jumps and stabs a Lunge spear in the same tick, then stays on the spear.",
            Module.Category.MOVEMENT);
      this.savedSlot = -1;
      this.lungeSlot = -1;
      INSTANCE = this;
   }

   public static LungeSwap get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled(); }

   @Override public String getDebugState() {
      return "spear=" + this.lungeSlot + " hand=" + this.savedSlot + " restore=" + this.restoring;
   }

   @Override
   public void onEnable() {
      if (!nullCheck() || mc.interactionManager == null) { this.disable(); return; }
      this.lungeSlot = findLungeSpear();
      if (this.lungeSlot == -1) { this.disable(); return; }
      this.savedSlot = mc.player.getInventory().getSelectedSlot();
      this.restoring = false;
      this.stayingOnSpear = false;
      // switch to spear
      InventoryUtil.a(mc.player, this.lungeSlot);
   }

   @Override
   public void onDisable() {
      restoreSlot();
      this.restoring = false;
      this.savedSlot = -1;
      this.lungeSlot = -1;
   }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) { this.disable(); return; }
      if (mc.currentScreen != null || AutoTotem.c(0) || AnchorMacro.a()) { restoreSlot(); this.disable(); return; }

      // re-find spear each tick in case inventory shifted
      this.lungeSlot = findLungeSpear();
      if (this.lungeSlot == -1) { this.disable(); return; }

      int curSlot = mc.player.getInventory().getSelectedSlot();

      if (this.restoring) {
         // make sure we're on the spear slot
         if (curSlot != this.lungeSlot) InventoryUtil.a(mc.player, this.lungeSlot);
         if (this.getBindMode() == BindMode.HOLD && !this.getBind().isDown()) {
            this.restoring = false;
            restoreSlot();
            this.disable(); return;
         }
         if (curSlot == this.savedSlot) {
            InventoryUtil.a(mc.player, this.lungeSlot);
            return;
         }
         restoreSlot();
         this.disable(); return;
      }

      // sync to spear slot
      if (curSlot != this.lungeSlot) {
         InventoryUtil.a(mc.player, this.lungeSlot); return;
      }

      // wait for spear to be in main hand
      ItemStack held = mc.player.getMainHandStack();
      if (!isLungeSpear(held)) return;

      // wait for attack charge
      if (mc.player.isBelowMinimumAttackCharge(held, 0)) return;

      // execute: jump then stab
      if (!mc.player.isOnGround()) {
         stab();
      } else {
         mc.player.input.jump();
         // stab happens next tick via hook
      }
   }

   private void stab() {
      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.restoring = true;
   }

   private void restoreSlot() {
      if (this.savedSlot >= 0 && nullCheck() && mc.interactionManager != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
   }

   private int findLungeSpear() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (isLungeSpear(stack)) return i;
      }
      return -1;
   }

   private static boolean isLungeSpear(ItemStack stack) {
      // lunge spear = spear (trident-like) with Lunge enchantment
      if (!stack.isIn(ItemTags.SPEARS)) return false;
      var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchants == null) return false;
      var rm = MinecraftClient.getInstance().world;
      if (rm == null) return false;
      var reg = rm.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
      if (reg.isEmpty()) return false;
      net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment> lunge = null; try { lunge = reg.get().getOrThrow(Enchantments.LUNGE); } catch(Exception _ex) {}
      return lunge != null && enchants.getLevel(lunge) > 0;
   }
}
