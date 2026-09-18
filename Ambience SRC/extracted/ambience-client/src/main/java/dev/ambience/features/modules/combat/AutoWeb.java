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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class AutoWeb extends Module {
   private static AutoWeb INSTANCE;
   private static final double REACH = 0.03;
   private static final Direction[] DIRS = Direction.values();
   private final Setting<Vector2f> delay;
   private final Setting<SwitchMode> switchBack;
   private final Setting<Integer>   switchSlot;
   private final Stopwatch timer;
   private long   delayMs;
   private boolean aiming;
   private boolean placing;
   private PlayerEntity target;

   public AutoWeb() {
      super("AutoWeb", "Places cobwebs on nearby opponents.", Module.Category.COMBAT);
      this.delay      = this.vec2f("Delay", 30f, 30f).setVec2TrackBounds(0f, 250f).setPage("General");
      this.switchBack = this.mode("Switch Back", SwitchMode.Slot).setPage("General");
      this.switchSlot = this.num("Switch Slot", 1, 1, 9)
                           .setVisibility(v -> this.switchBack.getValue() == SwitchMode.Slot)
                           .setPage("General");
      this.timer = new Stopwatch();
      INSTANCE = this;
      this.delayMs = randomDelay();
   }

   public static AutoWeb get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && (INSTANCE.aiming || INSTANCE.placing); }

   @Override public String getDisplayInfo() {
      if (this.target != null) return this.target.getName().getString();
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo == hi ? lo + "ms" : String.format("%.0f-%.0fms", v.x(), v.y());
   }

   @Override public void onEnable() { this.delayMs = randomDelay(); clearState(); }
   @Override public void onDisable() { SilentAim.e(); clearState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0) || LungeSwap.a(0)) return;
      if (mc.currentScreen != null || Freecam.b(0)) { clearAndReset(); return; }
      if (AutoTotem.c(0) || AnchorMacro.a() || AutoMace.a(0)) { clearAndReset(); return; }

      // find cobweb in inventory
      int webSlot = InventoryUtil.a(mc.player, Items.COBWEB);
      if (webSlot == -1) { clearState(); return; }

      // find closest player to web
      PlayerEntity nearest = nearestTarget();
      this.target = nearest;
      if (nearest == null) { clearState(); return; }

      // find placeable position at their feet
      BlockPos placePos = findPlacePos(nearest);
      if (placePos == null) { clearState(); return; }

      boolean aimed = RotationManager.b(mc.player, placePos, 4.5);
      if (!aimed) { this.aiming = true; return; }
      this.placing = true;

      if (!this.timer.a(this.delayMs)) {
         Access.setRightClickDelay(mc, 0);
         return;
      }

      // execute place
      int saved = mc.player.getInventory().getSelectedSlot();
      InventoryUtil.a(mc.player, webSlot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
      Vec3d hit = Vec3d.ofCenter(placePos);
      BlockHitResult hr = new BlockHitResult(hit, Direction.UP, placePos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hr);
      mc.player.swingHand(Hand.MAIN_HAND);
      restore(saved);
      this.delayMs = randomDelay();
      this.timer.a();
   }

   private void restore(int saved) {
      if (this.switchBack.getValue() == SwitchMode.Slot) {
         int slot = this.switchSlot.getValue() - 1;  // 1-indexed setting
         mc.player.getInventory().setSelectedSlot(slot);
      } else {
         mc.player.getInventory().setSelectedSlot(saved);
      }
      Access.ensureHasSentCarriedItem(mc.interactionManager);
   }

   private PlayerEntity nearestTarget() {
      if (mc.world == null) return null;
      PlayerEntity best = null; double bestDist = Double.MAX_VALUE;
      for (PlayerEntity p : mc.world.getPlayers()) {
         if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
         double d = mc.player.distanceTo(p);
         if (d < bestDist && d < 6.0) { bestDist = d; best = p; }
      }
      return best;
   }

   private BlockPos findPlacePos(PlayerEntity target) {
      BlockPos feet = target.getBlockPos();
      // try feet, then adjacent
      for (BlockPos candidate : new BlockPos[]{
              feet, feet.north(), feet.south(), feet.east(), feet.west()}) {
         if (mc.world.getBlockState(candidate).isAir()) return candidate;
      }
      return null;
   }

   private void clearState() {
      this.aiming  = false;
      this.placing = false;
      this.target  = null;
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

   public enum SwitchMode { Slot, Normal }
}
