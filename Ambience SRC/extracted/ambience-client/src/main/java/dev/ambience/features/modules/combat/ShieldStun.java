package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.LungeSwap;
import dev.ambience.hooks.ShieldStunHook;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public class ShieldStun extends Module {
   private static ShieldStun INSTANCE;
   private int restoreSlot;   // hotbar slot to restore after stun
   private int invAxeSlot;    // inventory (9-35) axe slot if from inv
   private int hotbarAxeSlot; // hotbar axe slot (0-8)

   public ShieldStun() {
      super("ShieldStun", "When you hit a blocking player, swaps to an axe to stun their shield then swaps back.",
            Module.Category.COMBAT);
      this.restoreSlot  = -1;
      this.invAxeSlot   = -1;
      this.hotbarAxeSlot = -1;
      INSTANCE = this;

      ShieldStunHook.bind(new ShieldStunHook.Impl() {
         @Override public void beforeAttack(PlayerEntity attacker, Entity target) { ShieldStun.onBefore(attacker, target); }
         @Override public void afterAttack()                                       { ShieldStun.onAfter(); }
      });
   }

   public static ShieldStun get() { return INSTANCE; }

   // active = swap is queued (between beforeAttack and afterAttack)
   public static boolean a(int unused) {
      return INSTANCE != null && INSTANCE.isEnabled()
          && (INSTANCE.restoreSlot != -1 || INSTANCE.invAxeSlot != -1);
   }

   @Override public String getDebugState() {
      if (this.restoreSlot == -1 && this.invAxeSlot == -1) return null;
      return "restore=" + this.restoreSlot + " inv=" + this.invAxeSlot + " hotbar=" + this.hotbarAxeSlot;
   }

   @Override public void onDisable() { restoreAll(); }

   public static void onBefore(PlayerEntity attacker, Entity target) {
      if (INSTANCE == null || !INSTANCE.isEnabled()) return;
      if (INSTANCE.restoreSlot != -1 || INSTANCE.invAxeSlot != -1) return;   // already swapped
      INSTANCE.trySwapToAxe(attacker, target);
   }

   public static void onAfter() {
      if (INSTANCE != null) INSTANCE.restoreAll();
   }

   private void trySwapToAxe(PlayerEntity attacker, Entity target) {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (attacker != mc.player) return;
      if (!(target instanceof PlayerEntity) || target == attacker) return;
      if (!(target instanceof LivingEntity living) || !living.isBlocking()) return;
      if (AutoTotem.c(0) || AnchorMacro.a() || AutoMace.a(0) || LungeSwap.a(0)) return;
      if (mc.currentScreen != null || AutoTotem.d(0)) return;
      if (isAxe(attacker.getMainHandStack())) return;  // already holding axe

      PlayerInventory inv = attacker.getInventory();
      int curSlot = inv.getSelectedSlot();

      // find axe in hotbar
      int axeSlot = findAxeInHotbar(attacker);
      if (axeSlot == -1) return;  // no axe reachable
      if (axeSlot == curSlot) return;

      this.restoreSlot  = curSlot;
      this.hotbarAxeSlot = axeSlot;
      InventoryUtil.a(mc.player, axeSlot);
   }

   private void restoreAll() {
      if (this.restoreSlot >= 0 && nullCheck() && mc.interactionManager != null) {
         mc.player.getInventory().setSelectedSlot(this.restoreSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      this.restoreSlot   = -1;
      this.invAxeSlot    = -1;
      this.hotbarAxeSlot = -1;
   }

   private static boolean isAxe(ItemStack stack) {
      return stack.isIn(ItemTags.AXES);
   }

   private static int findAxeInHotbar(PlayerEntity player) {
      PlayerInventory inv = player.getInventory();
      for (int i = 0; i < 9; i++) {
         if (isAxe(inv.getStack(i))) return i;
      }
      return -1;
   }
}
