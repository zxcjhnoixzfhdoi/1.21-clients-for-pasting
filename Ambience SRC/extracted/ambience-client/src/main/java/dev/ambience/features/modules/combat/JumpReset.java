package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.LungeSwap;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.JumpResetHook;
import dev.ambience.inject.Access;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class JumpReset extends Module {
   private static JumpReset INSTANCE;
   private final Setting<Integer> chance;
   private final Setting<Integer> delay;
   private final Setting<Float> range;
   private int prevHurtTime;
   private int delayTimer;
   private int jumpsQueued;

   public JumpReset() {
      super("JumpReset", "Jumps the tick you take knockback so you stay in combo range.", Module.Category.COMBAT);
      this.chance = this.num("Chance", 100, 0, 100).setPage("General");
      this.delay  = this.num("Delay",   0, 0,   3).setPage("General");
      this.range  = this.num("Range",  8f, 2f, 16f).setPage("General");
      INSTANCE = this;
      JumpResetHook.bind(JumpReset::doJump);
   }

   public static JumpReset get() {
      return INSTANCE;
   }

   @Override
   public String getDisplayInfo() {
      return this.chance.getValue() + "%";
   }

   @Override
   public String getDebugState() {
      if (this.jumpsQueued > 0) return "jump";
      if (this.delayTimer > 0) return "wait=" + this.delayTimer;
      return null;
   }

   @Override
   public void onDisable() {
      this.prevHurtTime = 0;
      this.delayTimer   = 0;
      this.jumpsQueued  = 0;
   }

   @Override
   public void onPreTick() {
      if (!nullCheck()) {
         this.prevHurtTime = 0;
         this.delayTimer   = 0;
         this.jumpsQueued  = 0;
         return;
      }
      ClientPlayerEntity player = mc.player;
      int hurtTime = player.hurtTime;
      boolean risingEdge = hurtTime > this.prevHurtTime;
      this.prevHurtTime = hurtTime;

      if (this.delayTimer > 0) {
         this.delayTimer--;
         if (this.delayTimer == 0 && canJump()) {
            this.jumpsQueued = 1;
         }
      }

      if (risingEdge && canJump()) {
         int chanceVal = this.chance.getValue();
         if (chanceVal < 100 && ThreadLocalRandom.current().nextInt(100) >= chanceVal) {
            return;
         }
         int d = Math.max(0, this.delay.getValue());
         if (d == 0) {
            this.jumpsQueued = 1;
         } else {
            this.delayTimer = d;
         }
      }
   }

   // called by JumpResetHook each tick — executes one queued jump
   public static void doJump() {
      JumpReset inst = get();
      if (inst == null || !inst.isEnabled() || inst.jumpsQueued <= 0) return;
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || player.input == null) return;
      if (AutoTotem.c(0) || Freecam.a(0)) return;
      player.input.jump();
      Access.setJumping(player, false);
      inst.jumpsQueued--;
   }

   private boolean canJump() {
      if (mc.currentScreen != null) return false;
      ClientPlayerEntity player = mc.player;
      if (!player.isOnGround() || player.isSpectator() || !player.isAlive()) return false;
      if (player.getAbilities().flying || player.isGliding() || player.isTouchingWater() || player.isClimbing()) return false;
      if (Freecam.a(0) || AutoTotem.c(0) || AnchorMacro.a() || AutoMace.a(0)) return false;
      if (WindHop.a(0) || PearlCatch.a(0)) return false;
      if (LungeSwap.a(0)) return false;
      return checkRange();
   }

   private boolean checkRange() {
      if (mc.world == null) return true;
      float r = this.range.getValue();
      for (var entity : mc.world.getPlayers()) {
         if (entity == mc.player) continue;
         if (entity.distanceTo(mc.player) <= r) return true;
      }
      return false;
   }
}
