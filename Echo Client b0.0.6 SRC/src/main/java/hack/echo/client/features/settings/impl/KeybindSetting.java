package hack.echo.client.features.settings.impl;

import lombok.Getter;
import lombok.Setter;
import hack.echo.client.features.settings.Setting;

import java.util.function.Predicate;

@Getter
@Setter
public class KeybindSetting extends Setting {
    private int key;
    private boolean listening;

    public KeybindSetting(CharSequence name, int defaultKey) {
        super(name);
        this.key = defaultKey;
        this.listening = false;
    }

    public KeybindSetting(CharSequence name, int defaultKey, Predicate<Object> dependency) {
        super(name);
        this.key = defaultKey;
        this.listening = false;
        this.setDependency(dependency);
    }
    
    public KeybindSetting(String name, int defaultKey) {
        this((CharSequence) name, defaultKey);
    }

    public KeybindSetting(String name, int defaultKey, Predicate<Object> dependency) {
        this((CharSequence) name, defaultKey, dependency);
    }

    @Override
    public String getTypeId() { return "key"; }

}


