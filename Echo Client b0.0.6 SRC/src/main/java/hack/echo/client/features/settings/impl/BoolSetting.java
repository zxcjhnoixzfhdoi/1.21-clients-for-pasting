package hack.echo.client.features.settings.impl;

import lombok.Setter;

import java.util.function.Predicate;

import hack.echo.client.features.settings.Setting;

@Setter
public class BoolSetting extends Setting {
    private boolean value;

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        notifyChange();
    }

    public BoolSetting(CharSequence name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }
    
    public BoolSetting(CharSequence name, boolean defaultValue, Predicate<Object> dependency) {
        super(name);
        this.value = defaultValue;
        this.setDependency(dependency);
    }
    
    public BoolSetting(String name, boolean defaultValue) {
        this((CharSequence) name, defaultValue);
    }
    
    public BoolSetting(String name, boolean defaultValue, Predicate<Object> dependency) {
        this((CharSequence) name, defaultValue, dependency);
    }

    @Override
    public String getTypeId() { return "boo"; }
}
