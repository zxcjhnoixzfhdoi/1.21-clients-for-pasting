package hack.echo.client.render3.impl.gl;

import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render3.api.Draw3D;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.opengl.GL33.*;

public class GlTracers extends GlInstancedShader {
    public GlTracers() {
        super("3d/basic.frag", "3d/tracers.vert", 16);
        new GlAttributeBuilder()
                .stride(16)
                .floatAttrib(3, 0)
                .intAttrib(1, 12);
    }

    public void addLine(double x, double y, double z, int color) {
        checkFlush();
        Vec3 origin = Draw3D.getInstance().origin;
        int pos = buffer.position();
        buffer.putFloat(pos, (float) (x - origin.x));
        buffer.putFloat(pos + 4, (float) (y - origin.y));
        buffer.putFloat(pos + 8, (float) (z - origin.z));
        buffer.putInt(pos + 12, color);
        buffer.position(pos + 16);
        count++;
    }

    @Override
    public void flush() {
        glUseProgram(program);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_LINES, 0, 2, flushBuffer());
    }
}
