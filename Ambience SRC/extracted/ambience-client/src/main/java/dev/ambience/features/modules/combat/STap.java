package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.STapHook;
import dev.ambience.inject.Access;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class STap extends Module {
   private static STap INSTANCE;
   private final Setting<Vector2f> backMs;
   private final Setting<Boolean>  strafe;
   private final Setting<Boolean>  weaponsOnly;
   private final Setting<Double>   approachSpeed;
   private final Setting<Boolean>  players;
   private final Setting<Boolean>  hostiles;
   private final Setting<Boolean>  passives;
   private boolean attacking;
   private long    backUntil;
   private Entity  target;
   private double  prevDist;
   private boolean approaching;

   public STap() {
      super("STap", "Walks back after a hit when the target approaches so they can't return the hit.",
            Module.Category.COMBAT);
      this.backMs       = this.vec2f("BackMs",       280f, 420f).setVec2TrackBounds(0f, 4000f).setPage("General");
      this.strafe       = this.bool("Strafe",        true).setPage("General");
      this.weaponsOnly  = this.bool("WeaponsOnly",   true).setPage("General");
      this.approachSpeed = this.num("ApproachSpeed", 0.08, 0.0, 0.5).setPage("General");
      this.players      = this.bool("Players",       true).setPage("Targets");
      this.hostiles     = this.bool("Hostiles",     false).setPage("Targets");
      this.passives     = this.bool("Passives",     false).setPage("Targets");
      this.prevDist = -1.0;
      INSTANCE = this;

      STapHook.bind(new STapHook.Impl() {
         @Override public void beforeAttack(PlayerEntity attacker, Entity target) { STap.onBefore(attacker, target); }
         @Override public void afterAttack()  { STap.onAfter(); }
         @Override public void freezeInput()  { STap.onFreeze(); }
      });
   }

   public static STap get() { return INSTANCE; }

   // active = timer running AND target approaching
   public static boolean a(int unused) {
      return INSTANCE != null && INSTANCE.isEnabled()
          && INSTANCE.backUntil > System.currentTimeMillis()
          && INSTANCE.approaching;
   }

   @Override public void onDisable() { this.attacking = false; this.backUntil = 0L; this.target = null; }

   public static void onBefore(PlayerEntity attacker, Entity target) {
      if (INSTANCE == null) return;
      INSTANCE.checkBefore(attacker, target);
   }

   public static void onAfter() {
      if (INSTANCE == null) return;
      INSTANCE.armTimer();
   }

   public static void onFreeze() {
      if (!a(0) || AutoTotem.c(0) || Freecam.a(0)) return;
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || player.input == null) return;
      // walk backward
      Access.setMoveVector(player.input, new Vec2f(0, -1));
   }

   @Override
   public void onPreTick() {
      // track approach
      if (this.target instanceof LivingEntity le) {
         double d = mc.player.distanceTo(this.target);
         double delta = this.prevDist >= 0 ? this.prevDist - d : 0;
         this.approaching = delta > this.approachSpeed.getValue();
         this.prevDist = d;
      } else {
         this.approaching = false;
         this.prevDist = -1;
      }
      if (!a(0)) { this.backUntil = 0L; }
   }

   private void checkBefore(PlayerEntity attacker, Entity target) {
      this.attacking = false;
      if (!this.isEnabled()) return;
      if (attacker != mc.player || !nullCheck()) return;
      if (Freecam.a(0) || AutoTotem.c(0) || AnchorMacro.a()) return;
      if (this.weaponsOnly.getValue() && !isWeapon(attacker)) return;
      if (!isValidTarget(target)) return;
      this.target = target;
      this.attacking = true;
   }

   private void armTimer() {
      if (!this.attacking) return;
      this.attacking = false;
      Vector2f v = this.backMs.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      long delay = lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
      this.backUntil = System.currentTimeMillis() + delay;
   }

   private boolean isWeapon(PlayerEntity p) {
      var stack = p.getMainHandStack();
      return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
   }

   private boolean isValidTarget(Entity e) {
      if (!(e instanceof LivingEntity) || e instanceof ArmorStandEntity) return false;
      if (e instanceof PlayerEntity) return this.players.getValue();
      if (e instanceof Monster)      return this.hostiles.getValue();
      return this.passives.getValue();
   }
}
