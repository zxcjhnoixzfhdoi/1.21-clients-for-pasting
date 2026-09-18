package dev.ambience.features.modules.player;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AnchorMacro;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.features.settings.Setting;
import dev.ambience.inject.Access;
import dev.ambience.util.RotationManager;
import dev.ambience.util.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.joml.Vector2f;

public class FastXP extends Module {
   private final Setting<Vector2f> delayMs;
   private final Stopwatch timer;
   private int cooldown;

   public FastXP() {
      super("FastXP", "Throws XP bottles quickly while holding right click.", Module.Category.PLAYER);
      this.delayMs = this.vec2f("DelayMs", 50.0F, 100.0F).setVec2TrackBounds(50.0F, 500.0F).setPage("General");
      this.timer = new Stopwatch();
   }

   @Override
   public String getDisplayInfo() {
      Vector2f v = this.delayMs.getValue();
      return String.format("%.0f-%.0fms", v.x(), v.y());
   }

   @Override
   public String getDebugState() {
      return this.cooldown > 0 ? "cooldown=" + this.cooldown : null;
   }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null || mc.currentScreen != null) return;
      if (AutoTotem.c(0) || AnchorMacro.a() || RotationManager.d()) return;

      if (this.cooldown > 0) {
         this.cooldown--;
      }

      Hand hand = findXpHand();
      if (hand == null) {
         this.timer.a();
         return;
      }

      if (!mc.options.useKey.isPressed()) {
         this.timer.a();
         return;
      }

      long delay = randomDelay();
      if (!this.timer.a(delay)) {
         Access.setRightClickDelay(mc, 0);
         return;
      }

      if (this.cooldown > 0 || mc.player.isUsingItem()) return;

      Access.setRightClickDelay(mc, 0);
      ClientPlayerInteractionManager im = mc.interactionManager;
      Access.ensureHasSentCarriedItem(im);
      RotationManager.b();
      im.interactItem(mc.player, hand);
      Access.setRightClickDelay(mc, 0);
      this.timer.a();
      this.cooldown = 1;
   }

   private Hand findXpHand() {
      ItemStack main = mc.player.getMainHandStack();
      if (main.isOf(Items.EXPERIENCE_BOTTLE)) return Hand.MAIN_HAND;
      ItemStack off = mc.player.getOffHandStack();
      if (off.isOf(Items.EXPERIENCE_BOTTLE)) return Hand.OFF_HAND;
      return null;
   }

   private long randomDelay() {
      Vector2f v = this.delayMs.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      if (lo >= hi) return lo;
      return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
   }
}
