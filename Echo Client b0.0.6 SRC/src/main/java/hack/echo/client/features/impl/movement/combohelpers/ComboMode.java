package hack.echo.client.features.impl.movement.combohelpers;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.impl.movement.ComboHelper;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.mixin.accessors.ClientInputAccessor;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.utils.Imports;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.EntityHitResult;

public abstract class ComboMode implements Imports {

    protected int attackTick = -1;

    public void onEnable() {
        attackTick = -1;
    }

    public void onDisable() {
        attackTick = -1;
    }

    public void onAttack(EventStartAttack e) {
        if (mc.player == null || mc.level == null) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        if (!validateAttack(e)) return;

        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;

        ComboHelper comboHelper = Echo.featureManager.getFeatureByClass(ComboHelper.class);
        if (comboHelper != null && !comboHelper.isEntityAllowed(target)) return;

        if (!validateTarget(target, hit)) return;

        attackTick = mc.player.tickCount;
        applyMovementInstantly();
    }

    public void onMovement(EventMovementInput e) {
        if (mc.player == null || mc.level == null) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (mc.player.isDeadOrDying()) return;
        if (attackTick == -1 || mc.player.tickCount < attackTick) return;

        applyMovement(e);
    }

    protected boolean validateAttack(EventStartAttack e) {
        return true;
    }

    protected boolean validateTarget(LivingEntity target, EntityHitResult hit) {
        return true;
    }

    protected abstract void applyMovement(EventMovementInput e);

    private void applyMovementInstantly() {
        if (mc.player.input == null) return;

        ClientInputAccessor input = (ClientInputAccessor) mc.player.input;
        Input keys = input.getKeyPresses();

        EventMovementInput movement = new EventMovementInput(
            keys.forward(),
            keys.backward(),
            keys.left(),
            keys.right(),
            keys.jump(),
            keys.shift(),
            keys.sprint()
        );

        applyMovement(movement);
        movement.syncFloatsFromBooleans();

        input.setKeyPresses(new Input(
            movement.forward,
            movement.back,
            movement.left,
            movement.right,
            movement.jump,
            movement.sneak,
            movement.sprint
        ));
        input.setMoveVector(new Vec2(movement.sidewaysAmount, movement.forwardAmount));
    }

    protected boolean isForwardKeyDown() {
        return InputHandler.isBindDown(((KeyMappingAccessor) mc.options.keyUp).getKey().getValue());
    }

    protected boolean isBackKeyDown() {
        return InputHandler.isBindDown(((KeyMappingAccessor) mc.options.keyDown).getKey().getValue());
    }

    protected boolean isJumpKeyDown() {
        return InputHandler.isBindDown(((KeyMappingAccessor) mc.options.keyJump).getKey().getValue());
    }
}
