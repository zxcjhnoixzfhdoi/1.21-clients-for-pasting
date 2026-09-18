package hack.echo.client.render3.impl.gl;

import hack.echo.client.Echo;
import hack.echo.client.chunk.BuiltChunk;
import hack.echo.client.chunk.ChunkManager;
import hack.echo.client.features.impl.misc.VoxelEspConfig;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render3.api.Draw3D;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.RoundingMode;
import org.joml.Vector3i;

import static org.lwjgl.opengl.GL33.*;

/**
 * Optimized voxel renderer for mostly static terrain. I.e Block esp / Search
 */
public class GlVoxelRenderer extends GlInstancedShader implements ChunkManager.ChunkCallback {

    private final FrustumIntersection frustum = new FrustumIntersection();

    private final ChunkManager cm;

    private final int paletteTbo;
    private final int paletteTexture;

    public GlVoxelRenderer() {
        super("3d/voxel.frag", "3d/voxel.vert", 20);
        new GlAttributeBuilder()
                .stride(20)
                .intAttrib(1, 0)
                .floatAttrib(4, 4);

        paletteTbo = glGenBuffers();
        glBindBuffer(GL_TEXTURE_BUFFER, paletteTbo);
        glBufferData(GL_TEXTURE_BUFFER, 4096 * 4L, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_TEXTURE_BUFFER, 0);

        paletteTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, paletteTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32UI, paletteTbo);
        glBindTexture(GL_TEXTURE_BUFFER, 0);

        cm = Echo.chunkManager;
        cm.setCallback(this);
        this.onPaletteChange(cm.getPalette());
    }

    @Override
    public void flush() {
        if (cm.isEmpty()) {
            return;
        }

        Draw3D draw3D = Draw3D.getInstance();
        Vec3 camPos = draw3D.origin;
        Vector3i camChunk = new Vector3i(camPos.x / 16.0, camPos.y / 16.0, camPos.z / 16.0, RoundingMode.FLOOR);

        frustum.set(draw3D.mvp);

        glUseProgram(program);
        glBindVertexArray(vao);

        uniform1i("fill", VoxelEspConfig.isFill() ? 1 : 0);
        uniform1i("outline", VoxelEspConfig.isOutline() ? 1 : 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_BUFFER, paletteTexture);
        uniform1i("palette", 0);

        glDisable(GL_BLEND);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        buffer.clear();


        BuiltChunk[] chunks = cm.getDense();
        int chunkCount = cm.getCount();
        for (int ci = 0; ci < chunkCount; ci++) {
            BuiltChunk chunk = chunks[ci];
            float minX = (float) (chunk.cx * 16.0 - camPos.x);
            float minY = (float) (chunk.cy * 16.0 - camPos.y);
            float minZ = (float) (chunk.cz * 16.0 - camPos.z);
            float maxX = minX + 16f;
            float maxY = minY + 16f;
            float maxZ = minZ + 16f;

            if (!frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
                continue;
            }

            IntArrayList[] chunkVertices = chunk.getVertices();
            if (chunkVertices == null) {
                continue;
            }
            int[] delta = {chunk.cy - camChunk.y, chunk.cx - camChunk.x, chunk.cz - camChunk.z};

            for (int i = 0; i < 6; i++) {
                var vertices = chunkVertices[i];
                if (vertices == null || vertices.isEmpty()) {
                    continue;
                }

                int axis = i / 2;
                int sign = ((i & 1) << 1) - 1;
                if (delta[axis] * sign > 0) {
                    continue;
                }
                int vertexCount = vertices.size();

                for (int j = 0; j < vertexCount; j++) {
                    checkFlush();
                    buffer.putInt(vertices.getInt(j));
                    buffer.putFloat(minX);
                    buffer.putFloat(minY);
                    buffer.putFloat(minZ);
                    buffer.putFloat(i);
                    this.count++;
                }
            }
        }

        draw(this.count);
        glEnable(GL_BLEND);
    }

    private void draw(long instanceCount) {
        if (instanceCount == 0) {
            return;
        }
        buffer.flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, (int) instanceCount);
        buffer.clear();
        this.count = 0;
    }

    @Override
    public void onChunkSet(int index, BuiltChunk chunk) {
    }

    @Override
    public void onChunkUpdated(int index, BuiltChunk chunk) {
    }

    @Override
    public void onChunkRemoved(int index) {
    }

    @Override
    public void onClear() {
    }

    @Override
    public void onPaletteChange(int[] palette) {
        if (palette == null || palette.length == 0) {
            return;
        }

        glBindBuffer(GL_TEXTURE_BUFFER, paletteTbo);
        glBufferSubData(GL_TEXTURE_BUFFER, 0, palette);
        glBindBuffer(GL_TEXTURE_BUFFER, 0);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        glDeleteBuffers(paletteTbo);
        glDeleteTextures(paletteTexture);
    }
}
