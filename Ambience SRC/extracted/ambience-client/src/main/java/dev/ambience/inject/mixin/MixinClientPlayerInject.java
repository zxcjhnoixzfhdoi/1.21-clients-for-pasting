package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.AutoSprintHook;
import dev.ambience.hooks.OmniSprintHook;
import dev.ambience.util.DebugLogger;
import dev.ambience.util.RotationManager;
import dev.ambience.util.SilentAim;
import dev.ambience.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import x.PostTickEvent;
import x.TickEvent;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerInject {
   @Inject(method = "sendMovementPackets", at = "HEAD", captureArgs = true)
   public static void beforeSendPosition(ClientPlayerEntity var0) {
      Util.EVENT_BUS.post(new TickEvent());
      RotationManager.a(var0);
      DebugLogger.f();
   }

   @Inject(method = "sendMovementPackets", at = "RETURN", captureArgs = true)
   public static void afterSendPosition(ClientPlayerEntity var0) {
      Util.EVENT_BUS.post(new PostTickEvent());
   }

   @Inject(method = "canStartSprinting", at = "HEAD", captureArgs = true)
   public static void autoSprint(ClientPlayerEntity var0) {
      OmniSprintHook.apply(var0);
      AutoSprintHook.apply(var0);
   }

   @Inject(method = "applyMovementSpeedFactors", at = "RETURN", cancellable = true, captureArgs = true, captureReturn = true)
   public static void omniVector(ClientPlayerEntity var0, Vec2f var1, Vec2f var2) {
      if (SilentAim.a(SilentAim.a.OMNI)) {
         if (var2 != null && !(var2.lengthSquared() < 1.0E-8F)) {
            if (Math.abs(var2.x) > 1.0E-5F) {
               Cancel.cancel(new Vec2f(0.0F, var2.length()));
            }
         }
      }
   }
}
