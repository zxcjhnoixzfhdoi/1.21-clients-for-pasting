package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class WorldParticles extends Module {
   private final Setting<Style> style;
   private final Setting<Double> density;
   private final Setting<Double> range;
   private final Setting<Double> height;
   private final Setting<Vector2f> speed;
   private final Setting<Double> circleSize;
   private int tickCounter;

   public WorldParticles() {
      super("WorldParticles", "Ambient stars, rings, and falling particles around you.", Module.Category.VISUALS);
      this.style      = this.mode("Style",      Style.Mix).setPage("General");
      this.density    = this.num("Density",     4.0, 1.0,  10.0).setPage("General");
      this.range      = this.num("Range",      18.0, 6.0,  48.0).setPage("General");
      this.height     = this.num("Height",     10.0, 3.0,  24.0).setPage("General");
      this.speed      = this.vec2f("Speed",    0.02F, 0.06F).setPage("General");
      this.circleSize = this.num("CircleSize",  2.5, 1.0,   6.0).setPage("General");
      this.circleSize.setVisibility(v -> this.style.getValue() == Style.Circles || this.style.getValue() == Style.Mix);
   }

   @Override
   public void onDisable() {
      this.tickCounter = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) return;
      ClientWorld world = mc.world;
      if (!!world.getDimension().hasCeiling()) return;   // overworld-only

      int densityTicks = (int) Math.round(this.density.getValue() * 0.4);
      int interval = Math.max(1, 1 - densityTicks);
      if (++this.tickCounter < interval) return;
      this.tickCounter = 0;

      Style active = this.style.getValue();
      if (active == Style.Mix) {
         int roll = ThreadLocalRandom.current().nextInt(3);
         active = roll == 0 ? Style.Circles : roll == 1 ? Style.Stars : Style.Fall;
      }
      switch (active) {
         case Stars  -> spawnStars((int) Math.round(this.density.getValue() * 0.35) + 1);
         case Circles -> spawnCircle();
         case Fall   -> spawnFalling((int) Math.round(this.density.getValue() * 2));
      }
   }

   private void spawnStars(int count) {
      Vec3d pos = mc.player.getEntityPos();
      double r = this.range.getValue(), h = this.height.getValue();
      for (int i = 0; i < count; i++) {
         double x = pos.x + randOff(r);
         double y = pos.y + ThreadLocalRandom.current().nextDouble(2.0, h);
         double z = pos.z + randOff(r);
         double sx = randSpeed(), sy = randSpeed(), sz = randSpeed();
         spawnParticle(ParticleTypes.END_ROD, x, y, z, sx, sy, sz);
         if (ThreadLocalRandom.current().nextBoolean()) {
            spawnParticle(ParticleTypes.GLOW, x, y, z, 0, 0, 0);
         }
      }
   }

   private void spawnCircle() {
      Vec3d pos = mc.player.getEntityPos();
      double r = this.circleSize.getValue();
      double cy = pos.y + 1.5 + ThreadLocalRandom.current().nextDouble(this.height.getValue() * 0.35);
      int pts = (int) Math.round(this.density.getValue() * 2);
      double baseAngle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
      for (int i = 0; i < pts; i++) {
         double angle = baseAngle + (Math.PI * 2 * i / pts);
         double x = pos.x + Math.cos(angle) * r;
         double z = pos.z + Math.sin(angle) * r;
         spawnParticle(ParticleTypes.ELECTRIC_SPARK, x, cy, z, 0, 0.01, 0);
         spawnParticle(ParticleTypes.GLOW, x, cy + 0.05, z, 0, 0, 0);
      }
   }

   private void spawnFalling(int count) {
      Vec3d pos = mc.player.getEntityPos();
      double r = this.range.getValue(), h = this.height.getValue();
      double maxSpeed = this.speed.getValue().y();
      for (int i = 0; i < count; i++) {
         double x = pos.x + randOff(r);
         double y = pos.y + h + ThreadLocalRandom.current().nextDouble(2.0, 6.0);
         double z = pos.z + randOff(r);
         SimpleParticleType type = ThreadLocalRandom.current().nextBoolean()
             ? ParticleTypes.FALLING_SPORE_BLOSSOM : ParticleTypes.CHERRY_LEAVES;
         spawnParticle(type, x, y, z, 0, -maxSpeed, 0);
      }
   }

   private void spawnParticle(SimpleParticleType type, double x, double y, double z, double vx, double vy, double vz) {
      mc.world.addParticleClient(type, x, y, z, vx, vy, vz);
   }

   private double randOff(double range) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      return r.nextDouble(-range, range);
   }

   private double randSpeed() {
      Vector2f v = this.speed.getValue();
      return ThreadLocalRandom.current().nextDouble(v.x(), v.y()) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
   }

   public enum Style { Stars, Circles, Fall, Mix }
}
