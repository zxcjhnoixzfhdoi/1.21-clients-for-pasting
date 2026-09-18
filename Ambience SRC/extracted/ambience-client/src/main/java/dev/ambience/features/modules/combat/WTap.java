package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.render.Freecam;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.WTapHook;
import dev.ambience.inject.Access;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector2f;

public class WTap extends Module {
   private static WTap INSTANCE;
   private final Setting<Vector2f> stopMs;
   private final Setting<Boolean>  strafe;
   private final Setting<Boolean>  weaponsOnly;
   private final Setting<Boolean>  players;
   private final Setting<Boolean>  hostiles;
   private final Setting<Boolean>  passives;
   private boolean attacking;
   private long stopUntil;

   public WTap() {
      super("WTap", "Stops walking after a sword hit so combos stay at range.", Module.Category.COMBAT);
      this.stopMs      = this.vec2f("StopMs",     325f, 400f).setVec2TrackBounds(0f, 4000f).setPage("General");
      this.strafe      = this.bool("Strafe",      true).setPage("General");
      this.weaponsOnly = this.bool("WeaponsOnly", true).setPage("General");
      this.players     = this.bool("Players",     true).setPage("Targets");
      this.hostiles    = this.bool("Hostiles",   false).setPage("Targets");
      this.passives    = this.bool("Passives",   false).setPage("Targets");
      INSTANCE = this;

      WTapHook.bind(new WTapHook.Impl() {
         @Override public void beforeAttack(PlayerEntity attacker, Entity target) { WTap.onBeforeAttack(attacker, target); }
         @Override public void afterAttack()  { WTap.onAfterAttack(); }
         @Override public void freezeInput()  { WTap.onFreezeInput(); }
      });
   }

   public static WTap get() { return INSTANCE; }

   // is stop timer active?
   public static boolean a(int unused) {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.stopUntil > System.currentTimeMillis();
   }

   public static void onBeforeAttack(PlayerEntity attacker, Entity target) {
      if (INSTANCE == null) return;
      INSTANCE.checkBeforeAttack(attacker, target);
   }

   public static void onAfterAttack() {
      if (INSTANCE == null) return;
      INSTANCE.armStopTimer();
   }

   public static void onFreezeInput() {
      if (!a(0) || AutoTotem.c(0) || Freecam.a(0)) return;
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || player.input == null) return;
      INSTANCE.freezePlayerInput(player);
   }

   @Override public String getDisplayInfo() {
      if (!a(0)) {
         Vector2f v = this.stopMs.getValue();
         int lo = Math.round(v.x()), hi = Math.round(v.y());
         return lo == hi ? lo + "ms" : String.format("%.0f-%.0fms", v.x(), v.y());
      }
      return Math.max(0L, this.stopUntil - System.currentTimeMillis()) + "ms";
   }

   @Override public String getDebugState() { return a(0) ? "stopping" : null; }

   @Override public void onDisable() { this.attacking = false; this.stopUntil = 0L; }

   @Override
   public void onPreTick() {
      if (!a(0)) {
         this.stopUntil = 0L;
      }
      onFreezeInput();
   }

   private void checkBeforeAttack(PlayerEntity attacker, Entity target) {
      this.attacking = false;
      if (!this.isEnabled()) return;
      if (attacker != mc.player || !nullCheck()) return;
      if (Freecam.a(0) || AutoTotem.c(0) || AnchorMacro.a()) return;
      if (this.weaponsOnly.getValue() && !isWeapon(attacker)) return;
      if (!isValidTarget(target)) return;
      this.attacking = true;
   }

   private void armStopTimer() {
      if (!this.attacking) return;
      this.attacking = false;
      Vector2f v = this.stopMs.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      long delay = (lo >= hi) ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
      this.stopUntil = System.currentTimeMillis() + delay;
   }

   private void freezePlayerInput(ClientPlayerEntity player) {
      // zero forward/back; keep strafe if setting allows
      Access.setMoveVector(player.input, Vec2f.ZERO);
   }

   private boolean isWeapon(PlayerEntity player) {
      var stack = player.getMainHandStack();
      return stack.isIn(ItemTags.SWORDS)  || stack.isIn(ItemTags.AXES)
          || stack.isOf(Items.MACE)       || stack.isOf(Items.TRIDENT);
   }

   private boolean isValidTarget(Entity target) {
      if (!(target instanceof LivingEntity) || target instanceof ArmorStandEntity) return false;
      if (target instanceof PlayerEntity)                                           return this.players.getValue();
      if (target instanceof Monster)                                                return this.hostiles.getValue();
      return this.passives.getValue();
   }
}
