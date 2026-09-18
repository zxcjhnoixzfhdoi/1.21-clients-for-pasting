package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.CrystalTracker;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class AutoCrystal extends Module {
   private static AutoCrystal INSTANCE;
   private static final double CRYSTAL_DIST = 2.88;

   private final Setting<Vector2f> delay;
   private final Setting<Boolean>  headBob;
   private final Setting<Boolean>  inAir;
   private final Setting<Boolean>  damageTick;
   private final Setting<Boolean>  pauseOnKill;
   private final Setting<Boolean>  switchSlot;
   private final Setting<Boolean>  place;
   private final Setting<Boolean>  doBreak;
   private final Setting<Double>   range;
   private final Stopwatch placeTimer;
   private final Stopwatch breakTimer;
   private long   placeDelay;
   private long   breakDelay;
   private int    crystalCount;
   private float  savedYaw, savedPitch;
   private int    crystalSlot;
   private boolean placing;
   private boolean breaking;

   public AutoCrystal() {
      super("AutoCrystal", "Places and breaks crystals on crosshair obsidian/bedrock", Module.Category.COMBAT);
      this.delay       = this.vec2f("Delay",      0f, 0f).setPage("General");
      this.headBob     = this.bool("Head Bob",   false).setPage("General");
      this.inAir       = this.bool("In Air",     false).setPage("General");
      this.damageTick  = this.bool("Damage Tick",false).setPage("General");
      this.pauseOnKill = this.bool("Pause On Kill", false).setPage("General");
      this.switchSlot  = this.bool("Switch",     false).setPage("General");
      this.place       = this.bool("Place",       true).setPage("General");
      this.doBreak     = this.bool("Break",       true).setPage("General");
      this.range       = this.num("Range",        4.5, 1.0, 6.0).setPage("General");
      this.placeTimer  = new Stopwatch();
      this.breakTimer  = new Stopwatch();
      this.crystalSlot = -1;
      INSTANCE = this;
   }

   public static AutoCrystal get() { return INSTANCE; }
   public static boolean b()       { return INSTANCE != null && INSTANCE.isEnabled(); }

   @Override public void onDisable() { resetState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0)) return;
      if (AutoTotem.c(0) || AnchorMacro.a()) return;
      if (!this.inAir.getValue() && !mc.player.isOnGround()) return;

      // break phase: attack crystal in crosshair
      if (this.doBreak.getValue() && this.breaking) {
         if (this.breakTimer.a(this.breakDelay)) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystalEntity crystal) {
               mc.interactionManager.attackEntity(mc.player, crystal);
               mc.player.swingHand(Hand.MAIN_HAND);
            }
            this.breaking = false;
         }
         return;
      }

      // place phase: place crystal on crosshair block
      if (this.place.getValue()) {
         HitResult hit = mc.crosshairTarget;
         if (hit instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            var state = mc.world.getBlockState(pos);
            if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) {
               if (mc.player.distanceTo(mc.player) <= this.range.getValue()) {
                  this.crystalSlot = findCrystal();
                  if (this.crystalSlot != -1 && this.placeTimer.a(this.placeDelay)) {
                     int saved = mc.player.getInventory().getSelectedSlot();
                     if (this.switchSlot.getValue()) {
                        mc.player.getInventory().setSelectedSlot(this.crystalSlot);
                        Access.ensureHasSentCarriedItem(mc.interactionManager);
                     }
                     ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     if (this.switchSlot.getValue()) {
                        mc.player.getInventory().setSelectedSlot(saved);
                        Access.ensureHasSentCarriedItem(mc.interactionManager);
                     }
                     if (result.isAccepted()) {
                        this.breaking = true;
                        this.breakDelay = randomDelay();
                        this.breakTimer.a();
                     }
                     this.placeDelay = randomDelay();
                     this.placeTimer.a();
                  }
               }
            }
         }
      }
   }

   private int findCrystal() {
      for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
      return -1;
   }

   private long randomDelay() {
      Vector2f v = this.delay.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }

   private void resetState() {
      this.placing     = false;
      this.breaking    = false;
      this.crystalSlot = -1;
   }
}
