package hack.echo.client.utils;

import net.fabricmc.loader.api.FabricLoader;

public class SodiumCompat {
    private static Boolean isSodiumLoaded = null;
    public static boolean isSodiumLoaded() {
        if (isSodiumLoaded == null) {
            isSodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
        }
        return isSodiumLoaded;
    }
}