package dev.ambience.features.gui.items.buttons;
import dev.ambience.features.settings.Setting;
public class SettingButton<T> extends dev.ambience.features.gui.base.GuiBase {
    protected Setting<T> setting;
    public SettingButton() {}
    public SettingButton(Setting<T> s) { this.setting = s; }
    public static <T> SettingButton<T> of(Setting<T> s) { return new SettingButton<>(s); }
}
