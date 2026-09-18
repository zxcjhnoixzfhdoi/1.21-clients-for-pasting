package x;

import net.minecraft.client.gui.DrawContext;

public class Render2DEvent extends Event {
   private final DrawContext a;
   private final float b;

   public Render2DEvent(DrawContext var1, float var2) {
      this.a = var1;
      this.b = var2;
   }

   public DrawContext a() {
      return this.a;
   }

   public float b() {
      return this.b;
   }
}
