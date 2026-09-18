
package hack.echo.client.features.settings;

import hack.echo.client.Echo;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Predicate;

@Getter
public class Setting {
    private final CharSequence nameSequence;
    @Setter
    private Predicate<Object> dependency;
    @Setter
    private Runnable changeListener;
    private CharSequence description = "";

    public Setting(CharSequence nameSequence) {
        this.nameSequence = nameSequence;
        this.dependency = null;
    }

    public Setting(CharSequence nameSequence, Predicate<Object> dependency) {
        this.nameSequence = nameSequence;
        this.dependency = dependency;
    }

    public Setting(String name) {
        this.nameSequence = name;
        this.dependency = null;
    }

    public String getName() {
        return nameSequence.toString();
    }

    public CharSequence getNameSequence() {
        return nameSequence;
    }

    public CharSequence getSuffixSequence() {
        return "";
    }

    public String getTypeId() {
        return "";
    }

    public boolean isVisible() {
        return dependency == null || dependency.test(null);
    }

    public Setting onChanged(Runnable listener) {
        this.changeListener = listener;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T describedBy(CharSequence description) {
        this.description = description == null ? "" : description;
        return (T) this;
    }

    public void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
        if (Echo.featureConfig != null && !Echo.featureConfig.loading) {
            Echo.featureConfig.saveProfile("_autosave");
        }
    }
}
