package hack.echo.client.features.settings.impl;

import hack.echo.client.features.settings.Setting;
import hack.echo.client.utils.strings.Concat;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Getter
@Setter
public class ModeSetting extends Setting {
    private CharSequence valueSequence;
    private List<CharSequence> modes;
    
    public ModeSetting(CharSequence name, CharSequence defaultValue, CharSequence... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.valueSequence = defaultValue;
    }
    
    public ModeSetting(CharSequence name, CharSequence defaultValue, Predicate<Object> dependency, CharSequence... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.valueSequence = defaultValue;
        this.setDependency(dependency);
    }
    
    public ModeSetting(CharSequence name, CharSequence defaultValue, List<CharSequence> modesList) {
        super(name);
        this.modes = modesList;
        this.valueSequence = defaultValue;
    }
    
    public void cycle() {
        int index = -1;
        for (int i = 0; i < modes.size(); i++) {
            if (equalsIgnoreCase(modes.get(i), valueSequence)) {
                index = i;
                break;
            }
        }
        if (index == -1) index = 0;
        index = (index + 1) % modes.size();
        CharSequence next = modes.get(index);
        this.valueSequence = next;
        notifyChange();
    }
    
    public boolean is(CharSequence mode) {
        return equalsIgnoreCase(this.valueSequence, mode);
    }
    public CharSequence getValueSequence() {
        return this.valueSequence;
    }
    public void setValue(CharSequence cs) {
        this.valueSequence = cs;
        notifyChange();
    }


    public CharSequence getValue() {
        return this.valueSequence;
    }

    @Override
    public String getTypeId() { return "mod"; }

    private static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
        if (a instanceof Concat && b instanceof Concat) {
            return ((Concat)a).fragmentedEquals((Concat)b);
        }
        if (a == b) return true;
        if (a == null || b == null) return false;
        int len = a.length();
        if (len != b.length()) return false;
        for (int i = 0; i < len; i++) {
            char ca = Character.toLowerCase(a.charAt(i));
            char cb = Character.toLowerCase(b.charAt(i));
            if (ca != cb) return false;
        }
        return true;
    }
} 