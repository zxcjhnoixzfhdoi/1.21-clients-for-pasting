package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.inject.InjectHooks;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRendererInject {
   @Inject(method = "updateCrosshairTarget", at = "RETURN", captureArgs = true)
   public static void cameraPick(GameRenderer var0, float var1) {
      InjectHooks.cameraPick(var1);
   }
}
