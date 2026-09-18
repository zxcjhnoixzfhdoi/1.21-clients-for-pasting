package dev.ambience.inject;

public final class InjectState {
   public static final ThreadLocal<float[]> ITEM_NO_SWAY = new ThreadLocal<>();
   public static final ThreadLocal<Boolean> ITEM_VIEW_SCALED = ThreadLocal.withInitial(() -> Boolean.FALSE);

   private InjectState() {
   }
}
