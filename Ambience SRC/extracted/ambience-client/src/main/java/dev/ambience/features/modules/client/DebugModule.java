package dev.ambience.features.modules.client;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.DebugLogger;
import net.minecraft.text.Text;

public class DebugModule extends Module {
   private static DebugModule INSTANCE;
   public final Setting<Boolean> chatNotify = this.bool("ChatNotify", true).setPage("General");
   public final Setting<Boolean> logFlying = this.bool("LogFlying", true).setPage("General");
   public final Setting<Boolean> logLooks = this.bool("LogLooks", true).setPage("General");
   public final Setting<Boolean> logTicks = this.bool("LogTicks", false).setPage("General");
   public final Setting<Integer> history = this.num("History", 160, 40, 400).setPage("General");

   public DebugModule() {
      super("Debug", "Logs module actions and dumps context on Grim flags to logs/ambience-debug.log", Module.Category.CLIENT);
      INSTANCE = this;
   }

   public static DebugModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getDisplayInfo() {
      return DebugLogger.d() == null ? "file" : "on";
   }

   @Override
   public void onEnable() {
      DebugLogger.b();
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("Ambience debug logging to logs/ambience-debug.log"), false);
      }
   }

   @Override
   public void onDisable() {
      DebugLogger.c();
   }
}
