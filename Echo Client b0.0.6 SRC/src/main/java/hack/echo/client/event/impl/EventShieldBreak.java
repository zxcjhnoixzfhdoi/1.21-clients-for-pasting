package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

@Getter
public class EventShieldBreak extends Event {
    private final Player player;

    public EventShieldBreak(Player player) {
        this.player = player;
    }
}
