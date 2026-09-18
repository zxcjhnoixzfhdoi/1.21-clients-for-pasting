package we.devs.opium.api.events;

import we.devs.opium.api.manager.event.Event;
import we.devs.opium.api.manager.event.EventListener;

import javax.swing.text.html.parser.Entity;

public class PushEntityEvent extends Event {
    private final Entity pushed;
    private final Entity pusher;

    public PushEntityEvent(Entity pushed, Entity pusher) {
        this.pushed = pushed;
        this.pusher = pusher;
    }

    public Entity getPushed() {
        return this.pushed;
    }

    public Entity getPusher() {
        return this.pusher;
    }
}