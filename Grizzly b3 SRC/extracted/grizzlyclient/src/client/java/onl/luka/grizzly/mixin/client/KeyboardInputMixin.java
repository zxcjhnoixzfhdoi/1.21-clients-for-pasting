package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.movement.Spin;
import onl.luka.grizzly.module.modules.movement.Stasis;
import onl.luka.grizzly.module.modules.movement.MoveFix;
import onl.luka.grizzly.util.RotationManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("RETURN"))
    private void medved$freezeMovement(CallbackInfo ci) {
        if (Stasis.shouldFreezeInput() || RotationManager.freezeMovement) {
            this.keyPresses = Input.EMPTY;
        } else if (RotationManager.suppressJump) {
            this.keyPresses = new Input(
                this.keyPresses.forward(),
                this.keyPresses.backward(),
                this.keyPresses.left(),
                this.keyPresses.right(),
                false,
                this.keyPresses.shift(),
                this.keyPresses.sprint()
            );
        }

        Input input = MoveFix.correctInput(this.keyPresses);
        this.keyPresses = Spin.correctInput(input);
        this.moveVector = Spin.correctMoveVector(input);
    }
}
