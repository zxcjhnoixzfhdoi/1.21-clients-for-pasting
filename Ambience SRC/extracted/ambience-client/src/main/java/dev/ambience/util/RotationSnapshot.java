package dev.ambience.util;

import dev.ambience.inject.Access;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class RotationSnapshot {
   private boolean a;
   private float b;
   private float c;
   private float d;
   private float e;
   private float f;
   private float g;
   private float h;
   private float i;
   private float j;
   private float k;

   public boolean a() {
      return this.a;
   }

   public void a(ClientPlayerEntity var1) {
      if (!this.a) {
         this.b = var1.getYaw();
         this.c = var1.getPitch();
         this.d = Access.yRotO(var1);
         this.e = Access.xRotO(var1);
         this.f = Access.yHeadRot(var1);
         this.g = Access.yHeadRotO(var1);
         this.h = Access.xBob(var1);
         this.i = Access.xBobO(var1);
         this.j = Access.yBob(var1);
         this.k = Access.yBobO(var1);
         this.a = true;
      }
   }

   public void a(ClientPlayerEntity var1, float var2, float var3, boolean var4) {
      float var5 = var1.getYaw();
      float var6 = var1.getPitch();
      float var7 = GcdFix.a(var5, var2);
      float var8 = GcdFix.b(var6, var3);
      var1.setYaw(var7);
      var1.setPitch(var8);
      Access.setYRotO(var1, var7);
      Access.setXRotO(var1, var8);
      Access.setYHeadRot(var1, var7);
      Access.setYHeadRotO(var1, var7);
      if (var4) {
         float var9 = MathHelper.clamp(-var8 * 0.1F, -10.0F, 10.0F);
         Access.setXBob(var1, var9);
         Access.setXBobO(var1, var9);
         Access.setYBob(var1, 0.0F);
         Access.setYBobO(var1, 0.0F);
      }
   }

   public void b(ClientPlayerEntity var1) {
      if (this.a) {
         var1.setYaw(this.b);
         var1.setPitch(this.c);
         Access.setYRotO(var1, this.d);
         Access.setXRotO(var1, this.e);
         Access.setYHeadRot(var1, this.f);
         Access.setYHeadRotO(var1, this.g);
         Access.setXBob(var1, this.h);
         Access.setXBobO(var1, this.i);
         Access.setYBob(var1, this.j);
         Access.setYBobO(var1, this.k);
         this.a = false;
      }
   }
}
