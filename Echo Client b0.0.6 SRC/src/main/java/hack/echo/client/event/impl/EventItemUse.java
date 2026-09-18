package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@Getter
public class EventItemUse extends Event {
    private final Player player;
    private final InteractionHand hand;

    public EventItemUse(Stage stage, Player player, InteractionHand hand) {
        super(stage);
        this.player = player;
        this.hand = hand;
    }

    public static class Pre extends EventItemUse {
        public Pre(Player player, InteractionHand hand) {
            super(Stage.PRE, player, hand);
        }
    }

    public static class Post extends EventItemUse {
        public Post(Player player, InteractionHand hand) {
            super(Stage.POST, player, hand);
        }
    }
}
