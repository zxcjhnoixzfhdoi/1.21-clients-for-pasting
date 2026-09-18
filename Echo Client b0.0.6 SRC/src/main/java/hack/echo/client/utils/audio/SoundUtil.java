package hack.echo.client.utils.audio;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.misc.ClickGUI;
import hack.echo.client.utils.Imports;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

// By Cyde.
public class SoundUtil implements Imports {

    private static ClickGUI getClickGUI() {
        if (Echo.featureManager == null) return null;
        return Echo.featureManager.getFeatureByClass(ClickGUI.class);
    }

    private static float applyMasterVolume(ClickGUI g, float volume) {
        return volume * g.soundMasterVolume.getValue();
    }

    public static void playClick() {
        ClickGUI g = getClickGUI();
        if (g != null && g.clickEnabled.getValue())
            EchoAudio.play("click", applyMasterVolume(g, g.clickVolume.getValue()), g.clickPitch.getValue());
    }

    public static void playToggle(boolean enabled) {
        ClickGUI g = getClickGUI();
        if (g != null && g.toggleEnabled.getValue())
            EchoAudio.play("toggle", applyMasterVolume(g, g.toggleVolume.getValue()), g.togglePitch.getValue());
    }

    public static void playExpand(boolean expanded) {
        ClickGUI g = getClickGUI();
        if (g != null && g.expandEnabled.getValue())
            EchoAudio.play("toggle", applyMasterVolume(g, g.expandVolume.getValue()), g.expandPitch.getValue());
    }

    public static void playHover() {
        ClickGUI g = getClickGUI();
        if (g != null && g.hoverEnabled.getValue())
            EchoAudio.play("keypress", applyMasterVolume(g, g.hoverVolume.getValue()), g.hoverPitch.getValue());
    }

    public static void playKeypress() {
        ClickGUI g = getClickGUI();
        if (g != null && g.keypressEnabled.getValue())
            EchoAudio.play("keypress", applyMasterVolume(g, g.keypressVolume.getValue()), g.keypressPitch.getValue());
    }

    public static void playScroll() {
        ClickGUI g = getClickGUI();
        if (g != null && g.scrollEnabled.getValue())
            EchoAudio.play("scroll", applyMasterVolume(g, g.scrollVolume.getValue()), g.scrollPitch.getValue());
    }

    public static void playSliderChange() {
        ClickGUI g = getClickGUI();
        if (g != null && g.sliderChangeEnabled.getValue())
            EchoAudio.play("keypress", applyMasterVolume(g, g.sliderChangeVolume.getValue()), g.sliderChangePitch.getValue());
    }

    public static void playColorChange() {
        ClickGUI g = getClickGUI();
        if (g != null && g.colorChangeEnabled.getValue())
            EchoAudio.play("keypress", applyMasterVolume(g, g.colorChangeVolume.getValue()), g.colorChangePitch.getValue());
    }

    public static void playSound(SoundEvent soundEvent, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch));
    }

    public static void playSound(SoundEvent soundEvent, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch, volume));
    }
}
