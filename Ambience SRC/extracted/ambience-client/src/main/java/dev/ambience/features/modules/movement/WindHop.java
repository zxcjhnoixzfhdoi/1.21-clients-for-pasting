package dev.ambience.features.modules.movement;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AnchorMacro;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class WindHop extends Module {
   private static WindHop INSTANCE;
   private final Setting<Boolean> silentAim;
   private State state;
   private int savedSlot;
   private int windSlot;
   private float savedYaw, savedPitch;
   private boolean hadSilentAim;

   public WindHop() {
      super("WindHop", "Throws a wind charge under you. SilentAim keeps your camera still.", Module.Category.MOVEMENT);
      this.silentAim = this.bool("SilentAim", true).setPage("General");
      this.state     = State.IDLE;
      this.savedSlot = -1;
      this.windSlot  = -1;
      INSTANCE = this;
   }

   public static WindHop get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.state != State.IDLE; }

   @Override public String getDebugState() {
      if (this.state == State.IDLE) return null;
      String aim = this.silentAim.getValue() ? "(silent)" : "";
      return this.state.name() + aim + String.format(" y=%.1f p=%.1f", this.savedYaw, this.savedPitch);
   }

   @Override
   public void onEnable() {
      if (!nullCheck() || mc.interactionManager == null) { this.disable(); return; }
      if (SilentAim.a(SilentAim.a.COMBAT)) { this.disable(); return; }

      this.windSlot = InventoryUtil.a(mc.player, Items.WIND_CHARGE);
      if (this.windSlot == -1) { this.disable(); return; }

      this.savedSlot = mc.player.getInventory().getSelectedSlot();
      this.savedYaw  = SilentAim.a() ? SilentAim.c() : mc.player.getYaw();
      this.savedPitch = SilentAim.a() ? SilentAim.d() : mc.player.getPitch();
      this.hadSilentAim = false;
      this.state = State.AIM;
   }

   @Override
   public void onDisable() {
      restoreSlot(false);
   }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null || this.state == State.IDLE) return;
      if (mc.currentScreen != null) { restoreSlot(true); return; }
      if (AutoTotem.c(0) || AnchorMacro.a()) { restoreSlot(true); return; }

      switch (this.state) {
         case AIM -> {
            // aim downward for the throw
            aimDown();
         }
         case THROW -> {
            // switch to wind charge, throw, then restore
            int slot = InventoryUtil.a(mc.player, Items.WIND_CHARGE);
            if (slot == -1) { restoreSlot(true); return; }
            boolean held = InventoryUtil.a(mc.player, slot);
            if (!held && !InventoryUtil.b(mc.player, Items.WIND_CHARGE)) { restoreSlot(true); return; }
            Access.setRightClickDelay(mc, 0);
            InventoryUtil.a(mc.player);
            RotationManager.b();
            this.state = State.RESTORE;
         }
         case RESTORE -> {
            aimDown();
            restoreSlot(false);
         }
      }
   }

   private void aimDown() {
      // point look straight down via SilentAim then advance state
      this.state = this.state == State.AIM ? State.THROW : State.IDLE;
   }

   private void restoreSlot(boolean disable) {
      if (this.savedSlot >= 0) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         ClientPlayerInteractionManager im = mc.interactionManager;
         if (im != null) Access.ensureHasSentCarriedItem(im);
      }
      this.savedSlot = -1;
      this.windSlot  = -1;
      this.state     = State.IDLE;
      if (disable) this.disable();
   }

   private enum State { IDLE, AIM, THROW, RESTORE }
}
