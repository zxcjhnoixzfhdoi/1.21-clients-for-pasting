package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.modules.render.Freelook;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.AimAssistHook;
import dev.ambience.util.RotationManager;
import dev.ambience.util.RotationSmoother;
import dev.ambience.util.SilentAim;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AimAssist extends Module {
   private static AimAssist INSTANCE;
   private final Setting<Float>    speed;
   private final Setting<Float>    fov;
   private final Setting<Float>    range;
   private final Setting<Boolean>  predict;
   private final Setting<Boolean>  stickyTarget;
   private final Setting<Boolean>  throughWalls;
   private final Setting<Boolean>  horizontal;
   private final Setting<Boolean>  weaponsOnly;
   private final Setting<Boolean>  onAttack;
   private final Setting<Boolean>  players;
   private final Setting<Boolean>  hostiles;
   private final Setting<Boolean>  passives;
   private final RotationSmoother  smoother;
   private LivingEntity target;

   public AimAssist() {
      super("AimAssist", "Smooth close-range aim for swords and UHC.", Module.Category.COMBAT);
      this.speed       = this.num("Speed",       8f,  1f, 20f).setPage("General");
      this.fov         = this.num("FOV",        70f, 15f, 180f).setPage("General");
      this.range       = this.num("Range",      3.5f, 2.5f, 6f).setPage("General");
      this.predict     = this.bool("Predict",    true).setPage("General");
      this.stickyTarget = this.bool("StickyTarget", true).setPage("General");
      this.throughWalls = this.bool("ThroughWalls", false).setPage("General");
      this.horizontal  = this.bool("Horizontal", false).setPage("General");
      this.weaponsOnly = this.bool("WeaponsOnly", true).setPage("General");
      this.onAttack    = this.bool("OnAttack",   false).setPage("General");
      this.players     = this.bool("Players",    true).setPage("Targets");
      this.hostiles    = this.bool("Hostiles",  false).setPage("Targets");
      this.passives    = this.bool("Passives",  false).setPage("Targets");
      this.smoother    = new RotationSmoother();
      INSTANCE = this;
      AimAssistHook.bind(AimAssist::onRenderTick);
   }

   public static AimAssist get() { return INSTANCE; }

   // called by hook; partialTick is the render partial tick
   public static void onRenderTick(double partialTick) {
      if (INSTANCE == null || !INSTANCE.isEnabled()) return;
      INSTANCE.tick(partialTick);
   }

   @Override public String getDisplayInfo() { return this.target != null ? this.target.getName().getString() : null; }
   @Override public String getDebugState()  { return getDisplayInfo(); }
   @Override public void   onDisable()      { this.target = null; this.smoother.a(); }

   private void tick(double partialTick) {
      if (!nullCheck() || mc.currentScreen != null) { this.target = null; return; }
      if (Freecam.a(0) || AutoTotem.c(0)) return;
      if (this.weaponsOnly.getValue() && !isWeapon()) { this.target = null; return; }
      if (this.onAttack.getValue() && !mc.options.attackKey.isPressed()) { this.target = null; return; }

      LivingEntity best = findTarget();
      if (best == null) { this.target = null; return; }
      this.target = best;

      // aim toward target head/body
      Vec3d targetPos = this.predict.getValue()
          ? best.getLerpedPos((float) partialTick).add(0, best.getEyeHeight(best.getPose()) * 0.85, 0)
          : best.getEyePos();
      Vec3d eye = mc.player.getEyePos();
      Vec3d delta = targetPos.subtract(eye);

      float yaw   = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
      float pitch = this.horizontal.getValue() ? mc.player.getPitch()
                    : (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x*delta.x+delta.z*delta.z)));

      // aim applied directly via mc.player
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
   }

   private LivingEntity findTarget() {
      if (mc.world == null) return null;
      float r = this.range.getValue();
      float fovHalf = this.fov.getValue() / 2f;
      Vec3d eye = mc.player.getEyePos();
      LivingEntity best = null; double bestAngle = fovHalf;

      // prefer sticky target if still valid
      if (this.stickyTarget.getValue() && this.target != null && isValidTarget(this.target)) {
         Vec3d d = this.target.getEyePos().subtract(eye);
         float angle = getAngleDiff(d);
         if (angle <= fovHalf && mc.player.distanceTo(this.target) <= r) best = this.target;
      }

      for (Entity e : mc.world.getEntities()) {
         if (!(e instanceof LivingEntity le) || !isValidTarget(le)) continue;
         double dist = mc.player.distanceTo(le);
         if (dist > r) continue;
         Vec3d d = le.getEyePos().subtract(eye);
         float angle = getAngleDiff(d);
         if (angle >= bestAngle) continue;
         if (!this.throughWalls.getValue() && isBlockedByWall(eye, le.getEyePos())) continue;
         bestAngle = angle; best = le;
      }
      return best;
   }

   private float getAngleDiff(Vec3d d) {
      float yaw   = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
      float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x*d.x+d.z*d.z)));
      float dy = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
      float dp = Math.abs(MathHelper.wrapDegrees(pitch - mc.player.getPitch()));
      return Math.max(dy, dp);
   }

   private boolean isBlockedByWall(Vec3d from, Vec3d to) {
      var ctx = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
      return mc.world.raycast(ctx).getType() != net.minecraft.util.hit.HitResult.Type.MISS;
   }

   private boolean isValidTarget(LivingEntity e) {
      if (e == mc.player || !e.isAlive() || e.isSpectator() || e instanceof ArmorStandEntity) return false;
      if (e instanceof PlayerEntity) return this.players.getValue();
      if (e instanceof Monster)      return this.hostiles.getValue();
      return this.passives.getValue();
   }

   private boolean isWeapon() {
      var stack = mc.player.getMainHandStack();
      return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(net.minecraft.item.Items.MACE);
   }
}
