package dev.ambience.features.modules.movement;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AnchorMacro;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;

public class PearlCatch extends Module {
   private static PearlCatch INSTANCE;
   private static final float LOOK_UP_PITCH = -90.0F;
   private static final int   LOOK_UP_TICKS = 40;
   private final Setting<Boolean>   silentAim;
   private final Setting<Boolean>   doubleWind;
   private final Setting<Vector2f>  hopMs;
   private final Setting<Vector2f>  throwMs;
   private State   state;
   private int     savedSlot;
   private int     windSlot;
   private int     pearlSlot;
   private float   savedYaw, savedPitch, lookYaw;
   private boolean hadSilent;
   private final Stopwatch timer;
   private long    waitMs;

   public PearlCatch() {
      super("PearlCatch", "Hops with a wind charge, then looks up and throws pearl + wind.",
            Module.Category.MOVEMENT);
      this.silentAim  = this.bool("SilentAim", true).setPage("General");
      this.doubleWind = this.bool("Double",    true).setPage("General");
      this.hopMs      = this.vec2f("HopMs",  40f, 70f).setVec2TrackBounds(0f, 400f)
                           .setVisibility(v -> this.doubleWind.getValue()).setPage("General");
      this.throwMs    = this.vec2f("ThrowMs", 0f,  0f).setVec2TrackBounds(0f, 250f).setPage("General");
      this.state      = State.IDLE;
      this.savedSlot  = -1;
      this.timer      = new Stopwatch();
      INSTANCE = this;
   }

   public static PearlCatch get()  { return INSTANCE; }
   public static boolean a(int unused)  { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.state != State.IDLE; }

   @Override public String getDebugState() {
      if (this.state == State.IDLE) return null;
      String aim = this.silentAim.getValue() ? "(silent)" : "";
      return this.state.name() + aim + " waitMs=" + this.waitMs + String.format(" yaw=%.1f", this.lookYaw);
   }

   @Override
   public void onEnable() {
      if (!nullCheck() || mc.interactionManager == null) { this.disable(); return; }
      if (AutoTotem.c(0) || AnchorMacro.a()) { this.disable(); return; }

      this.windSlot  = InventoryUtil.a(mc.player, Items.WIND_CHARGE);
      this.pearlSlot = InventoryUtil.a(mc.player, Items.ENDER_PEARL);
      if (this.windSlot == -1 || this.pearlSlot == -1) { this.disable(); return; }

      this.savedSlot = mc.player.getInventory().getSelectedSlot();
      this.savedYaw  = mc.player.getYaw();
      this.savedPitch = mc.player.getPitch();
      this.lookYaw   = MathHelper.wrapDegrees(this.savedYaw + 180f);
      this.hadSilent = false;
      this.waitMs    = randomDelay(this.hopMs);
      this.state     = State.HOP;
   }

   @Override public void onDisable() { restoreState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null || this.state == State.IDLE) return;
      if (mc.currentScreen != null || AutoTotem.c(0)) { restoreState(); return; }

      switch (this.state) {
         case HOP -> {
            // aim straight down + back, switch to wind charge
            RotationManager.a(mc.player, this.lookYaw, LOOK_UP_PITCH, this.silentAim.getValue());
            if (!this.timer.a(this.waitMs)) return;
            InventoryUtil.a(mc.player, this.windSlot);
            Access.ensureHasSentCarriedItem(mc.interactionManager);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.waitMs = randomDelay(this.throwMs);
            this.state = State.THROW_PEARL;
            this.timer.a();
         }
         case THROW_PEARL -> {
            // aim up, wait, throw pearl
            RotationManager.a(mc.player, this.lookYaw, LOOK_UP_PITCH, this.silentAim.getValue());
            if (!this.timer.a(this.waitMs)) return;
            InventoryUtil.a(mc.player, this.pearlSlot);
            Access.ensureHasSentCarriedItem(mc.interactionManager);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (this.doubleWind.getValue()) {
               this.waitMs = randomDelay(this.hopMs);
               this.state = State.THROW_WIND2;
            } else {
               this.state = State.RESTORE;
            }
            this.timer.a();
         }
         case THROW_WIND2 -> {
            if (!this.timer.a(this.waitMs)) return;
            InventoryUtil.a(mc.player, this.windSlot);
            Access.ensureHasSentCarriedItem(mc.interactionManager);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.state = State.RESTORE;
            this.timer.a();
         }
         case RESTORE -> restoreState();
      }
   }

   private void restoreState() {
      SilentAim.e();
      if (this.savedSlot >= 0 && nullCheck() && mc.interactionManager != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      this.state     = State.IDLE;
      this.savedSlot = -1;
      this.windSlot  = -1;
      this.pearlSlot = -1;
      this.disable();
   }

   private long randomDelay(Setting<Vector2f> s) {
      Vector2f v = s.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }

   private enum State { IDLE, HOP, THROW_PEARL, THROW_WIND2, RESTORE }
}
