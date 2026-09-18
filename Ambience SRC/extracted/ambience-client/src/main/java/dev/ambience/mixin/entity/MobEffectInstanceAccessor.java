package dev.ambience.mixin.entity;

import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StatusEffectInstance.class)
public interface MobEffectInstanceAccessor {
   @Accessor("duration")
   void ambience$setDuration(int var1);
}
