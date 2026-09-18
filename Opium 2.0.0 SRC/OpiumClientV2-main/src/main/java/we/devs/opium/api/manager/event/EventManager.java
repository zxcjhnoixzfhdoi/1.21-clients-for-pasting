package we.devs.opium.api.manager.event;

import java.util.ArrayList;
import java.util.List;
import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;

public class EventManager {
    private final List<EventListener> LISTENER_REGISTRY = new ArrayList<EventListener>();

    public void call(EventArgument argument) {
        new ArrayList<EventListener>(this.LISTENER_REGISTRY).forEach(argument::call);
    }

    public void register(EventListener listener) {
        this.LISTENER_REGISTRY.add(listener);
    }

    public boolean unregister(EventListener listener) {
        return this.LISTENER_REGISTRY.remove(listener);
    }
}