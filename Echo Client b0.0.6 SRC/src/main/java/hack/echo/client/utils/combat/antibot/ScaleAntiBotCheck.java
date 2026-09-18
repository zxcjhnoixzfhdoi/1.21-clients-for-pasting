package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class ScaleAntiBotCheck implements AntiBotCheck {

    private static final double MIN_VALID_PLAYER_SCALE = 0.75;
    private static final double MAX_VALID_PLAYER_SCALE = 1.25;

    @Override
    public CharSequence settingName() {
        return Concat.of("Scale");
    }

    @Override
    public boolean isBot(Player player, TargetControlModule targets) {
        if (!targets.antiBotChecks.isEnabled(settingName())) {
            return false;
        }

        double scale = player.getAttributeValue(Attributes.SCALE);
        return scale < MIN_VALID_PLAYER_SCALE || scale > MAX_VALID_PLAYER_SCALE;
    }
}
