package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.LungeSwap;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class AutoDrain extends Module {
   private static AutoDrain INSTANCE;
   private static final double REACH_MARGIN = 0.03;
   private final Setting<Vector2f> delay;
   private final Setting<Double>   range;
   private final Stopwatch         timer;
   private long    delayMs;
   private boolean silentActive;
   private boolean aiming;
   private BlockPos targetPos;
   private int      bucketSlot;

   public AutoDrain() {
      super("AutoDrain", "Scoops water other players place with buckets.", Module.Category.COMBAT);
      this.delay  = this.vec2f("Delay", 30f, 30f).setVec2TrackBounds(0f, 250f).setPage("General");
      this.range  = this.num("Range", 4.5, 1.0, 6.0).setPage("General");
      this.timer  = new Stopwatch();
      this.bucketSlot = -1;
      INSTANCE = this;
      this.delayMs = randomDelay();
   }

   public static AutoDrain get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && (INSTANCE.silentActive || INSTANCE.aiming); }

   @Override public String getDisplayInfo() {
      if (this.targetPos != null) return this.targetPos.toShortString();
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo == hi ? lo + "ms" : String.format("%.0f-%.0fms", v.x(), v.y());
   }

   @Override public String getDebugState() {
      if (this.targetPos == null && !this.silentActive) return null;
      String pos = this.targetPos != null ? this.targetPos.toShortString() : "—";
      return pos + " silent=" + this.silentActive;
   }

   @Override
   public void onEnable() {
      this.delayMs = randomDelay();
      clearState();
   }

   @Override
   public void onDisable() {
      SilentAim.e();
      clearState();
   }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0)) return;

      // toggle silentActive based on whether we are in aiming state
      if (!this.aiming) {
         this.aiming = !this.silentActive;
      } else {
         if (AutoMace.a(0)) {
            SilentAim.e();
            this.silentActive = false;
            clearState(); return;
         }
         this.silentActive = true;
      }

      if (mc.currentScreen != null || Freecam.b(0)) { clearAndReset(); return; }
      if (AutoTotem.c(0) || AnchorMacro.a() || AutoMace.a(0) || AutoEsc.a(0) || LungeSwap.a(0)) { clearAndReset(); return; }
      if (SilentAim.a(SilentAim.a.COMBAT)) {
         if (this.silentActive) { this.targetPos = null; return; }
      }
      if (RotationManager.d()) return;

      // find bucket
      this.bucketSlot = InventoryUtil.a(mc.player, Items.BUCKET);
      if (this.bucketSlot == -1) { this.targetPos = null; clearState(); return; }

      // find water block to drain
      BlockPos target = findWater();
      if (target == null) { this.targetPos = null; clearState(); return; }
      this.targetPos = target;

      double r = this.range.getValue();
      boolean inRange = RotationManager.c(mc.player, target, r);
      if (!inRange) return;

      boolean aimed = RotationManager.b(mc.player, target, r);
      if (!aimed) return;

      // aimed — fire on timer
      if (this.timer.a(this.delayMs)) {
         // scoop
         this.a(Vec3d.ofCenter(target), 0);
      } else {
         Access.setRightClickDelay(mc, 0);
      }
   }

   // perform the scoop interact
   public void a(Vec3d center, int unused) {
      // switch to bucket, interact, restore — simplified
      int saved = mc.player.getInventory().getSelectedSlot();
      InventoryUtil.a(mc.player, this.bucketSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
      mc.player.getInventory().setSelectedSlot(saved);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      this.delayMs = randomDelay();
      this.timer.a();
   }

   private BlockPos findWater() {
      ClientWorld world = mc.world;
      ClientPlayerEntity player = mc.player;
      double r = this.range.getValue();
      BlockPos playerPos = player.getBlockPos();
      BlockPos best = null; double bestDist = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.iterate(
               playerPos.add(-(int)r, -(int)r, -(int)r),
               playerPos.add( (int)r,  (int)r,  (int)r))) {
         FluidState fs = world.getFluidState(pos);
         if (!fs.isOf(Fluids.WATER) || !fs.isStill()) continue;
         double d = player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos));
         if (d > r * r) continue;
         if (d < bestDist) { bestDist = d; best = pos.toImmutable(); }
      }
      return best;
   }

   private void clearState() {
      this.aiming      = false;
      this.silentActive = false;
      this.targetPos   = null;
      this.bucketSlot  = -1;
   }

   private void clearAndReset() {
      SilentAim.e();
      clearState();
   }

   private long randomDelay() {
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }
}
