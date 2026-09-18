package hack.echo.client.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.HashMap;
import java.util.Map;


/**
 * Very complicated greedy mesher
 * @see <a href="https://www.youtube.com/watch?v=qnGoGq7DWMc">Explanation</a>
 */
public class GreedyMesher {

    private static final byte CHUNK_SIZE = 16;
    private static final byte CHUNK_SIZE_P = 18;
    private static final int AIR = 0;

    private static void buildAxisCols(int[] blockIds, int[][][] axisCols) {
        for (int i = 0; i < 4096; i++) {
            int blockId = blockIds[i];
            if (blockId == AIR)
                continue;

            int x = i & 15;
            int z = (i >> 4) & 15;
            int y = (i >> 8) & 15;

            int px = x + 1;
            int py = y + 1;
            int pz = z + 1;

            axisCols[0][pz][px] |= (1 << py);
            axisCols[1][py][pz] |= (1 << px);
            axisCols[2][py][px] |= (1 << pz);
        }
    }

    private static void buildFaceMasks(int[][][] axisCols, int[][][] faceMasks) {
        for (int axis = 0; axis < 3; axis++) {
            for (int i = 0; i < CHUNK_SIZE_P; i++) {
                for (int j = 0; j < CHUNK_SIZE_P; j++) {
                    int col = axisCols[axis][i][j];
                    faceMasks[axis * 2][i][j] = col & ~(col << 1);
                    faceMasks[axis * 2 + 1][i][j] = col & ~(col >>> 1);
                }
            }
        }
    }

    public static IntArrayList[] buildMesh(BuiltChunk chunk) {
        var vertexDataList = new IntArrayList[6];

        for (int i = 0; i < 6; i++) {
            vertexDataList[i] = new IntArrayList();
        }

        int[][][] axisCols = new int[3][CHUNK_SIZE_P][CHUNK_SIZE_P];
        buildAxisCols(chunk.getBlockIds(), axisCols);

        int[][][] faceMasks = new int[6][CHUNK_SIZE_P][CHUNK_SIZE_P];
        buildFaceMasks(axisCols, faceMasks);

        @SuppressWarnings("unchecked")
        Map<Integer, Map<Byte, int[]>>[] data = new Map[6];
        for (int i = 0; i < 6; i++) {
            data[i] = new HashMap<>();
        }

        for (int axis = 0; axis < 6; axis++) {
            int projAxis = axis / 2;

            for (byte p1 = 0; p1 < CHUNK_SIZE; p1++) {
                for (byte p2 = 0; p2 < CHUNK_SIZE; p2++) {
                    int col = faceMasks[axis][p2 + 1][p1 + 1];

                    col >>= 1;
                    col &= ~(1 << CHUNK_SIZE);

                    while (col != 0) {
                        byte axis_dim = (byte) Integer.numberOfTrailingZeros(col);
                        col &= col - 1;

                        byte x, y, z;
                        byte axis_pos, plane_p1, plane_p2;

                        switch (projAxis) {
                            case 0 -> {
                                x = p1;
                                y = axis_dim;
                                z = p2;
                                axis_pos = y;
                                plane_p1 = x;
                                plane_p2 = z;
                            }
                            case 1 -> {
                                y = p2;
                                z = p1;
                                x = axis_dim;
                                axis_pos = x;
                                plane_p1 = y;
                                plane_p2 = z;
                            }
                            default -> {
                                x = p1;
                                y = p2;
                                z = axis_dim;
                                axis_pos = z;
                                plane_p1 = x;
                                plane_p2 = y;
                            }
                        }

                        int blockId = chunk.getBlockId(x, y, z);

                        Map<Byte, int[]> planeMap = data[axis].computeIfAbsent(blockId, k -> new HashMap<>());
                        int[] plane = planeMap.computeIfAbsent(axis_pos, k -> new int[CHUNK_SIZE]);
                        plane[plane_p1] |= (1 << plane_p2);
                    }
                }
            }
        }

        for (int axis = 0; axis < 6; axis++) {

            for (Map.Entry<Integer, Map<Byte, int[]>> blockIdEntry : data[axis].entrySet()) {
                int blockId = blockIdEntry.getKey();

                for (Map.Entry<Byte, int[]> planeEntry : blockIdEntry.getValue().entrySet()) {
                    byte axisPos = planeEntry.getKey();
                    int[] plane = planeEntry.getValue();

                    for (byte row = 0; row < CHUNK_SIZE; row++) {
                        byte y2 = 0;
                        while (y2 < CHUNK_SIZE) {
                            int mask = (plane[row] >>> y2);
                            if (mask == 0)
                                break;

                            y2 += (byte) Integer.numberOfTrailingZeros(mask);
                            if (y2 >= CHUNK_SIZE)
                                continue;

                            byte h = (byte) Integer.numberOfTrailingZeros(~(plane[row] >>> y2));
                            if (y2 + h > CHUNK_SIZE)
                                h = (byte) (CHUNK_SIZE - y2);

                            int hAsMask = (1 << h) - 1;

                            int w = 1;
                            while (row + w < CHUNK_SIZE) {
                                int nextRowH = (plane[row + w] >>> y2) & hAsMask;
                                if (nextRowH != hAsMask)
                                    break;

                                plane[row + w] &= ~(hAsMask << y2);
                                w++;
                            }

                            byte x, y, z;

                            if (axis < 2) {
                                x = row;
                                y = axisPos;
                                z = y2;
                            } else if (axis < 4) {
                                x = axisPos;
                                y = row;
                                z = y2;
                            } else {
                                x = row;
                                y = y2;
                                z = axisPos;
                            }

                            int Data = 0;
                            Data |= (x & 0xF);
                            Data |= (y & 0xF) << 4;
                            Data |= (z & 0xF) << 8;
                            Data |= ((w - 1) & 0xF) << 12;
                            Data |= ((h - 1) & 0xF) << 16;
                            Data |= (blockId & 0xFFF) << 20;
                            vertexDataList[axis].add(Data);

                            y2 += h;
                        }
                    }
                }
            }
        }

        return vertexDataList;
    }
}