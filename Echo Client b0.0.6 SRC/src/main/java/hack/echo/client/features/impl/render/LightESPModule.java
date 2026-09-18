package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.registries.BuiltInRegistries;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class LightESPModule extends Feature {

    private final IntSetting maxY = new IntSetting(Concat.of("Max Y"), 0, -64, 319, Concat.of(""));
    private final IntSetting minLightLevel = new IntSetting(Concat.of("Min Light"), 5, 0, 15, Concat.of(""));
    private final ColorSetting baseColor = new ColorSetting(Concat.of("Color"), 255, 128, 0, 200, false);
    {
        maxY.onChanged(this::update);
        minLightLevel.onChanged(this::update);
        baseColor.onChanged(this::update);
    }

    public LightESPModule() {
        super(new FeatureInfo(Concat.of("Light ESP"), Concat.of("Scans for out of place light sources."),
                Category.RENDER, false));
    }

    private void update() {
        if (isNull() || !isEnabled())
            return;

        Object2IntMap<Block> map = new Object2IntOpenHashMap<>();
        int minLight = minLightLevel.getValue();

        for (Block block : BuiltInRegistries.BLOCK) {
            int light = block.defaultBlockState().getLightEmission();
            if (light >= minLight) {
                map.put(block, getThermalColorARGB(light));
            }
        }
        Echo.blockEspManager.enableBlocks(this, map);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        update();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.disableBlocks(this);
    }



    private float[] getThermalColor(int lightLevel) {
        float[] color = new float[4];

        // Smooth linear alpha ramp from minAlpha -> maxAlpha based on light level
        float minAlpha = 0.25f;
        float maxAlpha = 0.70f;
        float tAlpha = Math.min(1.0f, Math.max(0.0f, lightLevel / 15.0f));
        color[3] = minAlpha * (1.0f - tAlpha) + maxAlpha * tAlpha;

        float[] base = new float[] { baseColor.getRedNormalized(), baseColor.getGreenNormalized(),
                baseColor.getBlueNormalized() };

        float r, g, b;

        if (lightLevel <= 5) {
            float intensity = lightLevel / 5.0f;
            r = intensity * 0.4f;
            g = intensity * 0.4f;
            b = intensity * 0.4f;
        } else if (lightLevel <= 10) {
            float intensity = (lightLevel - 5) / 5.0f;
            r = 0.7f + intensity * 0.3f;
            g = intensity * 0.6f;
            b = intensity * 0.1f;
        } else if (lightLevel <= 14) {
            float intensity = (lightLevel - 10) / 4.0f;
            r = 1.0f;
            g = 0.7f + intensity * 0.25f;
            b = 0.2f + intensity * 0.3f;
        } else {
            r = 1.0f;
            g = 1.0f;
            b = 1.0f;
        }

        float accentStrength = 0.7f;
        float tRaw = Math.min(1.0f, Math.max(0.0f, lightLevel / 15.0f));
        float t = tRaw * accentStrength;

        color[0] = base[0] * (1 - t) + r * t;
        color[1] = base[1] * (1 - t) + g * t;
        color[2] = base[2] * (1 - t) + b * t;

        return color;
    }

    private int getThermalColorARGB(int lightLevel) {
        float[] color = getThermalColor(lightLevel);
        return ((int) (color[3] * 255) << 24) |
                ((int) (color[0] * 255) << 16) |
                ((int) (color[1] * 255) << 8) |
                ((int) (color[2] * 255));
    }
}
