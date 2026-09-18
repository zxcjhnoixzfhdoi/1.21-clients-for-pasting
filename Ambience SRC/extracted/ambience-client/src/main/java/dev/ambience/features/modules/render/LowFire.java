package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.LowFireHook;
import dev.ambience.util.LowFirePack;

public class LowFire extends Module {
   private static LowFire INSTANCE;

   public LowFire() {
      super("LowFire", "Better PvP style low fire overlay.", Module.Category.VISUALS);
      INSTANCE = this;

      LowFireHook.bind(new LowFireHook.Impl() {
         @Override
         public boolean enabled() {
            return INSTANCE.isEnabled();
         }

         @Override
         public float fireYOffset() {
            return INSTANCE.fireYOffset();
         }

         @Override
         public float fireHeightScale() {
            return INSTANCE.fireHeightScale();
         }

         @Override
         public float fireUvSlice(float u0, float u1) {
            return INSTANCE.fireUvSlice(u0, u1);
         }
      });
   }

   public static LowFire get() {
      return INSTANCE;
   }

   @Override
   public void onLoad() {
      if (this.isEnabled()) {
         LowFirePack.a(true);
      }
   }

   @Override
   public void onEnable() {
      LowFirePack.a(true);
   }

   @Override
   public void onDisable() {
      LowFirePack.a(false);
   }

   public float fireHeightScale() {
      return 0.003F;
   }

   public float fireYOffset() {
      return 0.24F;
   }

   public float fireUvSlice(float u0, float u1) {
      return Math.max((u1 - u0) / 16.0F, 0.001F);
   }
}
