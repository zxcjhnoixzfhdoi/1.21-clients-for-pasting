package hack.echo.client.mixin.input;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.movement.Sprint;
import net.minecraft.client.player.ClientInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientInput.class)
public class MixinClientInput {

    @Shadow
    protected Vec2 moveVector;

    @Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
    private void onHasForwardImpulse2(CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;

        Sprint sprint = Echo.featureManager.getFeatureByClass(Sprint.class);
        if (sprint != null && sprint.isOmniSprintEnabled()) {
            cir.setReturnValue(this.moveVector.lengthSquared() > 1.0E-5F);
        }
    }
}