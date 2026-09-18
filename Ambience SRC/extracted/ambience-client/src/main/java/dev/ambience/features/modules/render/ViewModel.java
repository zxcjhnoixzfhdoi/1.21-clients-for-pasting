package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.ViewModelHook;
import java.util.function.Predicate;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class ViewModel extends Module {
   private static ViewModel INSTANCE;
   public final Setting<Double> scale;
   public final Setting<Double> posX;
   public final Setting<Double> posY;
   public final Setting<Double> posZ;
   public final Setting<Boolean> noSway;
   public final Setting<Boolean> noSwapAnimation;
   public final Setting<Boolean> oldAnimation;

   public ViewModel() {
      super("ViewModel", "Scale and position first-person hands.", Module.Category.VISUALS);
      this.scale           = this.num("Scale",           0.85, 0.0, 3.0).setPage("General");
      this.posX            = this.num("PosX",            0.0, -1.0, 1.0).setPage("General");
      this.posY            = this.num("PosY",            0.0, -1.0, 1.0).setPage("General");
      this.posZ            = this.num("PosZ",            0.0, -1.0, 1.0).setPage("General");
      this.noSway          = this.bool("NoSway",          false).setPage("General");
      this.noSwapAnimation = this.bool("NoSwapAnimation", false).setPage("General");
      this.oldAnimation    = this.bool("OldAnimation",    false).setPage("General");
      INSTANCE = this;

      ViewModelHook.bind(new ViewModelHook.Impl() {
         @Override
         public boolean enabled()          { return INSTANCE.isEnabled(); }
         @Override
         public boolean noSway()           { return INSTANCE.isEnabled() && INSTANCE.noSway.getValue(); }
         @Override
         public boolean noSwapAnimation()  { return INSTANCE.isEnabled() && INSTANCE.noSwapAnimation.getValue(); }
         @Override
         public boolean oldAnimation()     { return INSTANCE.isEnabled() && INSTANCE.oldAnimation.getValue(); }
         @Override
         public void applyArm(AbstractClientPlayerEntity player, Hand hand, MatrixStack matrices) {
            INSTANCE.applyArm(player, hand, matrices);
         }
      });
   }

   public static ViewModel get() {
      return INSTANCE;
   }

   public static boolean test(Predicate<ViewModel> pred) {
      return INSTANCE != null && INSTANCE.isEnabled() && pred.test(INSTANCE);
   }

   private void applyArm(AbstractClientPlayerEntity player, Hand hand, MatrixStack matrices) {
      Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
      float side = arm == Arm.RIGHT ? 1.0F : -1.0F;

      double px = this.posX.getValue();
      double py = this.posY.getValue();
      double pz = this.posZ.getValue();
      float  sc = this.scale.getValue().floatValue();

      // pivot: translate to grip, scale, translate back
      matrices.push();
      matrices.translate(px * side, py, pz);
      matrices.translate( 0.56 * side, -0.52, -0.72);
      matrices.scale(sc, sc, sc);
      matrices.translate(-0.56 * side,  0.52,  0.72);
   }
}
