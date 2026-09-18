package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import net.minecraft.world.entity.player.Player;

public interface AntiBotCheck {

    CharSequence settingName();

    boolean isBot(Player player, TargetControlModule targets);
}
