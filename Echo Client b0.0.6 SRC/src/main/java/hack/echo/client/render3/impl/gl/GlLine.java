package hack.echo.client.render3.impl.gl;

import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render3.api.Draw3D;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;

public class GlLine extends GlInstancedShader {
    public GlLine() {
        super("basic.frag", "3d/line.vert", 28);
        new GlAttributeBuilder()
                .stride(28)
                .floatAttrib(3, 0)
                .floatAttrib(3, 12)
                .intAttrib(1, 24);
    }
    public void addLine(double fx, double fy, double fz, double tx, double ty, double tz, int color) {
        checkFlush();
        Vec3 origin = Draw3D.getInstance().origin;
        int pos = buffer.position();
        buffer.putFloat(pos, (float) (fx - origin.x));
        buffer.putFloat(pos + 4, (float) (fy - origin.y));
        buffer.putFloat(pos + 8, (float) (fz - origin.z));
        buffer.putFloat(pos + 12, (float) (tx - origin.x));
        buffer.putFloat(pos + 16, (float) (ty - origin.y));
        buffer.putFloat(pos + 20, (float) (tz - origin.z));
        buffer.putInt(pos + 24, color);
        buffer.position(pos + 28);
        count++;
    }
    @Override
    public void flush() {
        glUseProgram(program);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_LINES, 0, 2, flushBuffer());
    }
}
