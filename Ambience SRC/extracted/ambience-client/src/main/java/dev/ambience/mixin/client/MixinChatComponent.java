package dev.ambience.mixin.client;

import dev.ambience.util.DebugLogger;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MixinChatComponent {
   @Inject(method = "addMessage(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V", at = @At("HEAD"))
   private void ambience$grimFlag(Text var1, MessageSignatureData var2, MessageIndicator var3, CallbackInfo var4) {
      if (var1 != null) {
         DebugLogger.a(var1.getString());
      }
   }
}
