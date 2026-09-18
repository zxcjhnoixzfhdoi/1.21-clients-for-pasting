package x;

import dev.ambience.features.Feature;
import dev.ambience.features.settings.Setting;

public class FeatureUpdateEvent extends Event {
   private final FeatureUpdateEvent.Type eventType;
   private final Feature b;
   private Setting<?> c;

   public FeatureUpdateEvent(FeatureUpdateEvent.Type var1, Feature var2) {
      this.eventType = var1;
      this.b = var2;
   }

   public FeatureUpdateEvent(Setting<?> var1) {
      this(FeatureUpdateEvent.Type.SETTING_UPDATE, var1.getFeature());
      this.c = var1;
   }

   public FeatureUpdateEvent.Type a() {
      return this.eventType;
   }

   public Feature b() {
      return this.b;
   }

   public Setting<?> c() {
      return this.c;
   }

   public enum Type {
      TOGGLE_MODULE,
      SETTING_UPDATE;
   }
}
