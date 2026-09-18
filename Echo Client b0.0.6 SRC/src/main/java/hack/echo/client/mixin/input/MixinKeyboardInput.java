package hack.echo.client.mixin.input;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventInput;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.impl.render.Freecam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends ClientInput {

    @Shadow
    @Final
    private Options options;

    @Unique private boolean originalForward;
    @Unique private boolean originalBack;
    @Unique private boolean originalLeft;
    @Unique private boolean originalRight;
    @Unique private boolean originalJump;
    @Unique private boolean originalSneak;
    @Unique private boolean originalSprint;

    @Unique private EventMovementInput echo$moveEvent;

    @Unique
    private void echo$setKeyState(KeyMapping key, boolean desired, boolean toggleEnabled) {
        if (key instanceof ToggleKeyMapping && toggleEnabled) {
            if (key.isDown() != desired) {
                key.setDown(true);
            }
            return;
        }

        key.setDown(desired);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (Echo.isDestroyed) {
            this.echo$moveEvent = null;
            return;
        }

        EventInput event = new EventInput(this);
        event.call();

        this.originalForward = this.options.keyUp.isDown();
        this.originalBack = this.options.keyDown.isDown();
        this.originalLeft = this.options.keyLeft.isDown();
        this.originalRight = this.options.keyRight.isDown();
        this.originalJump = this.options.keyJump.isDown();
        this.originalSneak = this.options.keyShift.isDown();
        this.originalSprint = this.options.keySprint.isDown();

        echo$moveEvent = new EventMovementInput(
            this.originalForward,
            this.originalBack,
            this.originalLeft,
            this.originalRight,
            this.originalJump,
            this.originalSneak,
            this.originalSprint
        );

        echo$moveEvent.call();

        echo$moveEvent.syncFloatsFromBooleans();

        this.options.keyUp.setDown(echo$moveEvent.forwardAmount > 0);
        this.options.keyDown.setDown(echo$moveEvent.forwardAmount < 0);
        this.options.keyLeft.setDown(echo$moveEvent.sidewaysAmount > 0);
        this.options.keyRight.setDown(echo$moveEvent.sidewaysAmount < 0);
        this.options.keyJump.setDown(echo$moveEvent.jump);
        this.echo$setKeyState(this.options.keyShift, echo$moveEvent.sneak, this.options.toggleCrouch().get());
        this.echo$setKeyState(this.options.keySprint, echo$moveEvent.sprint, this.options.toggleSprint().get());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickPost(CallbackInfo ci) {
        if (Echo.isDestroyed) {
            this.echo$moveEvent = null;
            return;
        }

        this.options.keyUp.setDown(this.originalForward);
        this.options.keyDown.setDown(this.originalBack);
        this.options.keyLeft.setDown(this.originalLeft);
        this.options.keyRight.setDown(this.originalRight);
        this.options.keyJump.setDown(this.originalJump);
        this.echo$setKeyState(this.options.keyShift, this.originalSneak, this.options.toggleCrouch().get());
        this.echo$setKeyState(this.options.keySprint, this.originalSprint, this.options.toggleSprint().get());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void echo$onTickFreecam(CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            if (!echo$moveEvent.forceMovement) {
                this.keyPresses = new Input(false, false, false, false, false, false, false);
                this.moveVector = Vec2.ZERO;
            }
        }
    }
}
