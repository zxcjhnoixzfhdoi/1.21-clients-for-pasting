package dev.ambience.features.modules.client;
import dev.ambience.features.modules.Module;
import dev.ambience.hooks.GuiRuntime;
import dev.ambience.features.settings.Setting;
import java.awt.Color;
public class ClickGui extends Module implements GuiRuntime.Host {
    private static ClickGui INSTANCE;
    public Setting<String> a = this.str("Prefix",".");
    public Setting<Color> d = this.color("Custom Color",120,170,210,220);
    public Setting<Boolean> e = this.bool("Smooth",true);
    public Setting<Integer> f = this.num("Delay",240,0,600);
    public Setting<Float> g = this.num("Brightness",200f,1f,255f);
    public Setting<Float> h = this.num("Saturation",140f,1f,255f);
    public Setting<Boolean> i = this.bool("ClickGUI Font",true);
    public Setting<Boolean> j = this.bool("HUD Font",false);
    public Setting<String> k = this.str("Font Name","");
    public Setting<Theme> themeSettingField = this.mode("Theme",Theme.AMBIENCE);
    public Setting<Layout> layoutSettingField = this.mode("Layout",Layout.CENTER);
    public ClickGui() { super("ClickGui","Opens the ClickGui.",Module.Category.CLIENT); INSTANCE=this; }
    public static ClickGui get() { return INSTANCE; }
    @Override public boolean modernBlur() { return false; }
    @Override public boolean isClickGui(net.minecraft.client.gui.screen.Screen s) { return false; }
    @Override public boolean handleBindMouse(int btn, int action) { return false; }
    @Override public boolean clickGuiFont() { return i.getValue(); }
    @Override public boolean hudFont() { return j.getValue(); }
    @Override public String fontName() { return k.getValue(); }
    @Override public boolean rainbowTheme() { return themeSettingField.getValue() == Theme.RAINBOW; }
    @Override public float rainbowSaturation() { return h.getValue(); }
    @Override public float rainbowBrightness() { return g.getValue(); }
    @Override public int rainbowHue() { return 240; }
    public enum Theme { AMBIENCE, CUSTOM, RAINBOW }
    public enum Layout { CENTER, LEFT, RIGHT }
}
