package dev.ambience.event.impl.input;

import x.Event;

public class MouseInputEvent extends Event {
   private final int button;
   private final int action;

   public MouseInputEvent(int var1, int var2) {
      this.button = var1;
      this.action = var2;
   }

   public int getButton() {
      return this.button;
   }

   public int getAction() {
      return this.action;
   }
}
