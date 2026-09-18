package wtf.opal;

import net.fabricmc.api.ClientModInitializer;
import wtf.opal.client.OpalClient;
import wtf.opal.protection.annotation.NativeInclude;

@NativeInclude
public final class OpalFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OpalClient.setInstance();
    }

}
