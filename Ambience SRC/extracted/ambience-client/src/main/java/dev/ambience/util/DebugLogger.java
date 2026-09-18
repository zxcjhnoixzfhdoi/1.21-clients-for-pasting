package dev.ambience.util;

import dev.ambience.Ambience;
import dev.ambience.features.Feature;
import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.client.DebugModule;
import dev.ambience.inject.Access;
import dev.ambience.util.traits.Util;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.StackWalker.Option;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class DebugLogger implements Util {
   private static final DateTimeFormatter a = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
   private static final int b = 160;
   private static final StackWalker c = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
   private static final Deque<String> d = new ArrayDeque<>();
   private static final Map<String, String> e = new LinkedHashMap<>();
   private static BufferedWriter f;
   private static Path g;
   private static boolean h;
   private static int i;
   private static float j;
   private static float k;
   private static boolean l;
   private static int m;
   private static int n;

   private DebugLogger() {
   }

   public static boolean a() {
      DebugModule var0 = DebugModule.getInstance();
      return var0 != null && var0.isEnabled();
   }

   public static void b() {
      r();
      i = 0;
      m = 0;
      l = false;
      d.clear();
      e.clear();
      if (mc.runDirectory != null) {
         Path var0 = mc.runDirectory.toPath().resolve("logs");
         g = var0.resolve("ambience-debug.log");

         try {
            Files.createDirectories(var0);
            f = Files.newBufferedWriter(g, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            f("===== Ambience debug session " + LocalDateTime.now() + " =====");
            f("file=" + g.toAbsolutePath());
            q();
         } catch (IOException var2) {
            Ambience.a.error("Failed to open ambience-debug.log", var2);
            f = null;
         }
      }
   }

   public static void c() {
      f("===== debug session end flags=" + m + " =====");
      r();
   }

   public static Path d() {
      return g;
   }

   public static void e() {
      if (a() && mc.player != null) {
         i++;
         if (DebugModule.getInstance().logTicks.getValue()) {
            b("TICK", "pre", h());
         }

         g();
      }
   }

   public static void f() {
      if (a() && mc.player != null) {
         ClientPlayerEntity var0 = mc.player;
         j = var0.getYaw();
         k = var0.getPitch();
         l = true;
         if (DebugModule.getInstance().logFlying.getValue()) {
            b("FLYING", "sendPosition", h());
         }
      }
   }

   public static void a(float var0, float var1, float var2, float var3) {
      if (a() && DebugModule.getInstance().logLooks.getValue()) {
         if (var0 != var2 || var1 != var3) {
            b(
               "LOOK",
               n(),
               String.format(
                  "fromYaw=%.10f fromPitch=%.10f toYaw=%.10f toPitch=%.10f dYaw=%.10f dPitch=%.10f %s",
                  var0,
                  var1,
                  var2,
                  var3,
                  a(var2 - var0),
                  var3 - var1,
                  h()
               )
            );
         }
      }
   }

   public static void a(String var0, String var1) {
      if (a()) {
         b("SILENT", var0, n() + " " + var1 + " " + h());
      }
   }

   public static void b(String var0, String var1) {
      if (a()) {
         b("ACTION", n(), var0 + " " + var1 + " " + h());
      }
   }

   public static void c(String var0, String var1) {
      if (a()) {
         b("INTERACT", var0, var1 + " vsLastFlying=" + i() + " " + h());
      }
   }

   public static void a(int var0, int var1, String var2) {
      if (a()) {
         b("SLOT", n(), "from=" + var0 + " to=" + var1 + " item=" + var2 + " " + h());
      }
   }

   public static void d(String var0, String var1) {
      if (a()) {
         b("OMNI", var0, var1 + " " + h());
      }
   }

   public static void a(String var0, String var1, String var2) {
      if (a()) {
         b("MODULE", var0, var1 + (var2 != null && !var2.isEmpty() ? " " + var2 : ""));
      }
   }

   public static void a(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = var0.replace('\n', ' ').trim();
         if (b(var1)) {
            if (a()) {
               m++;
               String var2 = c(var1);
               f(var2);
               q();
               DebugModule var3 = DebugModule.getInstance();
               if (var3 != null && var3.chatNotify.getValue() && mc.player != null) {
                  mc.player.sendMessage(Text.literal("Ambience debug captured flag #" + m + " → logs/ambience-debug.log"), false);
               }
            }
         }
      }
   }

   private static boolean b(String var0) {
      String var1 = var0.toLowerCase();
      return !var1.contains("grim") ? false : var1.contains("failed") || var1.contains("flag") || var1.contains("vl");
   }

   private static String c(String var0) {
      StringBuilder var1 = new StringBuilder(4096);
      var1.append('\n');
      var1.append("########## GRIM FLAG #").append(m).append(" ##########\n");
      var1.append(o()).append(" FLAG ").append(var0).append('\n');
      var1.append("now ").append(h()).append('\n');
      var1.append("--- last ").append(d.size()).append(" events ---\n");

      for (String var3 : d) {
         var1.append(var3).append('\n');
      }

      var1.append("########## END FLAG #").append(m).append(" ##########\n");
      return var1.toString();
   }

   private static void g() {
      if (Ambience.e != null) {
         for (Module var1 : Ambience.e.getModules()) {
            if (!(var1 instanceof DebugModule)) {
               String var2 = var1.getName();
               String var3 = var1.isEnabled() ? var1.getDebugState() : "off";
               if (var3 == null) {
                  var3 = var1.isEnabled() ? "on" : "off";
               }

               String var4 = e.put(var2, var3);
               if (var4 != null && !var4.equals(var3)) {
                  b("STATE", var2, var4 + " -> " + var3 + " " + h());
               }
            }
         }
      }
   }

   private static void b(String var0, String var1, String var2) {
      if (!h) {
         h = true;

         try {
            String var3 = o() + " t=" + i + " " + var0 + " [" + var1 + "] " + var2;
            e(var3);
            f(var3);
         } finally {
            h = false;
         }
      }
   }

   private static String h() {
      ClientPlayerEntity var0 = mc.player;
      if (var0 == null) {
         return "player=null";
      }

      Vec3d var1 = var0.getEntityPos();
      Vec3d var2 = var0.getVelocity();
      float var3 = var0.getYaw();
      float var4 = var0.getPitch();
      StringBuilder var5 = new StringBuilder(512);
      var5.append(
         String.format(
            "pos=%.10f,%.10f,%.10f vel=%.10f,%.10f,%.10f yaw=%.10f pitch=%.10f yawO=%.10f pitchO=%.10f",
            var1.x,
            var1.y,
            var1.z,
            var2.x,
            var2.y,
            var2.z,
            var3,
            var4,
            var0.lastYaw,
            var0.lastPitch
         )
      );
      var5.append(" onGround=").append(var0.isOnGround());
      var5.append(" fallFly=").append(var0.isGliding());
      var5.append(" fall=").append(String.format("%.10f", var0.fallDistance));
      var5.append(" sprint=").append(var0.isSprinting());
      var5.append(" sneak=").append(var0.isSneaking());
      var5.append(" using=").append(var0.isUsingItem());
      var5.append(" hurt=").append(var0.hurtTime);
      var5.append(" slot=").append(var0.getInventory().getSelectedSlot());
      var5.append(" main=").append(a(var0.getMainHandStack()));
      var5.append(" off=").append(a(var0.getOffHandStack()));
      var5.append(" silent=").append(SilentAim.a() ? SilentAim.b() : "NONE");
      if (SilentAim.a()) {
         var5.append(String.format("(camYaw=%.10f camPitch=%.10f)", SilentAim.c(), SilentAim.d()));
      }

      var5.append(" actionTick=").append(RotationManager.d());
      var5.append(" keys=").append(j());
      if (var0.input != null) {
         var5.append(String.format(" move=%.4f,%.4f", Access.moveVector(var0.input).x, Access.moveVector(var0.input).y));
      }

      var5.append(" hit=").append(k());
      var5.append(" vsLastFlying=").append(i());
      var5.append(" busy=").append(l());
      var5.append(" enabled=").append(m());
      return var5.toString();
   }

   private static String i() {
      return l && mc.player != null
         ? String.format("dYaw=%.10f dPitch=%.10f lastYaw=%.10f lastPitch=%.10f", a(mc.player.getYaw() - j), mc.player.getPitch() - k, j, k)
         : "none";
   }

   private static String j() {
      if (mc.options == null) {
         return "-";
      }

      StringBuilder var0 = new StringBuilder();
      if (mc.options.forwardKey.isPressed()) {
         var0.append('W');
      }

      if (mc.options.leftKey.isPressed()) {
         var0.append('A');
      }

      if (mc.options.backKey.isPressed()) {
         var0.append('S');
      }

      if (mc.options.rightKey.isPressed()) {
         var0.append('D');
      }

      if (mc.options.jumpKey.isPressed()) {
         var0.append('J');
      }

      if (mc.options.sneakKey.isPressed()) {
         var0.append('_');
      }

      if (mc.options.sprintKey.isPressed()) {
         var0.append('P');
      }

      if (mc.options.useKey.isPressed()) {
         var0.append('R');
      }

      if (mc.options.attackKey.isPressed()) {
         var0.append('L');
      }

      if (mc.player != null && mc.player.input != null) {
         PlayerInput var1 = mc.player.input.playerInput;
         var0.append("/pkt=");
         if (var1.forward()) {
            var0.append('W');
         }

         if (var1.left()) {
            var0.append('A');
         }

         if (var1.backward()) {
            var0.append('S');
         }

         if (var1.right()) {
            var0.append('D');
         }

         if (var1.jump()) {
            var0.append('J');
         }
      }

      return var0.isEmpty() ? "-" : var0.toString();
   }

   private static String k() {
      HitResult var0 = mc.crosshairTarget;
      if (var0 == null) {
         return "null";
      } else if (var0 instanceof BlockHitResult var1 && var1.getType() == Type.BLOCK) {
         return "block:"
            + var1.getBlockPos().toShortString()
            + "/"
            + var1.getSide()
            + String.format("@%.10f,%.10f,%.10f", var1.getPos().x, var1.getPos().y, var1.getPos().z);
      } else {
         return var0 instanceof EntityHitResult var2
            ? "entity:" + var2.getEntity().getName().getString() + "#" + var2.getEntity().getId()
            : var0.getType().name();
      }
   }

   private static String l() {
      if (Ambience.e == null) {
         return "-";
      }

      String var0 = Ambience.e.getModules().stream().filter(Module::isEnabled).filter(var0x -> !(var0x instanceof DebugModule)).map(var0x -> {
         String var1 = var0x.getDebugState();
         return var1 == null ? null : var0x.getName() + "=" + var1;
      }).filter(var0x -> var0x != null).collect(Collectors.joining(","));
      return var0.isEmpty() ? "-" : var0;
   }

   private static String m() {
      if (Ambience.e == null) {
         return "-";
      }

      String var0 = Ambience.e
         .getModules()
         .stream()
         .filter(Module::isEnabled)
         .filter(
            var0x -> var0x.getCategory() != Module.Category.RENDER
               && var0x.getCategory() != Module.Category.VISUALS
               && !(var0x instanceof DebugModule)
               && !"ClickGui".equals(var0x.getName())
         )
         .map(Feature::getName)
         .collect(Collectors.joining(","));
      return var0.isEmpty() ? "-" : var0;
   }

   private static String a(ItemStack var0) {
      return var0 != null && !var0.isEmpty() ? var0.getItem().toString() + "*" + var0.getCount() : "empty";
   }

   private static String n() {
      try {
         return c.walk(var0 -> var0.filter(var0x -> {
            String var1x = var0x.getClassName();
            return var1x.startsWith("dev.ambience") && !var1x.contains("ModuleDebug");
         }).map(var0x -> d(var0x.getClassName()) + "." + var0x.getMethodName()).limit(3L).collect(Collectors.joining(" <- ")));
      } catch (Throwable var1) {
         return "unknown";
      }
   }

   private static String d(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 < 0 ? var0 : var0.substring(var1 + 1);
   }

   private static float a(float var0) {
      return MathHelper.wrapDegrees(var0);
   }

   private static String o() {
      return a.format(LocalDateTime.now());
   }

   private static int p() {
      DebugModule var0 = DebugModule.getInstance();
      return var0 == null ? 160 : var0.history.getValue();
   }

   private static void e(String var0) {
      d.addLast(var0);
      int var1 = p();

      while (d.size() > var1) {
         d.removeFirst();
      }
   }

   private static void f(String var0) {
      if (f != null) {
         try {
            f.write(var0);
            f.newLine();
            if (++n % 25 == 0) {
               f.flush();
            }
         } catch (IOException var2) {
            Ambience.a.error("Failed to write ambience-debug.log", var2);
         }
      }
   }

   private static void q() {
      if (f != null) {
         try {
            f.flush();
         } catch (IOException var1) {
         }
      }
   }

   private static void r() {
      if (f != null) {
         try {
            f.flush();
            f.close();
         } catch (IOException var1) {
         }

         f = null;
      }
   }
}
