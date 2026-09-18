package dev.ambience.mixin.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityRotationFieldsAccessor {
   @Accessor("yaw")
   float ambience$getYRot();

   @Accessor("pitch")
   float ambience$getXRot();

   @Accessor("yaw")
   void ambience$setYRot(float var1);

   @Accessor("pitch")
   void ambience$setXRot(float var1);
}
