package dev.ambience.util.render;

import dev.ambience.util.traits.Util;
import java.awt.Color;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class EspDraw {
   private static final float TRACER_WIDTH = 2.5F;
   private static final double TRACER_NEAR = 0.15;
   private static Vec3d tracerOrigin;

   private EspDraw() {
   }

   public static void box(Box var0, Color var1, boolean var2, boolean var3) {
      if (var0 != null && var1 != null) {
         try {
            int var4 = var1.getRGB();
            VisibilityConfigurable var5 = var2
               ? GizmoDrawing.box(var0, DrawStyle.filledAndStroked(var4, 2.5F, fillArgb(var1)))
               : GizmoDrawing.box(var0, DrawStyle.stroked(var4));
            if (var3) {
               var5.ignoreOcclusion();
            }
         } catch (IllegalStateException var6) {
         }
      }
   }

   public static void tracer(Vec3d var0, Color var1, boolean var2) {
      if (var0 != null && var1 != null) {
         Vec3d var3 = tracerOrigin();

         try {
            VisibilityConfigurable var4 = GizmoDrawing.line(var3, var0, var1.getRGB(), 2.5F);
            if (var2) {
               var4.ignoreOcclusion();
            }
         } catch (IllegalStateException var5) {
         }
      }
   }

   public static void updateTracerOrigin(Matrix4fc var0, Matrix4fc var1) {
      if (var0 != null && var1 != null && Util.mc.gameRenderer != null) {
         Matrix4f var2 = new Matrix4f(var0).invert();
         Matrix4f var3 = new Matrix4f(var1).invert();
         Vector4f var4 = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
         var2.transform(var4);
         var3.transform(var4);
         if (var4.w != 0.0F) {
            var4.div(var4.w);
         }

         Vec3d var5 = Util.mc.gameRenderer.getCamera().getCameraPos();
         tracerOrigin = new Vec3d(var5.x + var4.x, var5.y + var4.y, var5.z + var4.z);
      } else {
         tracerOrigin = null;
      }
   }

   public static Vec3d tracerOrigin() {
      Camera var0 = Util.mc.gameRenderer.getCamera();
      Vec3d var1 = var0.getCameraPos();
      Vector3fc var2 = var0.getHorizontalPlane();
      Vec3d var3 = new Vec3d(var1.x + var2.x() * 0.15, var1.y + var2.y() * 0.15, var1.z + var2.z() * 0.15);
      if (tracerOrigin == null) {
         return var3;
      } else {
         return tracerOrigin.squaredDistanceTo(var1) < 0.0225 ? var3 : tracerOrigin;
      }
   }

   private static int fillArgb(Color var0) {
      int var1 = Math.clamp(var0.getAlpha() / 4, 24, 70);
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), var1).getRGB();
   }
}
