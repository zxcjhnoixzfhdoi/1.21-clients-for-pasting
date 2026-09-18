package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.TotemAnimationHook;

public class TotemAnimation extends Module {
   private static TotemAnimation INSTANCE;
   private final Setting<Float> scale;
   private final Setting<Integer> length;

   public TotemAnimation() {
      super("TotemAnimation", "Makes the totem pop overlay smaller.", Module.Category.VISUALS);
      this.scale = this.num("Scale", 0.4F, 0.15F, 1.0F).setPage("General");
      this.length = this.num("Length", 40, 20, 80).setPage("General");
      INSTANCE = this;

      TotemAnimationHook.bind(new TotemAnimationHook.Impl() {
         @Override
         public boolean enabled() {
            return INSTANCE.isEnabled();
         }

         @Override
         public int animationLength() {
            return INSTANCE.length();
         }

         @Override
         public float animationScale() {
            return INSTANCE.scale();
         }
      });
   }

   public static TotemAnimation get() {
      return INSTANCE;
   }

   public float scale() {
      return this.scale.getValue();
   }

   public int length() {
      return this.length.getValue();
   }
}
