package dev.ambience.mixin.client;

import dev.ambience.hooks.FreecamHook;
import dev.ambience.util.RaytraceUtil;
import dev.ambience.util.SilentAim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "updateCrosshairTarget(F)V", at = @At("RETURN"))
   private void ambience$cameraPick(float var1, CallbackInfo var2) {
      if (this.client.player != null && this.client.world != null) {
         if (FreecamHook.isActive()) {
            this.ambience$applyPick(FreecamHook.getCameraPos(var1), FreecamHook.getPitch(), FreecamHook.getYaw());
         } else if (SilentAim.a()) {
            this.ambience$applyPick(this.client.player.getCameraPosVec(var1), SilentAim.d(), SilentAim.c());
         }
      }
   }

   @Unique
   private void ambience$applyPick(Vec3d var1, float var2, float var3) {
      double var4 = this.client.player.getBlockInteractionRange();
      double var6 = this.client.player.getEntityInteractionRange();
      Object var8 = RaytraceUtil.a(this.client.world, this.client.player, var1, var2, var3, var4, var6);
      if (var8 == null) {
         Vec3d var9 = RaytraceUtil.a(var2, var3);
         Vec3d var10 = var1.add(var9.multiply(var4));
         var8 = BlockHitResult.createMissed(var10, Direction.getFacing(var9.x, var9.y, var9.z), BlockPos.ofFloored(var10));
      }

      this.client.crosshairTarget = (HitResult)var8;
      this.client.targetedEntity = var8 instanceof EntityHitResult var11 ? var11.getEntity() : null;
   }
}
