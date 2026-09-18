package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class EventStartAttack extends Event {
    private final Player player;
    private final Entity target;

    public EventStartAttack(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public Entity getTarget() {
        return target;
    }

    public static class Pre extends EventStartAttack {
        public Pre(Player player, Entity target) {
            super(player, target);
        }
    }
}
