package x;

import dev.ambience.features.Feature;
import net.minecraft.text.Text;

public abstract class ChatMessenger extends Feature {
   public static void a(String var0, Object... var1) {
      if (var0 != null && mc.player != null) {
         String var2 = var1.length == 0 ? var0 : String.format(var0, var1);
         mc.player.sendMessage(Text.literal(var2), false);
      }
   }
}
