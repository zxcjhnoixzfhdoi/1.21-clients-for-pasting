package hack.echo.client.features.impl.misc;

import hack.echo.client.features.Feature;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public class Teams extends Feature {

    public Teams() {
        super(new FeatureInfo(
            Concat.of("Teams"),
            Concat.of("Team management"),
            Category.INTERNALS
        ));
    }
    public BoolSetting colorCheck = new BoolSetting(Concat.of("Color Check"), true);
    public BoolSetting nameCheck = new BoolSetting(Concat.of("Name Check"), false);
    public BoolSetting scoreboardCheck = new BoolSetting(Concat.of("Scoreboard Check"), true);

    public boolean isOnSameTeam(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        if (colorCheck.getValue()) {
            String playerName = player.getDisplayName().getString();
            String selfName = mc.player.getDisplayName().getString();
            if (playerName.startsWith("§") && selfName.startsWith("§")) {
                return playerName.charAt(1) == selfName.charAt(1);
            }
        }

        if (nameCheck.getValue()) {
            String playerName = player.getDisplayName().getString();
            String selfName = mc.player.getDisplayName().getString();
            int bracketIndex = playerName.lastIndexOf('[');
            if (bracketIndex != -1) {
                String playerTeam = playerName.substring(bracketIndex);
                return selfName.endsWith(playerTeam);
            }
        }

        if (scoreboardCheck.getValue()) {
            PlayerTeam playerTeam = mc.level.getScoreboard().getPlayersTeam(player.getName().getString());
            PlayerTeam selfTeam = mc.level.getScoreboard().getPlayersTeam(mc.player.getName().getString());
            return playerTeam != null && playerTeam.equals(selfTeam);
        }

        return false;
    }
}
