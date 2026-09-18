package we.devs.opium.client.events;

import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;

public class EventKey extends EventArgument {
    private final int keyCode;
    private final int action;

    public EventKey(int keyCode, int scanCode) {
        this.keyCode = keyCode;
        this.action = scanCode;
    }

    public int getKeyCode() {
        return this.keyCode;
    }

    public int getAction() {
        return this.action;
    }

    @Override
    public void call(EventListener listener) {
        listener.onKey(this);
    }
}
