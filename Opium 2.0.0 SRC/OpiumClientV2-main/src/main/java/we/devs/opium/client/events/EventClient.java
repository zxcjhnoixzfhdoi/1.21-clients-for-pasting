package we.devs.opium.client.events;

import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.client.values.Value;

public class EventClient extends EventArgument {
    private final Value value;

    public EventClient(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return this.value;
    }

    @Override
    public void call(EventListener listener) {
        listener.onClient(this);
    }
}
