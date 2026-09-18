package dev.ambience.mixin.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityPrevRotationAccessor {
   @Accessor("lastYaw")
   float ambience$getYRotO();

   @Accessor("lastPitch")
   float ambience$getXRotO();

   @Accessor("lastYaw")
   void ambience$setYRotO(float var1);

   @Accessor("lastPitch")
   void ambience$setXRotO(float var1);
}
