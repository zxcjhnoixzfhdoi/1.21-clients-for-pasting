package we.devs.opium.asm.mixins;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import we.devs.opium.asm.ducks.ILimbAnimator;

@Mixin(LimbAnimator.class)
public class LimbAnimatorMixin implements ILimbAnimator {
    @Shadow private float pos;

    @Override
    public float cheetOpium$getPos() {
        return pos;
    }

    @Override
    public void cheetOpium$setPos(float pos) {
        this.pos = pos;
    }
}
