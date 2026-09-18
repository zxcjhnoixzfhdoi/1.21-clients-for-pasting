package x;

import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent extends Event {
   private final MatrixStack a;
   private final float b;

   public Render3DEvent(MatrixStack var1, float var2) {
      this.a = var1;
      this.b = var2;
   }

   public MatrixStack a() {
      return this.a;
   }

   public float b() {
      return this.b;
   }
}
