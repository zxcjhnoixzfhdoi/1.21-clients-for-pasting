package dev.ambience.util.traits;

public interface Toggleable {
   boolean isToggled();

   void enable();

   void disable();

   default void d() {
      if (this.isToggled()) {
         this.disable();
      } else {
         this.enable();
      }
   }

   default void a(boolean var1) {
      if (this.isToggled() != var1) {
         this.d();
      }
   }
}
