
package hack.echo.client.features.impl.misc;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.api.ChatCompat;
import hack.echo.client.utils.Exporter;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.chat.Component;

public class Export extends Feature {

    public Export() {
        super(new FeatureInfo(
            Concat.of("Export"),
            Concat.of("Export configurations"),
            Category.INTERNALS
        ));
    }
    //? if debug {
    /*@Override
    public void onEnable() {
        Exporter.saveExport(Exporter.collect());
        
        if (mc.gui != null) {
            ChatCompat.addMessage(
                Component.literal("§a[Echo] §7Lowk exported features to export.json")
            );
        }
        setEnabled(false);
    }
    *///?}
}

