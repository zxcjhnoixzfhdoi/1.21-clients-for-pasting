package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.inject.Access;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
   private static Freecam INSTANCE;
   private static final PlayerInput ZERO_INPUT = new PlayerInput(false,false,false,false,false,false,false);
   private final Setting<Double> speed;
   // camera current pos (client) + previous pos (for interpolation)
   private double camX, camY, camZ;
   private double prevX, prevY, prevZ;
   private float yaw, pitch;
   private boolean active;

   public Freecam() {
      super("Freecam", "Detach the camera and fly around. Your player stays put.", Module.Category.VISUALS);
      this.speed = this.num("Speed", 1.0, 0.1, 5.0).setPage("General");
      INSTANCE = this;
      FreecamHook.bind(new FreecamHook.Impl() {
         @Override public boolean isActive()                     { return Freecam.a(0); }
         @Override public boolean blocksInteraction()            { return Freecam.a(0); }
         @Override public float   getYaw()                       { return Freecam.c(0); }
         @Override public float   getPitch()                     { return Freecam.d(0); }
         @Override public Vec3d   getCameraPos(float partials)   { return Freecam.a(partials, 0); }
         @Override public void    onTurn(double dx, double dy)   { Freecam.a(dx, dy, 0); }
         @Override public void    freezeInput()                  { Freecam.e(0); }
      });
   }

   public static Freecam get() { return INSTANCE; }
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.active; }
   public static boolean b(int unused) { return a(0); }
   public static float   c(int unused) { return INSTANCE != null ? INSTANCE.yaw   : 0f; }
   public static float   d(int unused) { return INSTANCE != null ? INSTANCE.pitch : 0f; }

   public static Vec3d a(float partials, int unused) {
      if (INSTANCE == null) return Vec3d.ZERO;
      return new Vec3d(
         MathHelper.lerp(partials, INSTANCE.prevX, INSTANCE.camX),
         MathHelper.lerp(partials, INSTANCE.prevY, INSTANCE.camY),
         MathHelper.lerp(partials, INSTANCE.prevZ, INSTANCE.camZ)
      );
   }

   public static void a(double dx, double dy, int unused) {
      if (!a(0)) return;
      INSTANCE.yaw   = MathHelper.wrapDegrees(INSTANCE.yaw   + (float) dx * 0.15F);
      INSTANCE.pitch = MathHelper.clamp(INSTANCE.pitch + (float) dy * 0.15F, -90f, 90f);
   }

   public static void e(int unused) {
      if (!a(0)) return;
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || player.input == null) return;
      player.input.playerInput = ZERO_INPUT;
      Access.setMoveVector(player.input, Vec2f.ZERO);
   }

   @Override public String getDisplayInfo()  { return String.format("%.1fx", this.speed.getValue()); }
   @Override public String getDebugState()   { return this.active ? String.format("%.1f/%.1f/%.1f", this.camX, this.camY, this.camZ) : null; }

   @Override
   public void onEnable() {
      if (!nullCheck()) { this.active = false; return; }
      Vec3d eye = mc.player.getEyePos();
      this.camX = this.prevX = eye.x;
      this.camY = this.prevY = eye.y;
      this.camZ = this.prevZ = eye.z;
      // inherit look direction from Freelook/SilentAim if active, else player
      Freelook fl = Freelook.get();
      if (fl != null && fl.isEnabled()) {
         this.yaw = Freelook.getYaw(); this.pitch = Freelook.getPitch();
      } else if (SilentAim.a()) {
         this.yaw = SilentAim.c(); this.pitch = SilentAim.d();
      } else {
         this.yaw = mc.player.getYaw(); this.pitch = mc.player.getPitch();
      }
      this.active = true;
   }

   @Override
   public void onDisable() { this.active = false; }

   @Override
   public void onPreTick() {
      if (!nullCheck()) { this.disable(); return; }
      // save prev pos for interpolation
      this.prevX = this.camX; this.prevY = this.camY; this.prevZ = this.camZ;
      if (mc.currentScreen != null) return;

      float fwd   = axis(mc.options.forwardKey.isPressed(), mc.options.backKey.isPressed());
      float right = axis(mc.options.leftKey.isPressed(),    mc.options.rightKey.isPressed());
      float up    = axis(mc.options.jumpKey.isPressed(),     mc.options.sneakKey.isPressed());
      if (fwd == 0 && right == 0 && up == 0) return;

      double spd = this.speed.getValue() * 0.5;
      if (mc.options.sprintKey.isPressed()) spd *= 2.0;

      Vec3d look = Vec3d.fromPolar(this.pitch, this.yaw);
      Vec3d side = Vec3d.fromPolar(0f, this.yaw - 90f);
      this.camX += (look.x * fwd + side.x * right) * spd;
      this.camY += (look.y * fwd + up) * spd;
      this.camZ += (look.z * fwd + side.z * right) * spd;
   }

   private static float axis(boolean pos, boolean neg) {
      return pos == neg ? 0f : pos ? 1f : -1f;
   }
}
