package dev.ambience.util.traits;

import dev.ambience.event.system.EventBus;
import net.minecraft.client.MinecraftClient;

public interface Util {
   MinecraftClient mc = MinecraftClient.getInstance();
   EventBus EVENT_BUS = new EventBus();
}
