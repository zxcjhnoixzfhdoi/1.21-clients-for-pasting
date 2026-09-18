package dev.ambience.event.impl.input;

import x.Event;

public class KeyInputEvent extends Event {
   private final int key;
   private final int action;

   public KeyInputEvent(int var1, int var2) {
      this.key = var1;
      this.action = var2;
   }

   public int getKey() {
      return this.key;
   }

   public int getAction() {
      return this.action;
   }
}
