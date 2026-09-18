package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;

public class FullbrightModule extends Feature {
    private static volatile FullbrightModule activeInstance;

    public FullbrightModule() {
        super(new FeatureInfo(
            Concat.of("Full Bright"),
            Concat.of("Full Bright"),
            Category.RENDER)
        );
    }

    @Override
    public void onEnable() {
        activeInstance = this;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.onDisable();
    }

    public static boolean isFullbrightEnabled() {
        if (Echo.isDestroyed) {
            return false;
        }

        FullbrightModule fullbright = activeInstance;
        return fullbright != null && fullbright.isEnabled();
    }
}
