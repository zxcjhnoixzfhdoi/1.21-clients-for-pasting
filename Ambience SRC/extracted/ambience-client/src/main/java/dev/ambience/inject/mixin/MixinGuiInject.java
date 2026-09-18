package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.inject.InjectHooks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinGuiInject {
   @Inject(method = "render", at = "RETURN", captureArgs = true)
   public static void onHudRender(InGameHud var0, DrawContext var1, RenderTickCounter var2) {
      InjectHooks.fireRender2D(var1, var2.getTickProgress(false));
   }
}
