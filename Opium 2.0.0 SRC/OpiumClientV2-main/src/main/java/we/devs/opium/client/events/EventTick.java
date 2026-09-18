package we.devs.opium.client.events;

import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;

public class EventTick extends EventArgument {
    @Override
    public void call(EventListener listener) {
        listener.onTick(this);
    }
}
