package dev.ambience.mixin.entity;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityHeadRotationAccessor {
   @Accessor("headYaw")
   float ambience$getYHeadRot();

   @Accessor("headYaw")
   void ambience$setYHeadRot(float var1);

   @Accessor("lastHeadYaw")
   float ambience$getYHeadRotO();

   @Accessor("lastHeadYaw")
   void ambience$setYHeadRotO(float var1);
}
