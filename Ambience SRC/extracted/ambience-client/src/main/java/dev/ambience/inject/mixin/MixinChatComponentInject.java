package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.util.DebugLogger;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

@Mixin(ChatHud.class)
public class MixinChatComponentInject {
   @Inject(method = "addMessage", at = "HEAD", captureArgs = true)
   public static void grimFlag(ChatHud var0, Text var1, MessageSignatureData var2, MessageIndicator var3) {
      if (var1 != null) {
         DebugLogger.a(var1.getString());
      }
   }
}
