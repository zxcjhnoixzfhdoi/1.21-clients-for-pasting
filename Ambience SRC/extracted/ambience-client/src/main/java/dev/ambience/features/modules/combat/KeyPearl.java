package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Bind;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.joml.Vector2f;

public class KeyPearl extends Module {
   private static KeyPearl INSTANCE;
   private final Setting<Bind>    throwKey;
   private final Setting<Boolean> restoreSlot;
   private final Setting<Vector2f> switchMs;
   private State state;
   private int savedSlot;
   private int pearlSlot;
   private long stateUntil;
   private boolean keyWasDown;

   public KeyPearl() {
      super("KeyPearl", "Throws an ender pearl from your hotbar when you press the bound key.",
            Module.Category.COMBAT);
      this.throwKey   = this.key("ThrowKey",  Bind.none()).setPage("General");
      this.restoreSlot = this.bool("RestoreSlot", true).setPage("General");
      this.switchMs   = this.vec2f("SwitchMs", 45f, 110f).setVec2TrackBounds(50f, 500f).setPage("General");
      this.state      = State.IDLE;
      this.savedSlot  = -1;
      this.pearlSlot  = -1;
      INSTANCE = this;
   }

   public static KeyPearl get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.state != State.IDLE; }

   @Override public String getDisplayInfo() { return this.state != State.IDLE ? this.state.name() : null; }
   @Override public String getDebugState()  { return this.state != State.IDLE ? this.state.name() : null; }

   @Override
   public void onDisable() {
      
   }

   @Override
   public void onPreTick() {
      if (!nullCheck()) return;
      boolean keyDown = this.throwKey.getValue().isDown();

      if (this.state == State.IDLE) {
         if (keyDown && !this.keyWasDown) {
            tryBegin();
         }
         this.keyWasDown = keyDown;
         return;
      }

      this.keyWasDown = keyDown;
      if (!keyDown && this.state != State.THROWN) {
         // key released before throw — abort and restore
         restore();
         return;
      }
      tick();
   }

   @Override
   public void onTick() {
      // executed every game tick when state != IDLE
      if (this.state == State.IDLE || mc.interactionManager == null) return;
      tick();
   }

   private void tryBegin() {
      if (mc.interactionManager == null || mc.currentScreen != null) return;
      if (AutoTotem.c(0) || AnchorMacro.a()) return;
      this.pearlSlot = findPearl();
      if (this.pearlSlot == -1) return;
      int cur = mc.player.getInventory().getSelectedSlot();
      if (this.pearlSlot == cur) {
         // already holding pearl — go straight to throw phase
         this.savedSlot = cur;
         this.stateUntil = System.currentTimeMillis() + randomDelay();
         this.state = State.SWITCHED;
      } else {
         this.savedSlot = cur;
         InventoryUtil.a(mc.player, this.pearlSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
         this.stateUntil = System.currentTimeMillis() + randomDelay();
         this.state = State.SWITCHED;
      }
   }

   private void tick() {
      long now = System.currentTimeMillis();
      switch (this.state) {
         case SWITCHED -> {
            if (now < this.stateUntil) return;
            // throw
            ClientPlayerInteractionManager im = mc.interactionManager;
            im.interactItem(mc.player, Hand.MAIN_HAND);
            if (this.restoreSlot.getValue() && this.savedSlot != this.pearlSlot) {
               this.stateUntil = now + randomDelay();
               this.state = State.THROWN;
            } else {
               
            }
         }
         case THROWN -> {
            if (now < this.stateUntil) return;
            restore();
         }
         default -> {}
      }
   }

   private void restore() {
      if (this.savedSlot >= 0 && nullCheck() && mc.interactionManager != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      
   }

   private void resetState() {
      this.state    = State.IDLE;
      this.savedSlot = -1;
      this.pearlSlot = -1;
      this.stateUntil = 0;
   }

   private int findPearl() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) return i;
      }
      return -1;
   }

   private long randomDelay() {
      Vector2f v = this.switchMs.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      if (lo >= hi) return lo;
      return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }

   private enum State { IDLE, SWITCHED, THROWN }
}
