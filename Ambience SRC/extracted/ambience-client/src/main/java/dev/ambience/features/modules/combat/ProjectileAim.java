package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.modules.render.Freelook;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.ProjectileAimHook;
import dev.ambience.util.RotationSmoother;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class ProjectileAim extends Module {
   // arrow physics constants
   private static final float BOW_SPEED   = 3.0F;
   private static final float ARROW_DRAG  = 0.99F;
   private static final float ARROW_GRAV  = 0.05F;

   private static ProjectileAim INSTANCE;
   private final Setting<Float>   speed;
   private final Setting<Float>   fov;
   private final Setting<Float>   range;
   private final Setting<Boolean> predict;
   private final Setting<Boolean> stickyTarget;
   private final Setting<Boolean> throughWalls;
   private final Setting<Boolean> bow;
   private final Setting<Boolean> crossbow;
   private final Setting<Boolean> players;
   private final Setting<Boolean> hostiles;
   private final Setting<Boolean> passives;
   private final RotationSmoother smoother;
   private LivingEntity target;

   public ProjectileAim() {
      super("ProjectileAim", "Smoothly aims bows and crossbows at people.", Module.Category.COMBAT);
      this.speed       = this.num("Speed",       8f,  1f, 20f).setPage("General");
      this.fov         = this.num("FOV",        90f, 20f, 180f).setPage("General");
      this.range       = this.num("Range",      48f,  8f,  80f).setPage("General");
      this.predict     = this.bool("Predict",    true).setPage("General");
      this.stickyTarget = this.bool("StickyTarget", true).setPage("General");
      this.throughWalls = this.bool("ThroughWalls", false).setPage("General");
      this.bow         = this.bool("Bow",        true).setPage("General");
      this.crossbow    = this.bool("Crossbow",   true).setPage("General");
      this.players     = this.bool("Players",    true).setPage("Targets");
      this.hostiles    = this.bool("Hostiles",  false).setPage("Targets");
      this.passives    = this.bool("Passives",  false).setPage("Targets");
      this.smoother    = new RotationSmoother();
      INSTANCE = this;
      ProjectileAimHook.bind(ProjectileAim::onRenderTick);
   }

   public static ProjectileAim get() { return INSTANCE; }
   public static void onRenderTick(double partialTick) { if (INSTANCE != null && INSTANCE.isEnabled()) INSTANCE.tick(partialTick); }

   @Override public String getDisplayInfo() { return this.target != null ? this.target.getName().getString() : null; }
   @Override public void   onDisable()      { this.target = null; this.smoother.a(); }

   private void tick(double partialTick) {
      if (!nullCheck() || mc.currentScreen != null) { this.target = null; return; }
      if (Freecam.a(0) || AutoTotem.c(0)) return;
      if (!isHoldingProjectileWeapon()) { this.target = null; return; }

      LivingEntity best = findTarget();
      if (best == null) { this.target = null; return; }
      this.target = best;

      float projectileSpeed = getProjectileSpeed();
      Vec3d eye = mc.player.getEyePos();
      Vec3d targetPos = this.predict.getValue() ? predictPos(best, eye, projectileSpeed) : best.getEyePos();
      Vec3d delta = targetPos.subtract(eye);

      float yaw   = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
      float pitch = computePitch(delta, projectileSpeed);

      // aim applied directly via mc.player
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
   }

   private Vec3d predictPos(LivingEntity e, Vec3d from, float speed) {
      Vec3d pos = e.getLerpedPos(0.5f).add(0, e.getEyeHeight(e.getPose()) * 0.85, 0);
      Vec3d vel = e.getVelocity();
      double dist = from.distanceTo(pos);
      double tof = dist / speed;
      return pos.add(vel.multiply(tof));
   }

   /** Solve for pitch that hits target at distance given gravity. */
   private float computePitch(Vec3d delta, float speed) {
      double h = delta.y;
      double hDist = Math.sqrt(delta.x*delta.x + delta.z*delta.z);
      double v = speed;
      double g = ARROW_GRAV;
      // simple ballistic: v^2/g ≈ range; use quadratic formula for pitch
      double denom = v * v + g * h;
      if (denom <= 0) return (float) -Math.toDegrees(Math.atan2(h, hDist));
      double discriminant = v * v * v * v - g * (g * hDist * hDist + 2 * h * v * v);
      if (discriminant < 0) return (float) -Math.toDegrees(Math.atan2(h, hDist));
      double tanPitch = (v * v - Math.sqrt(discriminant)) / (g * hDist);
      return (float) -Math.toDegrees(Math.atan(tanPitch));
   }

   private float getProjectileSpeed() {
      ItemStack stack = mc.player.getMainHandStack();
      if (stack.getItem() instanceof CrossbowItem) return 3.15F;
      return BOW_SPEED;
   }

   private boolean isHoldingProjectileWeapon() {
      ItemStack stack = mc.player.getMainHandStack();
      if (stack.getItem() instanceof BowItem)      return this.bow.getValue();
      if (stack.getItem() instanceof CrossbowItem) {
         var charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
         return this.crossbow.getValue() && charged != null && !charged.isEmpty();
      }
      return false;
   }

   private LivingEntity findTarget() {
      if (mc.world == null) return null;
      float r = this.range.getValue(), fovH = this.fov.getValue() / 2f;
      Vec3d eye = mc.player.getEyePos();
      LivingEntity best = null; double bestAngle = fovH;

      if (this.stickyTarget.getValue() && this.target != null && isValidTarget(this.target)) {
         double dist = mc.player.distanceTo(this.target);
         float angle = getAngle(this.target.getEyePos().subtract(eye));
         if (angle <= fovH && dist <= r) best = this.target;
      }
      for (Entity e : mc.world.getEntities()) {
         if (!(e instanceof LivingEntity le) || !isValidTarget(le)) continue;
         double dist = mc.player.distanceTo(le);
         if (dist > r) continue;
         float angle = getAngle(le.getEyePos().subtract(eye));
         if (angle >= bestAngle) continue;
         if (!this.throughWalls.getValue() && isBlocked(eye, le.getEyePos())) continue;
         bestAngle = angle; best = le;
      }
      return best;
   }

   private float getAngle(Vec3d d) {
      float y = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
      float p = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x*d.x+d.z*d.z)));
      return Math.max(Math.abs(MathHelper.wrapDegrees(y - mc.player.getYaw())),
                      Math.abs(MathHelper.wrapDegrees(p - mc.player.getPitch())));
   }

   private boolean isBlocked(Vec3d from, Vec3d to) {
      var ctx = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
      return mc.world.raycast(ctx).getType() != net.minecraft.util.hit.HitResult.Type.MISS;
   }

   private boolean isValidTarget(LivingEntity e) {
      if (e == mc.player || !e.isAlive() || e.isSpectator() || e instanceof ArmorStandEntity) return false;
      if (e instanceof PlayerEntity) return this.players.getValue();
      if (e instanceof Monster)      return this.hostiles.getValue();
      return this.passives.getValue();
   }
}
