package hack.echo.client.features.impl.misc;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;

public class ClientThemes extends Feature {

    public ClientThemes() {
        super(new FeatureInfo(
            Concat.of("Client Themes"),
            Concat.of("Client color themes"),
            Category.INTERNALS
        ));
    }

    
}
