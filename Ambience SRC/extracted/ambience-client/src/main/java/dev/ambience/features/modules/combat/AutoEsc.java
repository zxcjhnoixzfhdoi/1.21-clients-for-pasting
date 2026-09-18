package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.AutoEscHook;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoEsc extends Module {
   private static AutoEsc INSTANCE;
   private static boolean active;
   private final Setting<Float> holdMs;
   private final Stopwatch timer;
   private State   state;
   private int     savedSlot;
   private int     waterSlot;
   private BlockPos targetPos;

   public AutoEsc() {
      super("AutoEsc", "Places water on cobweb you are stuck in, then scoops it back up.",
            Module.Category.COMBAT);
      this.holdMs    = this.num("Hold", 400f, 0f, 1500f).setPage("General");
      this.timer     = new Stopwatch();
      this.state     = State.IDLE;
      this.savedSlot = -1;
      this.waterSlot = -1;
      INSTANCE = this;
      AutoEscHook.bind(new AutoEscHook.Impl() { @Override public boolean blocksInteraction() { return false; } });
   }

   public static AutoEsc get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && active; }

   @Override public void onDisable() { resetState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0) || Freecam.a(0)) return;
      if (AutoTotem.c(0) || AnchorMacro.a()) return;

      switch (this.state) {
         case IDLE     -> detectCobweb();
         case PLACE    -> placeWater();
         case HOLD     -> { if (this.timer.a((long) this.holdMs.getValue().longValue())) this.state = State.DRAIN; }
         case DRAIN    -> drainWater();
         case RESTORE  -> restore();
      }
   }

   private void detectCobweb() {
      ClientPlayerEntity player = mc.player;
      BlockPos pos = player.getBlockPos();
      if (mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB) return;
      // player stuck in cobweb — find water bucket
      this.waterSlot = findWaterBucket();
      if (this.waterSlot == -1) return;
      this.savedSlot = player.getInventory().getSelectedSlot();
      this.targetPos = pos;
      active = true;
      this.state = State.PLACE;
   }

   private void placeWater() {
      if (this.targetPos == null) { resetState(); return; }
      player().getInventory().setSelectedSlot(this.waterSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      // look down at cobweb
      SilentAim.a(mc.player.getYaw(), 89.9f, SilentAim.a.COMBAT);
      Vec3d hit = Vec3d.ofCenter(this.targetPos);
      BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, this.targetPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.timer.a();
      this.state = State.HOLD;
   }

   private void drainWater() {
      if (this.targetPos == null) { resetState(); return; }
      int bucketSlot = findEmptyBucket();
      if (bucketSlot == -1) bucketSlot = this.waterSlot;
      player().getInventory().setSelectedSlot(bucketSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      Vec3d hit = Vec3d.ofCenter(this.targetPos);
      BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, this.targetPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
      mc.player.swingHand(Hand.MAIN_HAND);
      SilentAim.e();
      this.state = State.RESTORE;
   }

   private void restore() {
      if (this.savedSlot >= 0) {
         player().getInventory().setSelectedSlot(this.savedSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      resetState();
   }

   private void resetState() {
      SilentAim.e();
      this.state     = State.IDLE;
      this.savedSlot = -1;
      this.waterSlot = -1;
      this.targetPos = null;
      active         = false;
   }

   private int findWaterBucket() {
      for (int i = 0; i < 9; i++) if (player().getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
      return -1;
   }

   private int findEmptyBucket() {
      for (int i = 0; i < 9; i++) if (player().getInventory().getStack(i).isOf(Items.BUCKET)) return i;
      return -1;
   }

   private ClientPlayerEntity player() { return mc.player; }

   private enum State { IDLE, PLACE, HOLD, DRAIN, RESTORE }
}
