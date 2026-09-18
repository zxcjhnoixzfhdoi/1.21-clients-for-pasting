package hack.echo.client.event.impl;

import hack.echo.client.event.Event;

public class EventBlockBreaking extends Event {
    private boolean breaking = false;

    public EventBlockBreaking(Event.Stage stage) {
        super(stage);
    }

    public boolean EventBlockBreaking() {
        this.breaking = breaking;
        return breaking;
    }

    public static class Pre extends EventBlockBreaking {
        public Pre() {
            super(Event.Stage.PRE);
        }
    }

    public static class Post extends EventBlockBreaking {
        public Post() {
            super(Event.Stage.POST);
        }
    }
}
