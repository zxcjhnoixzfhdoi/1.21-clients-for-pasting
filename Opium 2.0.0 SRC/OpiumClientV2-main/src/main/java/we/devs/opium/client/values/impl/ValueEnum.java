package we.devs.opium.client.values.impl;

import we.devs.opium.Opium;
import we.devs.opium.client.events.EventClient;
import we.devs.opium.client.values.Value;

import java.util.ArrayList;
import java.util.Arrays;

public class ValueEnum<T extends Enum<?>> extends Value {
    private final T defaultValue;
    private T value;
    private final ValueCategory parent;
    private T[] values;

    @SuppressWarnings("unchecked")
    public ValueEnum(String name, String tag, String description, T value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = null;
        values = (T[]) value.getDeclaringClass().getEnumConstants();
    }

    public ValueEnum(String name, String tag, String description, ValueCategory parent, T value) {
        super(name, tag, description);
        this.defaultValue = value;
        this.value = value;
        this.parent = parent;
    }

    public ValueCategory getParent() {
        return this.parent;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
        EventClient event = new EventClient(this);
        Opium.EVENT_MANAGER.call(event);
    }

    public T getEnum(String name) {
        for (T value : this.getEnums()) {
            if (!value.name().equals(name)) continue;
            return value;
        }
        return null;
    }

    public ArrayList<T> getEnums() {
        return new ArrayList<>(Arrays.asList(values));
    }
}
