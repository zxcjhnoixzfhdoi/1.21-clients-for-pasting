package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MovementAntiBotCheck implements AntiBotCheck, Imports {

    private final Set<UUID> movedEntities = new HashSet<>();

    @Override
    public CharSequence settingName() {
        return Concat.of("Movement");
    }

    @Override
    public boolean isBot(Player player, TargetControlModule targets) {
        if (!targets.antiBotChecks.isEnabled(settingName())) {
            return false;
        }

        return !hasEntityMoved(player);
    }

    private boolean hasEntityMoved(Player player) {
        if (mc.getConnection() == null) {
            return false;
        }

        if (player.xo != player.getX() || player.zo != player.getZ()) {
            movedEntities.add(player.getUUID());
        }

        return movedEntities.contains(player.getUUID());
    }
}
