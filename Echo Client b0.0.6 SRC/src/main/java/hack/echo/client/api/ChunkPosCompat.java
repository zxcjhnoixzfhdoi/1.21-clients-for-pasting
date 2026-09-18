package hack.echo.client.api;

import net.minecraft.world.level.ChunkPos;

public final class ChunkPosCompat {

    private ChunkPosCompat() {
    }

    public static int x(ChunkPos chunkPos) {
        //? if >=26.1 {
        /*return chunkPos.x();
        *///?} else {
        return chunkPos.x;
        //?}
    }

    public static int z(ChunkPos chunkPos) {
        //? if >=26.1 {
        /*return chunkPos.z();
        *///?} else {
        return chunkPos.z;
        //?}
    }
}
