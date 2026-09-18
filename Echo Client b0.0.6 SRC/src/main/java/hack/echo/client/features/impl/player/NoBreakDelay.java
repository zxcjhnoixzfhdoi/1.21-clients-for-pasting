package hack.echo.client.features.impl.player;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;

public class NoBreakDelay extends Feature {

    public NoBreakDelay() {
        super(new FeatureInfo(
                Concat.of("No Break Delay"),
                Concat.of("Removes block break delay"),
                Category.UTILITY
        ));
    }
}