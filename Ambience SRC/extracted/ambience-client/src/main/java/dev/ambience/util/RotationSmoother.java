package dev.ambience.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class RotationSmoother {
   private static final float a = 0.15F;
   private static final float b = 0.08F;
   private float c;
   private float d;

   public void a() {
      this.c = 0.0F;
      this.d = 0.0F;
   }

   public void a(ClientPlayerEntity var1, float var2, float var3, float var4, float var5) {
      this.a(var1, var2, var3, var4, var5, true, true);
   }

   public void a(ClientPlayerEntity var1, float var2, float var3, float var4, float var5, boolean var6, boolean var7) {
      if (var1 != null && !(var4 < 1.0E-4F)) {
         float var8 = var1.getYaw();
         float var9 = var1.getPitch();
         float[] var10 = this.a(var8, var9, var2, var3, var4, var5, var6, var7);
         if (var10 != null) {
            float var11 = MathHelper.wrapDegrees(var10[0] - var8);
            float var12 = var10[1] - var9;
            if (!(Math.abs(var11) < 1.0E-4F) || !(Math.abs(var12) < 1.0E-4F)) {
               var1.changeLookDirection(var11 / 0.15F, var12 / 0.15F);
            }
         }
      }
   }

   public void a(float var1, float var2, float var3, float var4, boolean var5, boolean var6) {
      if (SilentAim.a() && !(var3 < 1.0E-4F)) {
         float var7 = SilentAim.c();
         float var8 = SilentAim.d();
         float[] var9 = this.a(var7, var8, var1, var2, var3, var4, var5, var6);
         if (var9 != null) {
            SilentAim.b(var9[0], var9[1]);
         }
      }
   }

   private float[] a(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, boolean var8) {
      float var9 = var7 ? MathHelper.wrapDegrees(var3 - var1) : 0.0F;
      float var10 = var8 ? var4 - var2 : 0.0F;
      float var11 = (float)Math.hypot(var9, var10);
      if (var11 < 0.08F) {
         this.a();
         return null;
      }

      float var12 = 5.5F + var6 * 1.35F;
      float var13 = 2.15F * var12;
      if (var7) {
         this.c = this.c + (var9 * var12 * var12 - this.c * var13) * var5;
      } else {
         this.c = 0.0F;
      }

      if (var8) {
         this.d = this.d + (var10 * var12 * var12 - this.d * var13) * var5;
      } else {
         this.d = 0.0F;
      }

      float var14 = 90.0F + var6 * 18.0F;
      float var15 = (float)Math.hypot(this.c, this.d);
      if (var15 > var14) {
         float var16 = var14 / var15;
         this.c *= var16;
         this.d *= var16;
      }

      float var20 = this.c * var5;
      float var17 = this.d * var5;
      if (Math.abs(var20) > Math.abs(var9)) {
         var20 = var9;
         this.c = 0.0F;
      }

      if (Math.abs(var17) > Math.abs(var10)) {
         var17 = var10;
         this.d = 0.0F;
      }

      float var18 = GcdFix.a(var1, var1 + var20);
      float var19 = GcdFix.b(var2, var2 + var17);
      return new float[]{var18, var19};
   }
}
