package hack.echo.client.render2.impl.opengl.api;

import hack.echo.client.Echo;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public abstract class GlInstancedShader extends GlShader {

    private static final float GROW_FACTOR = 2f;

    protected final int stride;
    protected final int vbo;
    protected ByteBuffer buffer;
    protected int max;
    protected int count = 0;

    protected GlInstancedShader(String fragResource, String vertResource, int bytes) {
        super(fragResource, vertResource);
        this.stride = bytes;
        this.max = (1 << 12) / bytes;
        this.buffer = BufferUtils.createByteBuffer(1 << 12);

        this.vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 1 << 12, GL_DYNAMIC_DRAW);
    }

    public abstract void flush();

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(program);
        buffer.clear();
    }

    protected void checkFlush() {
        if (count >= max) grow();
    }

    private void grow() {
        int newMax = (int) (max * GROW_FACTOR);
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newMax * stride);

        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;

        max = newMax;

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) newMax * stride, GL_DYNAMIC_DRAW);
    }

    protected int flushBuffer() {
        buffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        buffer.clear();
        int c = count;
        count = 0;
        return c;
    }
}