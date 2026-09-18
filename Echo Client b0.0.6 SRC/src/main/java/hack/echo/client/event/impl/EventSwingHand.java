package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

@Getter
public class EventSwingHand extends Event {
    private final Player player;
    private final InteractionHand hand;

    public EventSwingHand(Player player, InteractionHand hand) {
        this.player = player;
        this.hand = hand;
    }

}
