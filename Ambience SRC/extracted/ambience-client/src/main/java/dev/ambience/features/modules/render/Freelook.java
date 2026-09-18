package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.FreelookHook;
import net.minecraft.util.math.MathHelper;

public class Freelook extends Module {
   private static Freelook INSTANCE;
   private float yaw;
   private float pitch;
   private boolean active;

   public Freelook() {
      super("Freelook", "Look around without turning your player.", Module.Category.VISUALS);
      INSTANCE = this;
      FreelookHook.bind(new FreelookHook.Impl() {
         @Override public boolean isActive()    { return Freelook.isActive(); }
         @Override public float   getYaw()      { return Freelook.getYaw(); }
         @Override public float   getPitch()    { return Freelook.getPitch(); }
         @Override public void    onTurn(double dx, double dy) { Freelook.onTurn(dx, dy); }
         @Override public void    absorbCamera(float y, float p) { Freelook.absorbCamera(y, p); }
      });
   }

   public static Freelook get() { return INSTANCE; }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.active && !Freecam.a(0);
   }

   public static float getYaw()   { return INSTANCE != null ? INSTANCE.yaw   : 0f; }
   public static float getPitch() { return INSTANCE != null ? INSTANCE.pitch : 0f; }

   public static void onTurn(double dx, double dy) {
      if (!isActive()) return;
      INSTANCE.yaw   = MathHelper.wrapDegrees(INSTANCE.yaw   + (float) dx * 0.15F);
      INSTANCE.pitch = MathHelper.clamp(INSTANCE.pitch + (float) dy * 0.15F, -90f, 90f);
   }

   public static void absorbCamera(float y, float p) {
      Freelook inst = INSTANCE;
      if (inst == null || !inst.isEnabled() || !inst.active) return;
      inst.yaw   = MathHelper.wrapDegrees(y);
      inst.pitch = MathHelper.clamp(p, -90f, 90f);
   }

   @Override
   public String getDebugState() {
      return this.active ? String.format("%.1f/%.1f", this.yaw, this.pitch) : null;
   }

   @Override
   public void onEnable() {
      initCamera();
   }

   @Override
   public void onDisable() {
      this.active = false;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.disable();
      }
   }

   private void initCamera() {
      if (!nullCheck()) {
         this.active = false;
         return;
      }
      if (Freecam.a(0)) {
         this.yaw   = Freecam.c(0);
         this.pitch = Freecam.d(0);
      } else {
         this.yaw   = mc.player.getYaw();
         this.pitch = mc.player.getPitch();
      }
      this.active = true;
   }
}
