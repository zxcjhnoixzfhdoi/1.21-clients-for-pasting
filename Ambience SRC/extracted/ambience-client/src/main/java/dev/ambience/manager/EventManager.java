package dev.ambience.manager;

import dev.ambience.Ambience;
import dev.ambience.event.impl.input.KeyInputEvent;
import dev.ambience.event.impl.input.MouseInputEvent;
import dev.ambience.event.system.Subscribe;
import dev.ambience.features.Feature;
import dev.ambience.hooks.OmniSprintHook;
import dev.ambience.util.DebugLogger;
import dev.ambience.util.RotationManager;
import x.PostTickEvent;
import x.PreTickEvent;
import x.TickEvent;

public class EventManager extends Feature {
   public void init() {
      EVENT_BUS.register(this);
   }

   public void onUnload() {
      EVENT_BUS.unregister(this);
   }

   @Subscribe
   public void onPreTick(PreTickEvent var1) {
      if (!nullCheck()) {
         RotationManager.a();
         OmniSprintHook.apply(mc.player);
         Ambience.e.onPreTick();
         DebugLogger.e();
      }
   }

   @Subscribe
   public void onTick(TickEvent var1) {
      if (!nullCheck()) {
         Ambience.e.onTick();
      }
   }

   @Subscribe
   public void onPostPositionTick(PostTickEvent var1) {
      if (!nullCheck()) {
         Ambience.e.onPostPositionTick();
      }
   }

   @Subscribe
   public void onKeyInput(KeyInputEvent var1) {
      if (var1.getAction() == 1) {
         Ambience.e.onKeyPressed(var1.getKey());
      } else if (var1.getAction() == 0) {
         Ambience.e.onKeyReleased(var1.getKey());
      }
   }

   @Subscribe
   public void onMouseInput(MouseInputEvent var1) {
      if (var1.getAction() == 1) {
         Ambience.e.onMousePressed(var1.getButton());
      } else if (var1.getAction() == 0) {
         Ambience.e.onMouseReleased(var1.getButton());
      }
   }
}
