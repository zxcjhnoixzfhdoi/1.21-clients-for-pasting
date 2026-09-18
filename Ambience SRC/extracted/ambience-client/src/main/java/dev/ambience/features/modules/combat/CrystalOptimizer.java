package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.CrystalOptimizerHook;
import dev.ambience.util.CrystalTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

public class CrystalOptimizer extends Module {
   private static final long GHOST_MS = 750L;
   private static CrystalOptimizer INSTANCE;
   private static Vec3d lastBreakPos;
   private static long ghostUntil;

   public CrystalOptimizer() {
      super("CrystalOptimizer", "Removes crystals client-side on hit so you don't wait for the server.", Module.Category.COMBAT);
      INSTANCE = this;
      CrystalOptimizerHook.bind(new CrystalOptimizerHook.Impl() {
         @Override
         public void afterAttack(Entity entity) {
            onAfterAttack(entity);
         }

         @Override
         public boolean consumePredictedExplosion(Vec3d pos) {
            return consumePredicted(pos);
         }
      });
   }

   public static CrystalOptimizer get() {
      return INSTANCE;
   }

   public static void onAfterAttack(Entity entity) {
      CrystalOptimizer inst = get();
      if (inst != null && inst.isEnabled()) {
         inst.removeCrystal(entity);
      }
   }

   // A server-side crystal explosion we already predicted+broke client-side: swallow it.
   public static boolean consumePredicted(Vec3d pos) {
      if (lastBreakPos != null && System.currentTimeMillis() - ghostUntil <= 0L) {
         if (lastBreakPos.squaredDistanceTo(pos) - 16.0 <= 0.0) {
            lastBreakPos = null;
            ghostUntil = 0L;
            return true;
         }
         return false;
      }
      lastBreakPos = null;
      return false;
   }

   private void removeCrystal(Entity entity) {
      if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive()) {
         return;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity player = mc.player;
      if (player == null || !(mc.world instanceof ClientWorld world)) {
         return;
      }
      if (player.isSpectator()) {
         return;
      }
      RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attackDamage = (RegistryEntry<net.minecraft.entity.attribute.EntityAttribute>)(Object)EntityAttributes.ATTACK_DAMAGE;
      if (player.getAttributeValue(attackDamage) - 0.0 <= 0.0) {
         return;
      }
      BlockPos pos = CrystalTracker.c(crystal);
      CrystalTracker.d(pos);
      spawnBreakEffect(world, crystal);
      crystal.emitGameEvent(GameEvent.ENTITY_DIE);
      crystal.remove(RemovalReason.KILLED);
      clearCrosshair(mc, player, crystal);
   }

   private static void spawnBreakEffect(ClientWorld world, EndCrystalEntity crystal) {
      double x = crystal.getX();
      double y = crystal.getY();
      double z = crystal.getZ();
      Vec3d pos = new Vec3d(x, y, z);
      float pitch = (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F;

      SoundEvent explode = (SoundEvent) SoundEvents.ENTITY_GENERIC_EXPLODE.value();
      world.playSoundClient(x, y, z, explode, SoundCategory.BLOCKS, 4.0F, pitch, false);
      world.addParticleClient(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
      

      lastBreakPos = pos;
      ghostUntil = System.currentTimeMillis() + GHOST_MS;
   }

   private static void clearCrosshair(MinecraftClient mc, ClientPlayerEntity player, EndCrystalEntity crystal) {
      if (mc.targetedEntity != crystal) {
         return;
      }
      HitResult hit = player.raycast(player.getBlockInteractionRange(), 1.0F, false);
      mc.targetedEntity = null;
      mc.crosshairTarget = hit;
   }
}
