package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.AnchorMacroHook;
import dev.ambience.inject.Access;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

public class AnchorMacro extends Module {
   private static AnchorMacro INSTANCE;
   private static boolean     swapping;

   private final Setting<Boolean>   silentAim;
   private final Setting<Boolean>   place;
   private final Setting<Boolean>   protect;
   private final Setting<Boolean>   doBreak;
   private final Setting<Boolean>   diagonalShield;
   private final Setting<Vector2f>  placeMs;
   private final Setting<Vector2f>  actionMs;
   private final Setting<Vector2f>  protectMs;
   private final Setting<Double>    range;
   private final Stopwatch          timer;

   // state machine
   private State     state;
   private BlockPos  anchorPos;       // where we placed/will place the anchor
   private BlockPos  glowstonePos;    // where we'll place glowstone to charge
   private BlockHitResult anchorHit;  // hit result for the anchor face
   private int       anchorSlot;
   private int       glowstoneSlot;
   private int       shieldSlot;
   private int       savedSlot;
   private float     savedYaw, savedPitch;
   private boolean   aiming;
   private BlockHitResult shieldHit;
   private int       chargesPlaced;

   public AnchorMacro() {
      super("AnchorMacro", "Auto-places, charges, shields, and detonates anchors.", Module.Category.COMBAT);
      this.silentAim      = this.bool("SilentAim",      true).setPage("General");
      this.place          = this.bool("Place",           true).setPage("General");
      this.protect        = this.bool("Protect",         true).setPage("General");
      this.doBreak        = this.bool("Break",           true).setPage("General");
      this.diagonalShield = this.bool("DiagonalShield",  true)
                               .setVisibility(v -> this.protect.getValue()).setPage("General");
      this.placeMs        = this.vec2f("PlaceMs",       0f, 0f)
                               .setVisibility(v -> this.place.getValue()).setPage("General");
      this.actionMs       = this.vec2f("ActionMs",      0f, 0f).setPage("General");
      this.protectMs      = this.vec2f("ProtectMs",     0f, 0f)
                               .setVisibility(v -> this.protect.getValue()).setPage("General");
      this.range          = this.num("Range",            4.5, 3.0, 6.0).setPage("General");
      this.timer          = new Stopwatch();
      this.state          = State.IDLE;
      this.anchorSlot     = -1;
      this.glowstoneSlot  = -1;
      this.shieldSlot     = -1;
      this.savedSlot      = -1;
      INSTANCE = this;
      AnchorMacroHook.bind(new AnchorMacroHook.Impl() { @Override public boolean blocksInteraction() { return AnchorMacro.a(); } });
   }

   public static AnchorMacro get()  { return INSTANCE; }
   public static boolean a()        { return INSTANCE != null && INSTANCE.isEnabled() && swapping; }

   @Override public String getDebugState() { return this.state != State.IDLE ? this.state.name() : null; }
   @Override public void   onDisable()     { resetState(); }

   public static void onHookTick() {
      if (INSTANCE != null) INSTANCE.tick();
   }

   private void tick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (mc.currentScreen != null || AutoTotem.c(0)) return;

