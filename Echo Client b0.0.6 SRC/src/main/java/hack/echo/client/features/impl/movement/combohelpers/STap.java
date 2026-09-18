package hack.echo.client.features.impl.movement.combohelpers;

import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.utils.strings.Concat;

public class STap extends ComboMode {

    public static final IntSetting durationSetting = new IntSetting(Concat.of("Duration"), 3, 1, 20, Concat.of(" Ticks"));
    public static final IntSetting reactionSetting = new IntSetting(Concat.of("Reaction Time"), 0, 0, 20, Concat.of(" Ticks"));
    public static final BoolSetting allowInAir = new BoolSetting(Concat.of("Ground Only"), false);

    @Override
    protected boolean validateAttack(EventStartAttack e) {
        return mc.player.zza > 0.0;
    }

    @Override
    protected void applyMovement(EventMovementInput e) {
        int ticksSinceAttack = mc.player.tickCount - attackTick;
        int startTick = reactionSetting.getValue();
        int endTick = startTick + durationSetting.getValue();

        if (ticksSinceAttack >= startTick && ticksSinceAttack < endTick) {
            if (allowInAir.getValue() && !mc.player.onGround()) {
                e.forward = isForwardKeyDown();
            } else {
                e.forward = false;
                e.back = true;
            }
        } else {
            e.back = isBackKeyDown();
            e.forward = isForwardKeyDown();
        }
    }
}
