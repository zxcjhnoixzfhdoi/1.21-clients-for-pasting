package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.movement.PearlCatch;
import dev.ambience.features.modules.movement.WindHop;
import dev.ambience.features.settings.Bind;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoMace extends Module {
   private static AutoMace INSTANCE;
   // gravity constants for fall-distance simulation
   private static final double GRAVITY = 0.08;
   private static final double DRAG_Y  = 0.98;
   private static final long   POST_HIT_MS = 400L;

   private final Setting<Double>  minFallDist;
   private final Setting<Double>  horizTargetDist;
   private final Setting<Boolean> trigger;
   private final Setting<Boolean> silentAim;
   private final Setting<Double>  maxYaw;
   private final Setting<Boolean> switchToMace;
   private final Setting<Boolean> restoreSlot;
   private final Setting<Boolean> breachCheck;
   private final Setting<Boolean> takeoffElytra;
   private final Setting<Boolean> expandHitbox;
   private final Setting<Double>  hitboxExpansion;
   private final Setting<StunMode> stunSlamMode;
   private final Setting<Double>  stunChance;
   private final Setting<Boolean> scaleWithFall;
   private final Setting<Double>  stunMinFall;
   private final Setting<Integer> axeHoldDelay;
   private final Setting<Bind>    stunBind;

   private State      state;
   private LivingEntity target;
   private boolean    wasFalling;
   private int        savedSlot;
   private int        maceSlot;
   private int        axeSlot;
   private int        axeHoldTick;
   private final Stopwatch timer;

   public AutoMace() {
      super("AutoMace", "Fall smash: look at a player, swap mace, hit. Optional axe stun first.",
            Module.Category.COMBAT);
      this.minFallDist    = this.num("MinFallDistance",   1.5, 1.5, 64.0).setPage("General");
      this.horizTargetDist = this.num("HorizontalTargetDist", 5.0, 1.0, 16.0).setPage("General");
      this.trigger        = this.bool("Trigger",           true).setPage("General");
      this.silentAim      = this.bool("SilentAim",         true).setPage("General");
      this.maxYaw         = this.num("MaxYaw",            180.0, 10.0, 180.0)
                               .setVisibility(v -> this.silentAim.getValue()).setPage("General");
      this.switchToMace   = this.bool("Switch",            true).setPage("General");
      this.restoreSlot    = this.bool("RestoreSlot",       true).setPage("General");
      this.breachCheck    = this.bool("BreachCheck",      false).setPage("General");
      this.takeoffElytra  = this.bool("TakeoffElytra",   false).setPage("General");
      this.expandHitbox   = this.bool("ExpandHitbox",    false).setPage("General");
      this.hitboxExpansion = this.num("HitboxExpansion",  0.5, 0.0, 2.0)
                               .setVisibility(v -> this.expandHitbox.getValue()).setPage("General");
      this.stunSlamMode   = this.mode("StunSlamMode",     StunMode.Chance).setPage("Stun Slam");
      this.stunChance     = this.num("Chance",           100.0, 0.0, 100.0)
                               .setVisibility(v -> this.stunSlamMode.getValue() == StunMode.Chance).setPage("Stun Slam");
      this.scaleWithFall  = this.bool("ScaleWithFall",   false).setPage("Stun Slam");
      this.stunMinFall    = this.num("StunMinFall",       0.0, 0.0, 64.0).setPage("Stun Slam");
      this.axeHoldDelay   = this.num("AxeHoldDelay",      0, 0, 10).setPage("Stun Slam");
      this.stunBind       = this.key("StunBind", Bind.none()).setPage("Stun Slam");
      this.state = State.IDLE;
      this.savedSlot = -1;
      this.maceSlot  = -1;
      this.axeSlot   = -1;
      this.timer     = new Stopwatch();
      INSTANCE = this;
   }

   public static AutoMace get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.state != State.IDLE; }

   @Override public String getDisplayInfo() { return this.state != State.IDLE ? this.state.name() : (this.target != null ? this.target.getName().getString() : null); }
   @Override public void   onDisable()      { resetState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) return;
      if (WindHop.a(0) || PearlCatch.a(0)) return;
      if (mc.currentScreen != null || AutoTotem.c(0) || AnchorMacro.a()) return;

      ClientPlayerEntity player = mc.player;

      switch (this.state) {
         case IDLE -> {
            if (player.isOnGround() || player.getVelocity().y >= 0) { this.wasFalling = false; return; }
            double fallVel = Math.abs(player.getVelocity().y);
            if (!this.wasFalling && fallVel < 0.08) return;
            this.wasFalling = true;

            // find target in range
            LivingEntity t = findTarget(player);
            if (t == null) return;

            double fallDist = estimateFallDist(player);
            if (fallDist < this.minFallDist.getValue()) return;
            this.target = t;

            // check elytra takeoff
            if (this.takeoffElytra.getValue() && player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && player.isGliding()) {
               player.stopGliding();
            }

            // find mace
            this.maceSlot = findMace();
            if (this.maceSlot == -1) return;
            this.savedSlot = player.getInventory().getSelectedSlot();

            // try stun slam (axe first)
            if (shouldStun(fallDist)) {
               this.axeSlot = findAxe();
               if (this.axeSlot != -1) {
                  if (this.switchToMace.getValue()) InventoryUtil.a(player, this.axeSlot);
                  this.axeHoldTick = this.axeHoldDelay.getValue();
                  this.state = State.STUN_AXE; return;
               }
            }

            beginMaceHit(player);
         }
         case STUN_AXE -> {
            if (this.axeHoldTick-- > 0) return;
            // attack with axe then switch to mace
            mc.interactionManager.attackEntity(player, this.target);
            player.swingHand(Hand.MAIN_HAND);
            beginMaceHit(player);
         }
         case SWING -> {
            if (!this.timer.a(POST_HIT_MS)) return;
            // aim at target
            if (this.silentAim.getValue() && this.target != null) {
               Vec3d delta = this.target.getEyePos().subtract(player.getEyePos());
               float yaw   = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
               float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
               float yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - player.getYaw()));
               if (yawDiff <= this.maxYaw.getValue()) {
                  SilentAim.a(yaw, pitch, SilentAim.a.COMBAT);
               }
            }
            // hit
            if (this.target != null && this.target.isAlive()) {
               mc.interactionManager.attackEntity(player, this.target);
               player.swingHand(Hand.MAIN_HAND);
            }
            SilentAim.e();
            this.state = State.RESTORE;
         }
         case RESTORE -> {
            if (this.restoreSlot.getValue() && this.savedSlot >= 0) {
               player.getInventory().setSelectedSlot(this.savedSlot);
               dev.ambience.inject.Access.ensureHasSentCarriedItem(mc.interactionManager);
            }
            resetState();
         }
      }
   }

   private void beginMaceHit(ClientPlayerEntity player) {
      if (this.switchToMace.getValue()) InventoryUtil.a(player, this.maceSlot);
      this.timer.a();
      this.state = State.SWING;
   }

   private boolean shouldStun(double fallDist) {
      if (this.stunSlamMode.getValue() == StunMode.OFF) return false;
      if (fallDist < this.stunMinFall.getValue()) return false;
      if (this.stunBind.getValue().isDown()) return true;
      if (this.stunSlamMode.getValue() == StunMode.Chance) {
         return ThreadLocalRandom.current().nextDouble(100.0) < this.stunChance.getValue();
      }
      return this.stunSlamMode.getValue() == StunMode.Always;
   }

   private double estimateFallDist(ClientPlayerEntity player) {
      double v = player.getVelocity().y, y = player.getY(); int t = 0;
      while (t++ < 80 && y > 0) {
         v = (v - GRAVITY) * DRAG_Y; y += v;
         if (mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(player.getX(), y - 0.01, player.getZ())).isSolidBlock(mc.world, net.minecraft.util.math.BlockPos.ofFloored(player.getX(), y, player.getZ()))) break;
      }
      return player.getY() - y;
   }

   private LivingEntity findTarget(ClientPlayerEntity player) {
      if (mc.world == null) return null;
      double r = this.horizTargetDist.getValue();
      double expand = this.expandHitbox.getValue() ? this.hitboxExpansion.getValue() : 0.0;
      Vec3d pos = player.getEntityPos();
      LivingEntity best = null; double bestDist = Double.MAX_VALUE;
      for (Entity e : mc.world.getEntities()) {
         if (!(e instanceof LivingEntity le) || le == player || !le.isAlive() || le.isSpectator() || le instanceof ArmorStandEntity) continue;
         double hDist = Math.sqrt(Math.pow(e.getX()-(pos.x),2)+Math.pow(e.getZ()-(pos.z),2));
         if (hDist > r) continue;
         Box box = le.getBoundingBox().expand(expand);
         if (this.silentAim.getValue()) {
            float yaw = (float) Math.toDegrees(Math.atan2(-(e.getX()-player.getX()), e.getZ()-player.getZ()));
            if (Math.abs(MathHelper.wrapDegrees(yaw-player.getYaw())) > this.maxYaw.getValue()) continue;
         }
         if (hDist < bestDist) { bestDist = hDist; best = le; }
      }
      return best;
   }

   private int findMace() {
      for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
      return -1;
   }

   private int findAxe() {
      for (int i = 0; i < 9; i++) { var s = mc.player.getInventory().getStack(i); if (s.isIn(ItemTags.AXES)) return i; }
      return -1;
   }

   private void resetState() {
      SilentAim.e();
      this.state    = State.IDLE;
      this.target   = null;
      this.savedSlot = -1;
      this.maceSlot  = -1;
      this.axeSlot   = -1;
      this.wasFalling = false;
   }

   private enum State { IDLE, STUN_AXE, SWING, RESTORE }
   public enum StunMode { OFF, Chance, Always, Bind }
}
