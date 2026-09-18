package dev.ambience.features.modules.player;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoMLG extends Module {
   // gravity simulation constants for fall-height prediction
   private static final double GRAVITY    = 0.08;
   private static final double DRAG_Y     = 0.98;
   private static final double DRAG_HORZ  = 0.91;

   private static AutoMLG INSTANCE;
   private final Setting<Float>   minFall;
   private final Setting<Float>   placeDist;
   private final Setting<Boolean> silentAim;
   private final Setting<Boolean> drain;
   private final Setting<Float>   drainDelay;
   private final Setting<Boolean> restoreSlot;
   private final Stopwatch timer;
   private int   savedSlot;
   private BlockPos waterPos;
   private boolean placed;
   private boolean draining;
   private boolean restoring;
   private float   savedPitch;

   public AutoMLG() {
      super("AutoMLG", "Water buckets under you on lethal falls, then scoops it back up.", Module.Category.PLAYER);
      this.minFall    = this.num("MinFall",    3f, 2f, 10f).setPage("General");
      this.placeDist  = this.num("PlaceDist",  4f, 2f,  6f).setPage("General");
      this.silentAim  = this.bool("SilentAim",  true).setPage("General");
      this.drain      = this.bool("Drain",      true).setPage("General");
      this.drainDelay = this.num("DrainDelay", 400f, 0f, 2000f)
                           .setVisibility(v -> this.drain.getValue()).setPage("General");
      this.restoreSlot = this.bool("RestoreSlot", true).setPage("General");
      this.timer      = new Stopwatch();
      this.savedSlot  = -1;
      INSTANCE = this;
   }

   public static AutoMLG get() { return INSTANCE; }

   // active = we're mid-MLG (placed or draining or restoring)
   public static boolean a(int unused) {
      if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
      return INSTANCE.placed || INSTANCE.draining || INSTANCE.restoring
          || INSTANCE.savedSlot != -1;
   }

   @Override public void onDisable() { resetState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (AutoTotem.c(0) || mc.currentScreen != null) return;

      ClientPlayerEntity player = mc.player;

      // draining phase: pick water back up
      if (this.placed && !this.draining) {
         if (this.timer.a((long)(float) this.drainDelay.getValue())) {
            this.draining = true;
         }
         return;
      }

      if (this.draining) {
         drainWater(); return;
      }

      if (this.restoring) {
         if (this.savedSlot >= 0 && this.restoreSlot.getValue()) {
            player.getInventory().setSelectedSlot(this.savedSlot);
            Access.ensureHasSentCarriedItem(mc.interactionManager);
         }
         resetState(); return;
      }

      // check if we're in a lethal fall
      if (player.isOnGround() || player.isTouchingWater() || player.isRiding()) return;
      if (player.getVelocity().y >= 0) return;
      if (!shouldPlace(player)) return;
      if (!this.drain.getValue()) return;

      // find bucket
      int bucketSlot = findBucket(player);
      if (bucketSlot == -1) return;

      // calculate landing pos
      Vec3d landPos = predictLanding(player);
      if (landPos == null) return;
      BlockPos placePos = BlockPos.ofFloored(landPos);

      // look down, place bucket
      this.savedSlot = player.getInventory().getSelectedSlot();
      this.savedPitch = player.getPitch();
      player.getInventory().setSelectedSlot(bucketSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      if (this.silentAim.getValue()) {
         SilentAim.a(player.getYaw(), 89.9f, SilentAim.a.COMBAT);
      }
      placeBucket(placePos);
      this.waterPos = placePos;
      this.placed   = true;
      this.timer.a();
   }

   private void drainWater() {
      if (this.waterPos == null) { resetState(); return; }
      if (mc.world.getBlockState(this.waterPos).getBlock() != Blocks.WATER
              && mc.world.getBlockState(this.waterPos).getBlock() != Blocks.WATER) {
         resetState(); return;
      }
      int bucketSlot = findWaterBucket(mc.player);
      if (bucketSlot == -1) bucketSlot = findBucket(mc.player);
      if (bucketSlot == -1) { resetState(); return; }
      mc.player.getInventory().setSelectedSlot(bucketSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      var ctx = new RaycastContext(mc.player.getEyePos(), Vec3d.ofCenter(this.waterPos),
                                   RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
      if (mc.world.raycast(ctx) instanceof BlockHitResult bhr) {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
      }
      SilentAim.e();
      this.draining = false;
      this.restoring = true;
   }

   private void placeBucket(BlockPos pos) {
      var ctx = new RaycastContext(mc.player.getEyePos(), Vec3d.ofCenter(pos).add(0, 0.5, 0),
                                   RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
      if (mc.world.raycast(ctx) instanceof BlockHitResult bhr) {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
      }
   }

   private boolean shouldPlace(ClientPlayerEntity player) {
      double vy = player.getVelocity().y;
      // simulate fall, check if landing damage > minFall
      double v = vy; double y = player.getY(); int ticks = 0;
      while (ticks < 200) {
         v = (v - GRAVITY) * DRAG_Y; y += v;
         if (y <= 0 || mc.world.getBlockState(BlockPos.ofFloored(player.getX(), y - 0.1, player.getZ())).isSolidBlock(mc.world, BlockPos.ofFloored(player.getX(), y, player.getZ()))) break;
         ticks++;
      }
      double fallDist = player.getY() - y;
      return fallDist >= this.minFall.getValue() + 3.0;
   }

   private Vec3d predictLanding(ClientPlayerEntity player) {
      double x = player.getX(), y = player.getY(), z = player.getZ();
      double vx = player.getVelocity().x, vy = player.getVelocity().y, vz = player.getVelocity().z;
      double dist = this.placeDist.getValue();
      for (int i = 0; i < 60; i++) {
         vy = (vy - GRAVITY) * DRAG_Y;
         vx *= DRAG_HORZ; vz *= DRAG_HORZ;
         y += vy; x += vx; z += vz;
         BlockPos pos = BlockPos.ofFloored(x, y - 0.01, z);
         if (!mc.world.getBlockState(pos).isAir()) return new Vec3d(x, y, z);
      }
      return null;
   }

   private int findBucket(ClientPlayerEntity player) {
      for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
      for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isOf(Items.BUCKET)) return -1;
      return -1;
   }

   private int findWaterBucket(ClientPlayerEntity player) {
      for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
      return -1;
   }

   private void resetState() {
      SilentAim.e();
      this.savedSlot  = -1;
      this.waterPos   = null;
      this.placed     = false;
      this.draining   = false;
      this.restoring  = false;
   }
}
