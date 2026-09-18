package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.player.Player;

public final class NameAntiBotCheck implements AntiBotCheck {

    @Override
    public CharSequence settingName() {
        return Concat.of("Name");
    }

    @Override
    public boolean isBot(Player player, TargetControlModule targets) {
        if (!targets.antiBotChecks.isEnabled(settingName())) {
            return false;
        }

        String name = getBotCheckName(player);
        if (name.isEmpty()) {
            return true;
        }

        if (name.contains(":")) {
            return true;
        }

        if (name.contains("+")) {
            return true;
        }

        if (name.contains(" ")) {
            return true;
        }

        if (name.startsWith("cit")) {
            return true;
        }

        return name.startsWith("npc");
    }

    private String getBotCheckName(Player player) {
        if (player.getDisplayName() == null) {
            return "";
        }

        String name = player.getDisplayName().getString();
        if (name == null) {
            return "";
        }

        return name.toLowerCase();
    }
}
