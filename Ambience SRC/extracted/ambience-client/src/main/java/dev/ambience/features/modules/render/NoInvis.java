package dev.ambience.features.modules.render;
import dev.ambience.features.modules.Module;
import dev.ambience.hooks.NoInvisHook;
public class NoInvis extends Module {
   private static NoInvis INSTANCE;
   public NoInvis() {
      super("NoInvis", "Shows invisible entities.", Module.Category.RENDER);
      INSTANCE = this;
      NoInvisHook.bind(new NoInvisHook.Impl() {
         @Override public boolean enabled() { return INSTANCE != null && INSTANCE.isEnabled(); }
      });
   }
   public static NoInvis get() { return INSTANCE; }
}
