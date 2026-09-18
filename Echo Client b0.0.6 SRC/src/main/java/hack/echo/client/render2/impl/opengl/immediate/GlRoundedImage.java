package hack.echo.client.render2.impl.opengl.immediate;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlShader;
import hack.echo.client.render2.impl.opengl.utils.GLShaderUniforms;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlRoundedImage extends GlShader {

    public GlRoundedImage() {
        super("2d/image/rounded_image.frag", "2d/image/rounded_image.vert");
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
    }

    public void draw(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int textureId, float alpha) {
        glUseProgram(program);
        GLShaderUniforms.uniformMatrix4f(program, "modelMat", model);
        GLShaderUniforms.uniform4f(program, "rect", x, y, w, h);
        GLShaderUniforms.uniform4f(program, "radius", r1, r2, r3, r4);
        GLShaderUniforms.uniform1f(program, "uAlpha", alpha);
        GLShaderUniforms.uniform1f(program, "z", Draw2D.nextZ());
        GLShaderUniforms.uniformTexture(program, "Sampler0", textureId);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
}
