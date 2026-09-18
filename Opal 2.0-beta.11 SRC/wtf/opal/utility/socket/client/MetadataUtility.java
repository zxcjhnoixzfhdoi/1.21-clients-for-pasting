package wtf.opal.utility.socket.client;

import net.fabricmc.loader.api.FabricLoader;
import wtf.opal.client.ReleaseInfo;
import wtf.opal.protection.annotation.NativeInclude;

@NativeInclude
public final class MetadataUtility {

    private MetadataUtility() {
    }

    public static String getAgent() {
        return FabricLoader.getInstance().getModContainer("opal").orElseThrow().getMetadata().getId() + "/" + ReleaseInfo.VERSION;
    }

}
