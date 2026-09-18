package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class AntiBotChecks {

    private static final List<AntiBotCheck> CHECKS = List.of(
            new NameAntiBotCheck(),
            new BlissAntiBotCheck(),
            new MovementAntiBotCheck(),
            new ScaleAntiBotCheck(),
            new TabListAntiBotCheck()
    );

    private AntiBotChecks() {
    }

    public static boolean isBot(Player player, TargetControlModule targets) {
        for (AntiBotCheck check : CHECKS) {
            if (!check.isBot(player, targets)) {
                continue;
            }

            return true;
        }

        return false;
    }
}
