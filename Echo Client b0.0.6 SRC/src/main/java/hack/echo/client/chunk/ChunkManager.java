package hack.echo.client.chunk;

import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.api.ChunkPosCompat;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;

import static hack.echo.client.utils.Imports.mc;

public class ChunkManager {

    public interface ChunkCallback {
        void onChunkSet(int index, BuiltChunk chunk);

        void onChunkUpdated(int index, BuiltChunk chunk);
        void onChunkRemoved(int index);
        void onClear();
        void onPaletteChange(int[] palette);
    }

    private BuiltChunk[] dense = new BuiltChunk[4096];
    private int count = 0;
    private final Long2IntOpenHashMap sparse = new Long2IntOpenHashMap();
    @Getter
    private final int[] palette = new int[4096];
    @Setter
    @Getter
    private ChunkCallback callback;

    @Getter
    private final int maxChunks = 200_000;
    public ChunkManager() {
        sparse.defaultReturnValue(-1);
        EventManager.register(this);
    }

    @EventSubscribe
    public void onTick(EventTick event) {
        rebuildDirty();
    }

    public void callbackPalette() {
        if (callback != null) callback.onPaletteChange(this.palette);
    }

    public static long getKey(int cx, int cy, int cz) {
        return ((long) (cx & 0x1FFFFF) << 42) |
                ((long) (cy & 0x1FFFFF) << 21) |
                ((long) (cz & 0x1FFFFF));
    }

    public synchronized void addChunk(int[] blockIds, int cx, int cy, int cz) {
        if (count >= maxChunks) return;
        long key = getKey(cx, cy, cz);
        int existing = sparse.get(key);
        if (existing != -1) {
            dense[existing] = new BuiltChunk(cx, cy, cz, blockIds);
            if (callback != null) callback.onChunkUpdated(existing, dense[existing]);
            return;
        }

        if (count >= dense.length) grow();
        dense[count] = new BuiltChunk(cx, cy, cz, blockIds);
        sparse.put(key, count);
        count++;
        if (callback != null) callback.onChunkSet(count - 1, dense[count - 1]);
    }

    public synchronized void addBlock(int x, int y, int z, int blockId) {
        int cx = Math.floorDiv(x, 16);
        int cy = Math.floorDiv(y, 16);
        int cz = Math.floorDiv(z, 16);

        long key = getKey(cx, cy, cz);
        int index = sparse.get(key);
        BuiltChunk chunk;
        if (index == -1) {
            if (count >= maxChunks) return;

            if (count >= dense.length) grow();
            chunk = new BuiltChunk(cx, cy, cz, new int[4096]);
            dense[count] = chunk;
            sparse.put(key, count);
            count++;
            if (callback != null) callback.onChunkSet(count - 1, chunk);
        } else {
            chunk = dense[index];
        }
        chunk.setBlockId(x & 15, y & 15, z & 15, blockId);
    }

    public synchronized void removeBlock(int x, int y, int z) {
        int cx = Math.floorDiv(x, 16);
        int cy = Math.floorDiv(y, 16);
        int cz = Math.floorDiv(z, 16);

        int index = sparse.get(getKey(cx, cy, cz));
        if (index == -1) return;
        dense[index].setBlockId(x & 15, y & 15, z & 15, 0);
    }

    public synchronized void removeChunk(int cx, int cy, int cz) {
        long key = getKey(cx, cy, cz);
        int index = sparse.get(key);
        if (index == -1) return;

        count--;
        if (index != count) {
            BuiltChunk moved = dense[count];
            dense[index] = moved;
            sparse.put(getKey(moved.cx, moved.cy, moved.cz), index);
            if (callback != null) callback.onChunkSet(index, moved);
        }
        dense[count] = null;
        sparse.remove(key);
        if (callback != null) callback.onChunkRemoved(count);
    }

    public synchronized void removeChunk(ChunkPos pos) {
        assert mc.level != null;
        for (int sectionY = mc.level.getMinY() >> 4; sectionY < mc.level.getMaxY() >> 4; sectionY++) {
            removeChunk(ChunkPosCompat.x(pos), sectionY, ChunkPosCompat.z(pos));
        }
    }


    public synchronized void rebuildDirty() {
        for (int i = 0; i < count; i++) {
            BuiltChunk c = dense[i];
            if (c.isDirty()) {
                c.build();
                if (callback != null) callback.onChunkUpdated(i, c);
            }
        }
    }

    public synchronized void clear() {
        Arrays.fill(dense, 0, count, null);
        sparse.clear();
        count = 0;
        if (callback != null) callback.onClear();
    }

    public synchronized BuiltChunk[] getDense() {
        return dense;
    }

    public synchronized int getCount() {
        return count;
    }

    public synchronized boolean isEmpty() {
        return count == 0;
    }

    private void grow() {
        dense = Arrays.copyOf(dense, dense.length * 2);
    }
}
