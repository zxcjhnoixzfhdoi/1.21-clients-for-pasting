package dev.ambience.mixin.client;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameOverlayRenderer.class)
public interface ScreenEffectRendererAccessor {
   @Accessor("floatingItemTimer")
   void ambience$setItemActivationTicks(int var1);
}
