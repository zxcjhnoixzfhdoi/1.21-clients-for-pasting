package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Getter
public class EventOnAttackEntity extends Event {
    private final Player player;
    private final Entity target;

    public EventOnAttackEntity(Stage stage, Player player, Entity target) {
        super(stage);
        this.player = player;
        this.target = target;
    }

    public static class Pre extends EventOnAttackEntity {
        public Pre(Player player, Entity target) {
            super(Stage.PRE, player, target);
        }
    }

    public static class Post extends EventOnAttackEntity {
        public Post(Player player, Entity target) {
            super(Stage.POST, player, target);
        }
    }
}