      switch (this.state) {
         case IDLE      -> detectAndStart();
         case PLACE     -> placeAnchor();
         case CHARGE    -> chargeAnchor();
         case PROTECT   -> placeShield();
         case DETONATE  -> detonate();
         case RESTORE   -> restoreSlot();
      }
   }

   private void detectAndStart() {
      HitResult hit = mc.crosshairTarget;
      if (!(hit instanceof BlockHitResult bhr)) return;
      BlockPos pos = bhr.getBlockPos();
      var state = mc.world.getBlockState(pos);
      boolean isAnchorBase = state.isOf(Blocks.RESPAWN_ANCHOR) || state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
      if (!isAnchorBase) return;

      // find items
      this.anchorSlot     = findSlot(Items.RESPAWN_ANCHOR);
      this.glowstoneSlot  = findSlot(Items.GLOWSTONE);
      this.shieldSlot     = findSlot(Items.SHIELD);
      this.savedSlot      = mc.player.getInventory().getSelectedSlot();

      if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.anchorPos = pos;
         this.anchorHit = bhr;
         int charges = state.get(RespawnAnchorBlock.CHARGES);
         this.chargesPlaced = charges;
         this.state = charges < 4 && this.glowstoneSlot != -1 ? State.CHARGE : State.DETONATE;
      } else {
         if (!this.place.getValue() || this.anchorSlot == -1) return;
         this.anchorPos = pos.up();
         this.anchorHit = new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
         this.state = State.PLACE;
      }
      swapping = true;
      this.timer.a();
   }

   private void placeAnchor() {
      if (!this.timer.a(randomDelay(this.placeMs))) return;
      if (this.anchorSlot == -1) { resetState(); return; }
      switchTo(this.anchorSlot);
      if (this.silentAim.getValue()) aimAt(Vec3d.ofCenter(this.anchorHit.getBlockPos()));
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.anchorHit);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.chargesPlaced = 0;
      this.state = this.glowstoneSlot != -1 ? State.CHARGE : State.DETONATE;
      this.timer.a();
   }

   private void chargeAnchor() {
      if (!this.timer.a(randomDelay(this.actionMs))) return;
      if (this.chargesPlaced >= 4 || this.glowstoneSlot == -1) {
         this.state = this.protect.getValue() && this.shieldSlot != -1 ? State.PROTECT : State.DETONATE;
         this.timer.a(); return;
      }
      switchTo(this.glowstoneSlot);
      var hit = new BlockHitResult(Vec3d.ofCenter(this.anchorPos), Direction.UP, this.anchorPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.chargesPlaced++;
      this.glowstoneSlot = findSlot(Items.GLOWSTONE);
      this.timer.a();
   }

   private void placeShield() {
      if (!this.timer.a(randomDelay(this.protectMs))) return;
      // place shield on the side the player is on (or diagonal)
      if (this.shieldSlot == -1) { this.state = State.DETONATE; return; }
      switchTo(this.shieldSlot);
      Direction dir = this.diagonalShield.getValue()
          ? bestDiagonalDir() : playerFacingDir();
      BlockPos shieldPos = this.anchorPos.offset(dir);
      BlockHitResult shr = new BlockHitResult(Vec3d.ofCenter(shieldPos), dir.getOpposite(), shieldPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, shr);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.state = State.DETONATE;
      this.timer.a();
   }

   private void detonate() {
      if (!this.doBreak.getValue()) { restoreSlot(); return; }
      if (!this.timer.a(randomDelay(this.actionMs))) return;
      // click anchor to detonate
      var hit = new BlockHitResult(Vec3d.ofCenter(this.anchorPos), Direction.UP, this.anchorPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.state = State.RESTORE;
      this.timer.a();
   }

   private void restoreSlot() {
      if (!this.timer.a(50)) return;
      if (this.savedSlot >= 0) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         Access.ensureHasSentCarriedItem(mc.interactionManager);
      }
      SilentAim.e();
      resetState();
   }

   private void switchTo(int slot) {
      mc.player.getInventory().setSelectedSlot(slot);
      Access.ensureHasSentCarriedItem(mc.interactionManager);
   }

   private void aimAt(Vec3d pos) {
      Vec3d delta = pos.subtract(mc.player.getEyePos());
      float yaw   = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
      float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
      SilentAim.a(yaw, pitch, SilentAim.a.COMBAT);
   }

   private Direction playerFacingDir() {
      float yaw = mc.player.getYaw();
      if (yaw > 135 || yaw < -135) return Direction.NORTH;
      if (yaw > 45)  return Direction.WEST;
      if (yaw > -45) return Direction.SOUTH;
      return Direction.EAST;
   }

   private Direction bestDiagonalDir() {
      // pick the horizontal direction facing the player
      Vec3d delta = mc.player.getEntityPos().subtract(Vec3d.ofCenter(this.anchorPos));
      if (Math.abs(delta.x) > Math.abs(delta.z)) return delta.x > 0.0 ? Direction.EAST : Direction.WEST;
      return delta.z > 0 ? Direction.SOUTH : Direction.NORTH;
   }

   private int findSlot(net.minecraft.item.Item item) {
      for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(item)) return i;
      return -1;
   }

   private long randomDelay(Setting<Vector2f> s) {
      Vector2f v = s.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      return lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }

   private void resetState() {
      swapping       = false;
      this.state     = State.IDLE;
      this.anchorPos = null;
      this.anchorHit = null;
      this.savedSlot = -1;
      this.anchorSlot = -1;
      this.glowstoneSlot = -1;
      this.chargesPlaced = 0;
   }

   private enum State { IDLE, PLACE, CHARGE, PROTECT, DETONATE, RESTORE }
}
