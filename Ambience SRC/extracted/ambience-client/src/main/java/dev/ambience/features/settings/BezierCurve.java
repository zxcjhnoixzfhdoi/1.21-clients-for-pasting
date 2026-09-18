package dev.ambience.features.settings;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;

public final class BezierCurve {
   public static final float DELAY_TRACK_MIN = 0.0F;
   public static final float DELAY_TRACK_MAX = 400.0F;
   public float x1;
   public float y1;
   public float x2;
   public float y2;
   public float minMs;
   public float maxMs;

   public BezierCurve(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.x1 = var1;
      this.y1 = var2;
      this.x2 = var3;
      this.y2 = var4;
      this.minMs = var5;
      this.maxMs = var6;
   }

   public BezierCurve copy() {
      return new BezierCurve(this.x1, this.y1, this.x2, this.y2, this.minMs, this.maxMs);
   }

   public BezierCurve clamped() {
      BezierCurve var1 = this.copy();
      var1.x1 = MathHelper.clamp(var1.x1, 0.0F, 1.0F);
      var1.y1 = MathHelper.clamp(var1.y1, 0.0F, 1.0F);
      var1.x2 = MathHelper.clamp(var1.x2, 0.0F, 1.0F);
      var1.y2 = MathHelper.clamp(var1.y2, 0.0F, 1.0F);
      float var2 = MathHelper.clamp(Math.min(var1.minMs, var1.maxMs), 0.0F, 400.0F);
      float var3 = MathHelper.clamp(Math.max(var1.minMs, var1.maxMs), 0.0F, 400.0F);
      var1.minMs = var2;
      var1.maxMs = var3;
      return var1;
   }

   public float ease(float var1) {
      var1 = MathHelper.clamp(var1, 0.0F, 1.0F);
      if (var1 != 0.0F && var1 != 1.0F) {
         return Math.abs(this.x1 - this.y1) < 1.0E-4F && Math.abs(this.x2 - this.y2) < 1.0E-4F ? var1 : bezier(this.solveT(var1), this.y1, this.y2);
      } else {
         return var1;
      }
   }

   public int sampleMs() {
      BezierCurve var1 = this.clamped();
      if (var1.maxMs <= var1.minMs) {
         return Math.round(var1.minMs);
      }

      float var2 = var1.ease(ThreadLocalRandom.current().nextFloat());
      if (!Float.isFinite(var2)) {
         var2 = ThreadLocalRandom.current().nextFloat();
      }

      return Math.max(0, Math.round(var1.minMs + (var1.maxMs - var1.minMs) * var2));
   }

   public static BezierCurve parse(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String[] var1 = var0.split(",");
         if (var1.length != 6) {
            return null;
         }

         try {
            return new BezierCurve(
                  Float.parseFloat(var1[0].trim()),
                  Float.parseFloat(var1[1].trim()),
                  Float.parseFloat(var1[2].trim()),
                  Float.parseFloat(var1[3].trim()),
                  Float.parseFloat(var1[4].trim()),
                  Float.parseFloat(var1[5].trim())
               )
               .clamped();
         } catch (NumberFormatException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      return this.x1 + "," + this.y1 + "," + this.x2 + "," + this.y2 + "," + this.minMs + "," + this.maxMs;
   }

   private float solveT(float var1) {
      float var2 = var1;
      int var3 = 0;

      while (var3 < 8) {
         float var4 = bezier(var2, this.x1, this.x2);
         float var5 = bezierDerivative(var2, this.x1, this.x2);
         if (!(Math.abs(var5) < 1.0E-6F)) {
            float var6 = var2 - (var4 - var1) / var5;
            if (!(var6 < 0.0F) && !(var6 > 1.0F)) {
               var2 = var6;
               var3++;
               continue;
            }

            return this.solveTBisection(var1);
         }
         break;
      }

      return MathHelper.clamp(var2, 0.0F, 1.0F);
   }

   private float solveTBisection(float var1) {
      float var2 = 0.0F;
      float var3 = 1.0F;
      float var4 = var1;

      for (int var5 = 0; var5 < 12; var5++) {
         var4 = (var2 + var3) * 0.5F;
         if (bezier(var4, this.x1, this.x2) < var1) {
            var2 = var4;
         } else {
            var3 = var4;
         }
      }

      return var4;
   }

   private static float bezier(float var0, float var1, float var2) {
      float var3 = 1.0F - var0;
      return 3.0F * var3 * var3 * var0 * var1 + 3.0F * var3 * var0 * var0 * var2 + var0 * var0 * var0;
   }

   private static float bezierDerivative(float var0, float var1, float var2) {
      float var3 = 1.0F - var0;
      return 3.0F * var3 * var3 * var1 + 6.0F * var3 * var0 * (var2 - var1) + 3.0F * var0 * var0 * (1.0F - var2);
   }
}
