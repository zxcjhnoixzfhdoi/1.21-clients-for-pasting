package hack.echo.client.features.impl.render;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.api.ClientLevelCompat;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import hack.echo.client.utils.strings.Concat;

public class AmbienceMod extends Feature {

	public AmbienceMod() {
		super(new FeatureInfo(
			Concat.of("Ambience"),
			Concat.of("Modifies world ambience"),
			Category.RENDER
		));
	}

    private static final BoolSetting ChangeTime = new BoolSetting(Concat.of("Custom time"),  false);
    private static final IntSetting Time = new IntSetting(Concat.of("Time"), 18000, 0, 24000, o -> ChangeTime.getValue());
    private static final BoolSetting ChangeWeather = new BoolSetting(Concat.of("Modify weather"), false);
    private static final ModeSetting Weather = new ModeSetting(
        Concat.of("Weather"),
        Concat.of("Clear"),
        Concat.of("Clear"),
        Concat.of("Rain"),
        Concat.of("Thunder")
    );

    private static final BoolSetting ChangeFogColor = new BoolSetting(Concat.of("Fog color"), false);
    private static final ColorSetting FogColor = new ColorSetting(Concat.of("Fog color"), 90, 220, 255, 255, o -> ChangeFogColor.getValue(), false);
    private static final BoolSetting GradientFogColor = new BoolSetting(Concat.of("Gradient"), true, o -> ChangeFogColor.getValue());
    private static final ColorSetting FogGradientColor = new ColorSetting(Concat.of("Gradient color"), 12, 35, 92, 255, o -> ChangeFogColor.getValue() && GradientFogColor.getValue(), false);
    private static final IntSetting FogRenderDistance = new IntSetting(Concat.of("Fog distance"), 6, 0, 48, o -> ChangeFogColor.getValue());

    public static boolean isChangeTimeEnabled() { return ChangeTime.getValue(); }
    public static long getCustomTime() { return Time.getValue(); }
    public static boolean isChangeWeatherEnabled() { return ChangeWeather.getValue(); }
    public static CharSequence getWeather() { return Weather.getValue(); }
    public static CharSequence getWeatherSequence() { return Weather.getValueSequence(); }
    public static boolean isChangeFogColorEnabled() { return ChangeFogColor.getValue(); }
    public static boolean isFogGradientEnabled() { return GradientFogColor.getValue(); }
    public static int getFogColorR() { return FogColor.getRed(); }
    public static int getFogColorG() { return FogColor.getGreen(); }
    public static int getFogColorB() { return FogColor.getBlue(); }
    public static int getFogGradientColorR() { return FogGradientColor.getRed(); }
    public static int getFogGradientColorG() { return FogGradientColor.getGreen(); }
    public static int getFogGradientColorB() { return FogGradientColor.getBlue(); }
    public static float getFogRenderDistance() { return (float) FogRenderDistance.getValue(); }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void PacketEventListener(EventPacketReceive event) {
        if (isNull()) return;

        if (event.getPacket() instanceof ClientboundSetTimePacket packet) {
            if (ChangeTime.getValue()) {
                ClientLevelCompat.setTimeFromServer(mc.level, getCustomTime());
                event.cancel();
            }

            if (ChangeWeather.getValue()) {
                if (Weather.is(Concat.of("Clear"))) {
                    mc.level.setRainLevel(0.0f);
                    mc.level.setThunderLevel(0.0f);
                } else if (Weather.is(Concat.of("Rain"))) {
                    mc.level.setRainLevel(1.0f);
                    mc.level.setThunderLevel(0.0f);
                } else if (Weather.is(Concat.of("Thunder"))) {
                    mc.level.setRainLevel(1.0f);
                    mc.level.setThunderLevel(1.0f);
                } else if (Weather.is(Concat.of("Snow"))) {
                    mc.level.setRainLevel(0.7f);
                    mc.level.setThunderLevel(0.0f);
                }
                event.cancel();
            } else {
                mc.level.setRainLevel(0.0f);
                mc.level.setThunderLevel(0.0f);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.level != null) {
            mc.level.setRainLevel(0.0f);
            mc.level.setThunderLevel(0.0f);
        }
    }
}
