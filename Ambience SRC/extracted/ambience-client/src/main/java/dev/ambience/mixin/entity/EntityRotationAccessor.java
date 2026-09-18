package dev.ambience.mixin.entity;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface EntityRotationAccessor {
   @Accessor("renderPitch")
   float ambience$getXBob();

   @Accessor("renderPitch")
   void ambience$setXBob(float var1);

   @Accessor("lastRenderPitch")
   float ambience$getXBobO();

   @Accessor("lastRenderPitch")
   void ambience$setXBobO(float var1);

   @Accessor("renderYaw")
   float ambience$getYBob();

   @Accessor("renderYaw")
   void ambience$setYBob(float var1);

   @Accessor("lastRenderYaw")
   float ambience$getYBobO();

   @Accessor("lastRenderYaw")
   void ambience$setYBobO(float var1);
}
