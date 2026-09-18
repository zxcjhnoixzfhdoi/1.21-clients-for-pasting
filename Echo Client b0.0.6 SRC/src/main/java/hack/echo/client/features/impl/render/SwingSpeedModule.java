package hack.echo.client.features.impl.render;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.utils.strings.Concat;

public class SwingSpeedModule extends Feature {

    public SwingSpeedModule() {
        super(new FeatureInfo(
            Concat.of("Swing Speed"),
            Concat.of("Increases the speed of your swing animation."),
            Category.RENDER,
            false
        ));
    }

    public static final IntSetting swingSpeed = new IntSetting(Concat.of("Swing Speed"), 7, 1, 20);

}
