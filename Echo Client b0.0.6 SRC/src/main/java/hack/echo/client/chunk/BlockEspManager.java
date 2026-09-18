package hack.echo.client.chunk;

import hack.echo.client.Echo;
import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.impl.misc.VoxelEspConfig;
import hack.echo.client.utils.Imports;
import hack.echo.client.api.ChunkPosCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class BlockEspManager implements Imports {

    private final Map<Object, Object2IntMap<Block>> enabledBlocks = new HashMap<>();
    private final ChunkManager chunkManager;

    private final ExecutorService threadPool = Executors.newWorkStealingPool();

    public BlockEspManager() {
        EventManager.register(this);
        chunkManager = Echo.chunkManager;
        rebuildPalette();
    }

    @EventSubscribe
    public void event(EventChunkLoad event) {
        var c = event.getChunk();
        chunkManager.removeChunk(c.getPos());
        threadPool.submit(() -> searchChunk(c));
    }

    @EventSubscribe
    public void event(EventChunkUnload event) {
        chunkManager.removeChunk(event.getChunkPos());
    }

    @EventSubscribe
    public void event(EventBlockUpdate event) {
        var pos = event.getPos();
        var block = event.getState().getBlock();
        int blockId = BuiltInRegistries.BLOCK.getId(block);

        if (blockId != 0 && chunkManager.getPalette()[blockId] != 0) {
            chunkManager.addBlock(pos.getX(), pos.getY(), pos.getZ(), blockId);
        } else {
            chunkManager.removeBlock(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @EventSubscribe
    public void event(EventLevelChange event) {
        chunkManager.clear();
        if (!enabledBlocks.isEmpty()) {
            searchWorld();
        }
    }

    @EventSubscribe
    public void event(EventRender3D eventRender3D) {
        if (VoxelEspConfig.hasTracers()) {
            var draw = eventRender3D.getDraw3D().getCustomTarget();
            int[] palette = chunkManager.getPalette();
            BuiltChunk[] dense = chunkManager.getDense();
            int chunkCount = chunkManager.getCount();
            for (int ci = 0; ci < chunkCount; ci++) {
                BuiltChunk chunk = dense[ci];
                for (Cluster cluster : chunk.getClusterObjectArrayList()) {
                    if (cluster.blockId() == 0) continue;
                    int x = chunk.cx << 4;
                    int y = chunk.cy << 4;
                    int z = chunk.cz << 4;
                    draw.tracer(
                            x + cluster.cx(),
                            y + cluster.cy(),
                            z + cluster.cz(),
                            255 << 24 | palette[cluster.blockId()]
                    );
                }
            }
        }
    }


    public void enableBlocks(Object from, Iterable<Block> blocks, int color) {
        Object2IntMap<Block> map = new Object2IntOpenHashMap<>();
        for (Block block : blocks) {
            map.put(block, color);
        }
        enabledBlocks.put(from, map);
        rebuildPalette();
        searchWorld();
    }

    public void enableBlocks(Object from, Object2IntMap<Block> blocks) {
        enabledBlocks.put(from, blocks);
        rebuildPalette();
        searchWorld();
    }

    public void updateColor(Object from, int color) {
        Object2IntMap<Block> map = enabledBlocks.get(from);
        if (map == null) return;
        map.replaceAll((block, old) -> color);
        rebuildPalette();
    }

    public void updateAlpha(Object from, int alpha) {
        Object2IntMap<Block> map = enabledBlocks.get(from);
        if (map == null) return;
        map.replaceAll((block, old) -> (alpha << 24) | (old & 0x00FFFFFF));
        rebuildPalette();
    }

    public void disableBlocks(Object from) {
        if (enabledBlocks.remove(from) != null) {
            rebuildPalette();
            searchWorld();
        }
    }

    public synchronized void rebuildPalette() {
        var colors = chunkManager.getPalette();
        Arrays.fill(colors, 0);
        var registry = BuiltInRegistries.BLOCK;

        for (var map : enabledBlocks.values()) {
            for (var entry : map.object2IntEntrySet()) {
                int id = registry.getId(entry.getKey());
                colors[id] = entry.getIntValue();
            }
        }
        chunkManager.callbackPalette();
    }

    public void searchWorld() {
        chunkManager.clear();
        threadPool.submit(() -> {
            var chunks = mc.level.getChunkSource().storage.chunks;
            chunkManager.clear();
            IntStream.range(0, chunks.length())
                    .parallel()
                    .mapToObj(chunks::get)
                    .filter(Objects::nonNull)
                    .forEach(this::searchChunk);
        });
    }

    public void searchChunk(LevelChunk chunk) {
        var sections = chunk.getSections();
        var chunkPos = chunk.getPos();
        var registry = BuiltInRegistries.BLOCK;
        var palette = chunkManager.getPalette();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section == null || section.hasOnlyAir())
                continue;

            var states = section.getStates();

            if (!states.maybeHas(s -> {
                int id = registry.getId(s.getBlock());
                return palette[id] != 0;
            })) {
                continue;
            }

            int[] blockIds = new int[4096];
            for (int j = 0; j < 4096; j++) {
                var state = states.get(j);
                int id = registry.getId(state.getBlock());
                blockIds[j] = (id != 0 && palette[id] != 0) ? id : 0;
            }

            chunkManager.addChunk(blockIds, ChunkPosCompat.x(chunkPos), chunk.getSectionYFromSectionIndex(i), ChunkPosCompat.z(chunkPos));
        }
    }

}
