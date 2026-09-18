package hack.echo.client.render2.impl.opengl.api;

import static org.lwjgl.opengl.GL33.*;


/**
 * Sets attribute pointers and divisors
 */
public class GlAttributeBuilder {
    private int stride;
    private int index = 0;

    public GlAttributeBuilder stride(int stride) {
        this.stride = stride;
        return this;
    }

    public GlAttributeBuilder floatAttrib(int size, long pointer) {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, size, GL_FLOAT, false, stride, pointer);
        glVertexAttribDivisor(index, 1);
        index++;
        return this;
    }

    public GlAttributeBuilder intAttrib(int size, long pointer) {
        glEnableVertexAttribArray(index);
        glVertexAttribIPointer(index, size, GL_INT, stride, pointer);
        glVertexAttribDivisor(index, 1);
        index++;
        return this;
    }

    public GlAttributeBuilder mat4Attrib(long offset) {
        for (int i = 0; i < 4; i++) {
            glEnableVertexAttribArray(index);
            glVertexAttribPointer(index, 4, GL_FLOAT, false, stride, offset + i * 16L);
            glVertexAttribDivisor(index, 1);
            index++;
        }
        return this;
    }
}
