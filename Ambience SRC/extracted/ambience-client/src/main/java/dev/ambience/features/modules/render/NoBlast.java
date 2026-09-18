package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.NoBlastHook;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

public class NoBlast extends Module {
   private static NoBlast INSTANCE;

   public NoBlast() {
      super("NoBlast", "Removes explosion blast particles.", Module.Category.RENDER);
      INSTANCE = this;
      NoBlastHook.bind(new NoBlastHook.Impl() {
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

   public static NoBlast get() {
      return INSTANCE;
   }

   public boolean shouldCancel(ParticleEffect effect) {
      if (!this.isEnabled() || effect == null) {
         return false;
      }
      ParticleType<?> type = effect.getType();
      return type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER;
   }
}
