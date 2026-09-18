package we.devs.opium.client.values.impl;

import we.devs.opium.Opium;
import we.devs.opium.client.events.EventClient;
import we.devs.opium.client.values.Value;

public class ValueBoolean extends Value {
    private final boolean defaultValue;
    private boolean value;
    private final ValueCategory parent;

    public ValueBoolean(String name, String tag, String description, boolean value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = null;
    }

    public ValueBoolean(String name, String tag, String description, ValueCategory parent, boolean value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = parent;
    }

    public ValueCategory getParent() {
        return this.parent;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    public boolean getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
        EventClient event = new EventClient(this);
        Opium.EVENT_MANAGER.call(event);
    }
}
