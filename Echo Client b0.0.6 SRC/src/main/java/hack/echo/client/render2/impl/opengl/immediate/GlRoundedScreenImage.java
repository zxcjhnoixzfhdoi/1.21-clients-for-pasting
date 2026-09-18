package hack.echo.client.render2.impl.opengl.immediate;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlShader;
import hack.echo.client.render2.impl.opengl.utils.GLShaderUniforms;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlRoundedScreenImage extends GlShader {

    public GlRoundedScreenImage() {
        super("2d/image/rounded_image.frag",
                "2d/image/rounded_screen_image.vert");
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
    }

    public void draw(Matrix4f model, float x, float y, float w, float h, float rad1, float rad2, float rad3, float rad4,
            int textureId, float alpha) {
        glUseProgram(program);
        GLShaderUniforms.uniformMatrix4f(program, "modelMat", model);
        GLShaderUniforms.uniform4f(program, "rect", x, y, w, h);
        GLShaderUniforms.uniform4f(program, "radius", rad1, rad2, rad3, rad4);
        GLShaderUniforms.uniform1f(program, "uAlpha", alpha);
        GLShaderUniforms.uniform1f(program, "z", Draw2D.nextZ());
        GLShaderUniforms.uniformTexture(program, "Sampler0", textureId);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
}
