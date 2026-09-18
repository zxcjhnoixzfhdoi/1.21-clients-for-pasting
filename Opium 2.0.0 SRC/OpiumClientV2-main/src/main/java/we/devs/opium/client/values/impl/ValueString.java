package we.devs.opium.client.values.impl;

import we.devs.opium.Opium;
import we.devs.opium.client.events.EventClient;
import we.devs.opium.client.values.Value;

public class ValueString extends Value {
    private final String defaultValue;
    private String value;
    private final ValueCategory parent;

    public ValueString(String name, String tag, String description, String value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = null;
    }

    public ValueString(String name, String tag, String description, ValueCategory parent, String value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = parent;
    }

    public ValueCategory getParent() {
        return this.parent;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        EventClient event = new EventClient(this);
        Opium.EVENT_MANAGER.call(event);
    }
}
