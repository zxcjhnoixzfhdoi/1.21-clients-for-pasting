package hack.echo.client.features.impl.misc;

import hack.echo.client.chunk.BlockEspManager;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.utils.strings.Concat;

public class VoxelEspConfig extends Feature {
    private static VoxelEspConfig instance;
    public VoxelEspConfig() {
        super(new FeatureInfo(
                Concat.of("Voxel Config"),
                Concat.of("Rendering configs for voxel based esp"),
                Category.INTERNALS
        ));
        instance = this;
        modeSetting.setOption(Concat.of("Fill"), true);
        modeSetting.setOption(Concat.of("Outline"), true);
    }

    private final MultiModeSetting modeSetting = new MultiModeSetting(Concat.of("Mode"), Concat.of("Fill"), Concat.of("Outline"));
    private final BoolSetting tracers = new BoolSetting(Concat.of("Tracers"), false);
    public static boolean isFill() {
        return instance.modeSetting.isEnabled(Concat.of("Fill"));
    }

    public static boolean isOutline() {
        return instance.modeSetting.isEnabled(Concat.of("Outline"));
    }

    public static boolean hasTracers() {
        return instance.tracers.getValue();
    }
}
