package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.LungeSwap;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.modules.player.AutoMLG;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public class TotemMainhand extends Module {
   private static TotemMainhand INSTANCE;
   private final Setting<Integer> switchGrace;
   private int   graceTimer;   // ticks remaining before restore
   private boolean swapped;    // we switched to totem
   private int   prevSlot;     // slot before totem switch

   public TotemMainhand() {
      super("TotemMainhand",
            "Keeps a totem in your main hand unless another combat action needs it.",
            Module.Category.COMBAT);
      this.switchGrace = this.num("SwitchGrace", 15, 0, 40).setPage("General");
      this.prevSlot = -1;
      INSTANCE = this;
   }

   public static TotemMainhand get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled(); }

   @Override public String getDebugState() {
      if (!nullCheck()) return null;
      if (!InventoryUtil.b(mc.player, Items.TOTEM_OF_UNDYING)) return null;
      if (isBlockedByOtherModule()) return null;
      if (InventoryUtil.a(mc.player, Items.TOTEM_OF_UNDYING) == -1) return null;
      if (this.graceTimer > 0) return "grace=" + this.graceTimer;
      if (this.swapped) return "holding";
      return null;
   }

   @Override
   public void onDisable() {
      this.graceTimer = 0;
      this.swapped = false;
      this.prevSlot = -1;
   }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      ClientPlayerEntity player = mc.player;
      if (player.isCreative() || player.isSpectator() || !player.isAlive()) return;

      int curSlot = player.getInventory().getSelectedSlot();
      boolean hasTotem = InventoryUtil.b(player, Items.TOTEM_OF_UNDYING);

      if (mc.currentScreen != null) {
         // user has screen open — if we swapped, start grace then stop interfering
         if (this.swapped) {
            this.swapped = false;
            this.prevSlot = curSlot;
            this.graceTimer = Math.max(0, this.switchGrace.getValue());
         }
         return;
      }

      if (!hasTotem) { this.swapped = false; this.prevSlot = -1; this.graceTimer = 0; return; }
      if (isBlockedByOtherModule()) { return; }
      if (RotationManager.d()) return;

      int totemSlot = InventoryUtil.a(player, Items.TOTEM_OF_UNDYING);
      if (totemSlot == -1) { this.swapped = false; this.graceTimer = 0; return; }

      boolean mainHasTotem = isTotem(player.getMainHandStack());

      if (!this.swapped) {
         // detect player switched away from our totem slot
         if (this.prevSlot >= 0 && curSlot != this.prevSlot) {
            this.graceTimer = Math.max(this.graceTimer, this.switchGrace.getValue());
         }
      }

      if (this.graceTimer > 0) {
         this.graceTimer--;
         if (this.graceTimer == 0 && !mainHasTotem) {
            // restore to totem after grace
            InventoryUtil.a(player, totemSlot);
            this.swapped = true;
            this.prevSlot = totemSlot;
         }
         return;
      }

      if (!mainHasTotem) {
         // switch to totem
         InventoryUtil.a(player, totemSlot);
         this.swapped = true;
         this.prevSlot = totemSlot;
      } else {
         this.prevSlot = curSlot;
      }
   }

   private boolean isBlockedByOtherModule() {
      return AutoTotem.c(0) || AnchorMacro.a() || AutoMace.a(0)
          || WindHop.a(0) || PearlCatch.a(0) || LungeSwap.a(0)
          || Freecam.a(0) || AutoMLG.a(0);
   }

   private static boolean isTotem(ItemStack stack) {
      return stack.isOf(Items.TOTEM_OF_UNDYING);
   }
}
