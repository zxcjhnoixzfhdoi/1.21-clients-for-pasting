package hack.echo.client.chunk;

import hack.echo.client.Echo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class BuiltChunk  {

    @Getter
    private IntArrayList[] vertices;
    @Getter
    private boolean dirty = true;
    @Getter
    private final int[] blockIds;
    @Getter
    private final ObjectList<Cluster> clusterObjectArrayList = new ObjectArrayList<>();

    public final int cx;
    public final int cy;
    public final int cz;
    public BuiltChunk(int cx, int cy, int cz, int[] blockIds) {
        this.blockIds = blockIds;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    public void build() {
        this.vertices = GreedyMesher.buildMesh(this);
        buildClusters();
        dirty = false;
    }

    public void setBlockId(int x, int y, int z, int blockId) {
        int i = computeIndex(x, y, z);
        if (i != -1) {
            dirty |= (blockId != getBlockIds()[i]);
            getBlockIds()[i] = blockId;
        }
    }

    public int getBlockId(int x, int y, int z) {
        int i = computeIndex(x, y, z);
        if (i != -1) {
            return this.blockIds[i];
        }
        return -1;
    }

    public static int computeIndex(int x, int y, int z) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) {
            return -1;
        }
        return ((y << 8) | (z << 4) | x);
    }

    private void buildClusters() {
        clusterObjectArrayList.clear();

        DisjointUnionSets dus = getDisjointUnionSets();

        Int2ObjectMap<IntList> clusters = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < 4096; i++) {
            if (blockIds[i] == 0) continue;
            int root = dus.find(i);
            clusters.computeIfAbsent(root, k -> new IntArrayList()).add(i);
        }

        for (IntList cluster : clusters.values()) {
            double cx = 0;
            double cy = 0;
            double cz = 0;
            int blockId = blockIds[cluster.getInt(0)];

            for (int i : cluster) {
                cx += i & 0xF;
                cy += (i >> 8) & 0xF;
                cz += (i >> 4) & 0xF;
            }

            clusterObjectArrayList.add(new Cluster(
                    cx / cluster.size() + 0.5,
                    cy / cluster.size() + 0.5,
                    cz / cluster.size() + 0.5,
                    blockId
            ));
        }
    }

    private @NotNull DisjointUnionSets getDisjointUnionSets() {
        DisjointUnionSets dus = new DisjointUnionSets();

        for (int i = 0; i < 4096; i++) {
            if (blockIds[i] == 0) continue;

            int x = i & 0xF;
            int z = (i >> 4) & 0xF;
            int y = (i >> 8) & 0xF;

            if (x < 15) {
                int ri = i + 1;
                if (blockIds[ri] == blockIds[i]) dus.union(i, ri);
            }

            if (z < 15) {
                int fi = i + 16;
                if (blockIds[fi] == blockIds[i]) dus.union(i, fi);
            }

            if (y < 15) {
                int ui = i + 256;
                if (blockIds[ui] == blockIds[i]) dus.union(i, ui);
            }
        }
        return dus;
    }


}
