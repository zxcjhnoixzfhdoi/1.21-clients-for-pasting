package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.LungeSwap;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.RotationManager;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class TBot extends Module {
   private final Setting<Vector2f> delay;
   private final Setting<Boolean>  tpsSync;
   private final Setting<Boolean>  stickyTarget;
   private final Setting<Boolean>  pauseInScreen;
   private final Setting<Float>    range;
   private final Setting<Boolean>  players;
   private final Setting<Boolean>  hostiles;
   private final Setting<Boolean>  passives;
   private final Setting<Boolean>  crystals;
   private final Setting<Boolean>  swordCooldown;
   private final Setting<Float>    perfectHit;
   private final Setting<CritMode> critPriority;
   private final Setting<Integer>  critWaitTicks;
   private final Stopwatch timer;
   private Entity   lastTarget;
   private int      critWait;
   private Boolean  wasCrit;
   private long     delayMs;
   private double   serverTps = 50.0;

   public TBot() {
      super("TBot", "Attacks the entity under your crosshair when in range.", Module.Category.COMBAT);
      this.delay        = this.vec2f("Delay",     0f, 0f).setVec2TrackBounds(0f, 250f).setPage("General");
      this.tpsSync      = this.bool("TpsSync",    true).setPage("General");
      this.stickyTarget = this.bool("StickyTarget", false).setPage("General");
      this.pauseInScreen = this.bool("PauseInScreen", true).setPage("General");
      this.range        = this.num("Range",       3.0f, 2.5f, 3.0f).setPage("General");
      this.players      = this.bool("Players",    true).setPage("Targets");
      this.hostiles     = this.bool("Hostiles",   true).setPage("Targets");
      this.passives     = this.bool("Passives",  false).setPage("Targets");
      this.crystals     = this.bool("Crystals",   true).setPage("Targets");
      this.swordCooldown = this.bool("SwordCooldown", true).setPage("Sword");
      this.perfectHit   = this.num("PerfectHit", 100f, 0f, 100f)
                             .setVisibility(v -> this.swordCooldown.getValue()).setPage("Sword");
      this.critPriority = this.mode("CritPriority", CritMode.OFF).setPage("Sword");
      this.critWaitTicks = this.num("CritWaitTicks", 4, 0, 10)
                              .setVisibility(v -> this.critPriority.getValue() != CritMode.OFF).setPage("Sword");
      this.timer = new Stopwatch();
      this.delayMs = 0;
   }

   @Override public String getDisplayInfo() {
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      String base = lo == hi ? lo + "ms" : String.format("%.0f-%.0fms", v.x(), v.y());
      if (this.swordCooldown.getValue() && this.perfectHit.getValue() < 100f) {
         return base + " " + Math.round(this.perfectHit.getValue()) + "%";
      }
      return base;
   }

   @Override public void onDisable() { this.lastTarget = null; this.critWait = 0; }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0) || LungeSwap.a(0) || Freecam.a(0)) return;
      if (AutoTotem.c(0) || AnchorMacro.a()) return;
      if (this.pauseInScreen.getValue() && mc.currentScreen != null) return;

      Entity target = getTarget();
      if (target == null) { this.lastTarget = null; return; }
      this.lastTarget = target;

      float r = this.range.getValue();
      if (mc.player.distanceTo(target) > r) return;

      // sword cooldown check
      if (this.swordCooldown.getValue()) {
         float pct = this.perfectHit.getValue() / 100f;
         if (mc.player.getAttackCooldownProgress(0f) < pct) return;
      }

      // crit priority
      if (this.critPriority.getValue() != CritMode.OFF) {
         boolean onGround = mc.player.isOnGround();
         boolean needCrit = this.critPriority.getValue() == CritMode.Force;
         if (needCrit && onGround) {
            this.critWait++;
            if (this.critWait < this.critWaitTicks.getValue()) return;
         }
         this.critWait = 0;
      }

      // timer gate
      long delay = randomDelay();
      if (!this.timer.a(delay)) return;

      // attack
      mc.interactionManager.attackEntity(mc.player, target);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.timer.a();
   }

   private Entity getTarget() {
      HitResult hit = mc.crosshairTarget;
      if (!(hit instanceof EntityHitResult ehr)) return null;
      Entity e = ehr.getEntity();
      return isValidTarget(e) ? e : null;
   }

   private boolean isValidTarget(Entity e) {
      if (e == null || e == mc.player || !(e instanceof LivingEntity le)) return false;
      if (!le.isAlive() || le.isSpectator() || e instanceof ArmorStandEntity) return false;
      if (e instanceof EndCrystalEntity) return this.crystals.getValue();
      if (e instanceof PlayerEntity)     return this.players.getValue();
      if (e instanceof Monster)          return this.hostiles.getValue();
      return this.passives.getValue();
   }

   private long randomDelay() {
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      if (lo >= hi) return lo;
      return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }

   public enum CritMode { OFF, Prefer, Force }
}
