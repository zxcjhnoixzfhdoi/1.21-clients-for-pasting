package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.inject.InjectBootstrap;
import dev.ambience.util.traits.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import x.PreTickEvent;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftTick extends MinecraftClient {
   private MixinMinecraftTick(RunArgs var1) {
      super(var1);
   }

   @Inject(at = "HEAD")
   public void tick() {
      if (!InjectBootstrap.isStarted()) {
         InjectBootstrap.start();
      }

      if (this.player != null) {
         Util.EVENT_BUS.post(new PreTickEvent());
      }
   }
}
