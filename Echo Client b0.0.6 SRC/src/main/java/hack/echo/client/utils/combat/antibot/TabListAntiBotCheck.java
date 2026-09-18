package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

public final class TabListAntiBotCheck implements AntiBotCheck, Imports {

    @Override
    public CharSequence settingName() {
        return Concat.of("Tab List");
    }

    @Override
    public boolean isBot(Player player, TargetControlModule targets) {
        if (!targets.antiBotChecks.isEnabled(settingName())) {
            return false;
        }

        if (mc.getConnection() == null) {
            return false;
        }

        PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        return info == null;
    }
}
