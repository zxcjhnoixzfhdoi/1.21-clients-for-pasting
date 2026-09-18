package hack.echo.client.features.settings.impl;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Predicate;

import hack.echo.client.features.settings.Setting;

@Getter
@Setter
public class IntSetting extends Setting {
    private int value;
    private int minValue;
    private int maxValue;
    private CharSequence suffixSequence = "";
    private String suffix = "";

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue, String suffix) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffixSequence = suffix;
        this.suffix = suffix;
    }

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue, String suffix,
            Predicate<Object> dependency) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffixSequence = suffix;
        this.suffix = suffix;
        this.setDependency(dependency);
    }

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue, CharSequence suffix) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffixSequence = suffix;
        this.suffix = null;
    }

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue, CharSequence suffix,
            Predicate<Object> dependency) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffixSequence = suffix;
        this.suffix = null;
        this.setDependency(dependency);
    }

    public IntSetting(CharSequence name, int defaultValue, int minValue, int maxValue, Predicate<Object> dependency) {
        super(name);
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.setDependency(dependency);
    }

    public IntSetting(String name, int defaultValue, int minValue, int maxValue) {
        this((CharSequence) name, defaultValue, minValue, maxValue);
    }

    public IntSetting(String name, int defaultValue, int minValue, int maxValue, String suffix) {
        this((CharSequence) name, defaultValue, minValue, maxValue, suffix);
    }

    public IntSetting(String name, int defaultValue, int minValue, int maxValue, String suffix,
            Predicate<Object> dependency) {
        this((CharSequence) name, defaultValue, minValue, maxValue, suffix, dependency);
    }

    public IntSetting(String name, int defaultValue, int minValue, int maxValue, Predicate<Object> dependency) {
        this((CharSequence) name, defaultValue, minValue, maxValue, dependency);
    }

    public void setValue(int value) {
        this.value = Math.max(minValue, Math.min(maxValue, value));
        notifyChange();
    }

    public void setValueNoNotify(int value) {
        this.value = Math.max(minValue, Math.min(maxValue, value));
    }

    public String getSuffix() {
        if (this.suffix != null)
            return this.suffix;
        if (this.suffixSequence == null)
            return null;
        this.suffix = this.suffixSequence.toString();
        return this.suffix;
    }

    public CharSequence getSuffixSequence() {
        return this.suffixSequence;
    }

    @Override
    public String getTypeId() { return "int"; }
}