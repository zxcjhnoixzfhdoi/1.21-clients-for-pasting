package dev.ambience.features.modules.movement;
import dev.ambience.features.modules.combat.STap;
import dev.ambience.features.modules.combat.WTap;
import dev.ambience.features.modules.combat.BreachSwap;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.render.Freelook;
import dev.ambience.hooks.OmniSprintHook;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class OmniSprint extends Module {
   private static OmniSprint INSTANCE;

   public OmniSprint() {
      super("OmniSprint", "Silent-aims your body toward WASD so sprinting is always forward.",
            Module.Category.MOVEMENT);
      INSTANCE = this;
      OmniSprintHook.bind(OmniSprint::tick);
   }

   public static OmniSprint get() { return INSTANCE; }
   public static boolean b(int unused) { return INSTANCE != null && INSTANCE.isEnabled(); }

   @Override public String getDebugState() {
      return SilentAim.a(SilentAim.a.OMNI) ? "active" : null;
   }

   public static void tick(ClientPlayerEntity player) {
      if (player == null || player.input == null) return;
      OmniSprint inst = get();
      if (inst == null || !inst.isEnabled()) return;
      if (mc.currentScreen != null) return;
      if (!canApply(player)) return;

      boolean fwd   = mc.options.forwardKey.isPressed();
      boolean back  = mc.options.backKey.isPressed();
      boolean left  = mc.options.leftKey.isPressed();
      boolean right = mc.options.rightKey.isPressed();

      float moveX = axis(fwd,  back);
      float moveZ = axis(left, right);

      if (Math.abs(moveX) < 1e-4f && Math.abs(moveZ) < 1e-4f) {
         SilentAim.b(SilentAim.a.OMNI);  // not moving — release OMNI
         return;
      }

      // base yaw — from SilentAim, Freelook, or player
      float baseYaw;
      if (SilentAim.a()) {
         baseYaw = SilentAim.c();
      } else if (Freelook.isActive()) {
         baseYaw = Freelook.getYaw();
      } else {
         baseYaw = player.getYaw();
      }

      float basePitch;
      if (SilentAim.a()) {
         basePitch = SilentAim.d();
      } else if (Freelook.isActive()) {
         basePitch = Freelook.getPitch();
      } else {
         basePitch = player.getPitch();
      }

      float targetYaw = computeTargetYaw(baseYaw, moveX, moveZ);
      SilentAim.a(targetYaw, basePitch, SilentAim.a.OMNI);

      // if OMNI silent aim was set, rotate body to face sprint direction
      if (!SilentAim.a(SilentAim.a.OMNI)) {
         applyBodyYaw(player, targetYaw);
      }
   }

   /** Compute yaw so that fwd/strafe inputs result in desired world direction. */
   private static float computeTargetYaw(float cameraYaw, float fwd, float strafe) {
      // angle of input vector (fwd=+Z, right=+X in game space)
      float inputAngle = (float) Math.toDegrees(Math.atan2(strafe, fwd));
      return MathHelper.wrapDegrees(cameraYaw + inputAngle);
   }

   private static void applyBodyYaw(ClientPlayerEntity player, float yaw) {
      player.setYaw(yaw);
      
   }

   private static boolean canApply(ClientPlayerEntity player) {
      if (player.getAbilities().flying) return false;
      return !WTap.a(0) && !STap.a(0) && !BreachSwap.a(0);
   }

   private static float axis(boolean pos, boolean neg) {
      return pos == neg ? 0f : pos ? 1f : -1f;
   }
}
