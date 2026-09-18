package hack.echo.client.render3.impl.gl;

import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render3.api.Draw3D;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.opengl.GL33.*;

/*
    Renders non grid aligned boxes with varying width and height. i.e entity bounding box
 */
public class GlBox extends GlInstancedShader {

    public GlBox() {
        super("3d/box.frag", "3d/box.vert", 28);
        new GlAttributeBuilder()
                .stride(28)
                .floatAttrib(3, 0)
                .floatAttrib(3, 12)
                .intAttrib(1, 24);
    }

    public void add(double x, double y, double z, double sx, double sy, double sz, int color) {
        checkFlush();
        int pos = buffer.position();
        Vec3 origin = Draw3D.getInstance().origin;
        buffer.putFloat(pos, (float) (x - origin.x));
        buffer.putFloat(pos + 4, (float) (y - origin.y));
        buffer.putFloat(pos + 8, (float) (z - origin.z));
        buffer.putFloat(pos + 12, (float) sx);
        buffer.putFloat(pos + 16, (float) sy);
        buffer.putFloat(pos + 20, (float) sz);
        buffer.putInt(pos + 24, color);
        buffer.position(pos + 28);
        count++;
    }

    @Override
    public void flush() {
        glUseProgram(program);
        glBindVertexArray(vao);
        glDisable(GL_DEPTH_TEST);
        glDrawArraysInstanced(GL_TRIANGLES, 0, 36, flushBuffer());
    }
}
