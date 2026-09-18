package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

@Getter
public class EventPerformUseItemOn extends Event {
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult  hitResult;

    public EventPerformUseItemOn(Stage stage, Player player, InteractionHand hand, BlockHitResult hitResult) {
        super(stage);
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
    }

    public static class Pre extends EventPerformUseItemOn {
        public Pre(Player player, InteractionHand hand, BlockHitResult hitResult) {
            super(Stage.PRE, player, hand, hitResult);
        }
    }

    public static class Post extends EventPerformUseItemOn {
        public Post(Player player, InteractionHand hand, BlockHitResult hitResult) {
            super(Stage.POST, player, hand, hitResult);
        }
    }
    
    
}
