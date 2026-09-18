package hack.echo.client.features.impl.misc;

import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.screens.clickgui.glass.GlassUIConstants;
import hack.echo.client.utils.strings.Concat;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Feature {

    public ClickGUI() {
        super(new FeatureInfo(
            Concat.of("Click GUI"),
            Concat.of("Click-based GUI"),
            Category.INTERNALS,
            GLFW.GLFW_KEY_RIGHT_SHIFT
        ));
    }
    
    public final FloatSetting glassScrollSpeed = new FloatSetting(Concat.of("Scroll Speed"), 50.0f, 1.0f, 50.0f, 1.0f);
    public final BoolSetting glassDarkMode = new BoolSetting(Concat.of("Dark Mode"), true);
    {
        glassDarkMode.onChanged(GlassUIConstants::syncTheme);
    }
    
    // Click sound settings
    public final FloatSetting soundMasterVolume = new FloatSetting(Concat.of("Sound Master Volume"), 1.0f, 0.0f, 1.0f, 0.1f);
    public final BoolSetting clickEnabled = new BoolSetting(Concat.of("Click Sound"), true);
    public final FloatSetting clickVolume = new FloatSetting(Concat.of("Click Volume"), 1.0f, 0.0f, 1.0f, 0.1f, o -> clickEnabled.getValue());
    public final FloatSetting clickPitch = new FloatSetting(Concat.of("Click Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> clickEnabled.getValue());
    
    // Toggle sound settings
    public final BoolSetting toggleEnabled = new BoolSetting(Concat.of("Toggle Sound"), true);
    public final FloatSetting toggleVolume = new FloatSetting(Concat.of("Toggle Volume"), 1.0f, 0.0f, 1.0f, 0.1f, o -> toggleEnabled.getValue());
    public final FloatSetting togglePitch = new FloatSetting(Concat.of("Toggle Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> toggleEnabled.getValue());
    
    // Expand sound settings
    public final BoolSetting expandEnabled = new BoolSetting(Concat.of("Expand Sound"), true);
    public final FloatSetting expandVolume = new FloatSetting(Concat.of("Expand Volume"), 1.0f, 0.0f, 1.0f, 0.1f, o -> expandEnabled.getValue());
    public final FloatSetting expandPitch = new FloatSetting(Concat.of("Expand Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> expandEnabled.getValue());
    
    // Hover sound settings
    public final BoolSetting hoverEnabled = new BoolSetting(Concat.of("Hover Sound"), true);
    public final FloatSetting hoverVolume = new FloatSetting(Concat.of("Hover Volume"), 0.9f, 0.0f, 1.0f, 0.1f, o -> hoverEnabled.getValue());
    public final FloatSetting hoverPitch = new FloatSetting(Concat.of("Hover Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> hoverEnabled.getValue());
    
    // Keypress sound settings
    public final BoolSetting keypressEnabled = new BoolSetting(Concat.of("Keypress Sound"), true);
    public final FloatSetting keypressVolume = new FloatSetting(Concat.of("Keypress Volume"), 0.7f, 0.0f, 1.0f, 0.1f, o -> keypressEnabled.getValue());
    public final FloatSetting keypressPitch = new FloatSetting(Concat.of("Keypress Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> keypressEnabled.getValue());
    
    // Scroll sound settings
    public final BoolSetting scrollEnabled = new BoolSetting(Concat.of("Scroll Sound"), true);
    public final FloatSetting scrollVolume = new FloatSetting(Concat.of("Scroll Volume"), 0.4f, 0.0f, 1.0f, 0.1f, o -> scrollEnabled.getValue());
    public final FloatSetting scrollPitch = new FloatSetting(Concat.of("Scroll Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> scrollEnabled.getValue());
    
    // Slider change sound settings
    public final BoolSetting sliderChangeEnabled = new BoolSetting(Concat.of("Slider Change Sound"), true);
    public final FloatSetting sliderChangeVolume = new FloatSetting(Concat.of("Slider Change Volume"), 0.7f, 0.0f, 1.0f, 0.1f, o -> sliderChangeEnabled.getValue());
    public final FloatSetting sliderChangePitch = new FloatSetting(Concat.of("Slider Change Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> sliderChangeEnabled.getValue());
    
    // Color change sound settings
    public final BoolSetting colorChangeEnabled = new BoolSetting(Concat.of("Color Change Sound"), true);
    public final FloatSetting colorChangeVolume = new FloatSetting(Concat.of("Color Change Volume"), 0.7f, 0.0f, 1.0f, 0.1f, o -> colorChangeEnabled.getValue());
    public final FloatSetting colorChangePitch = new FloatSetting(Concat.of("Color Change Pitch"), 1.0f, 0.1f, 2.0f, 0.1f, o -> colorChangeEnabled.getValue());
    

    //FIXME : profile loading causes this to get called. Which sets minecraft screen before mouseHandler has been created
    @Override
    public void onEnable() {
        try {
            Echo.screenManager.displayClickGUI();
            this.setEnabled(false);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isEnabled() {
        return Echo.screenManager.isClickGUIOpen();
    }

    @Override
    protected boolean shouldNotifyToggle() {
        return false;
    }

    public float getScrollSpeed() {
        return glassScrollSpeed.getValue();
    }
}
