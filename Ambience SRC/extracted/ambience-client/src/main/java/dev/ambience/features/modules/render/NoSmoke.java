package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.NoSmokeHook;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

public class NoSmoke extends Module {
   private static NoSmoke INSTANCE;

   public NoSmoke() {
      super("NoSmoke", "Removes smoke particles.", Module.Category.RENDER);
      INSTANCE = this;
      NoSmokeHook.bind(new NoSmokeHook.Impl() {
         @Override
         public boolean enabled() {
            return INSTANCE.isEnabled();
         }

         @Override
         public boolean shouldCancel(ParticleEffect effect) {
            return INSTANCE.shouldCancel(effect);
         }
      });
   }

   public static NoSmoke get() {
      return INSTANCE;
   }

   public boolean shouldCancel(ParticleEffect effect) {
      if (!this.isEnabled() || effect == null) {
         return false;
      }
      ParticleType<?> type = effect.getType();
      return type == ParticleTypes.SMOKE
          || type == ParticleTypes.LARGE_SMOKE
          || type == ParticleTypes.WHITE_SMOKE
          || type == ParticleTypes.CAMPFIRE_COSY_SMOKE
          || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
   }
}
