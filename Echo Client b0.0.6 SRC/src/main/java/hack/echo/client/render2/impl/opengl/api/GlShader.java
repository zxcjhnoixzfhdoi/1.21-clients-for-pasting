package hack.echo.client.render2.impl.opengl.api;

import hack.echo.client.render2.impl.opengl.utils.GLShaderUniforms;
import hack.echo.client.render2.impl.opengl.utils.ShaderUtil;
import org.joml.Matrix4f;

import static hack.echo.client.render2.api.Draw2D.cameraUboBinding;
import static org.lwjgl.opengl.GL33.*;

public class GlShader {

    protected final int vao;
    protected final int program;

    public GlShader(String fragSource, String vertSource) {
        this.program = ShaderUtil.createShader(
                "gl/" + fragSource,
                "gl/" + vertSource);

        int index = glGetUniformBlockIndex(program, "Camera");
        if (index != GL_INVALID_INDEX) {
            glUniformBlockBinding(program, index, cameraUboBinding);
        }
        this.vao = glGenVertexArrays();
    }

    protected void uniform1i(String name, int i) {
        GLShaderUniforms.uniform1i(program, name, i);
    }

    protected void uniform1ui(String name, int i) {
        GLShaderUniforms.uniform1ui(program, name, i);
    }

    protected void uniform2i(String name, int i, int j) {
        GLShaderUniforms.uniform2i(program, name, i, j);
    }

    protected void uniform1f(String name, float f) {
        GLShaderUniforms.uniform1f(program, name, f);
    }

    protected void uniform2f(String name, float f, float g) {
        GLShaderUniforms.uniform2f(program, name, f, g);
    }

    protected void uniform3f(String name, float f, float g, float h) {
        GLShaderUniforms.uniform3f(program, name, f, g, h);
    }

    protected void uniform3i(String name, int f, int g, int h) {
        GLShaderUniforms.uniform3i(program, name, f, g, h);
    }

    protected void uniform4f(String name, float f, float g, float h, float i) {
        GLShaderUniforms.uniform4f(program, name, f, g, h, i);
    }

    protected void uniformMatrix4f(String name, Matrix4f matrix) {
        GLShaderUniforms.uniformMatrix4f(program, name, matrix);
    }

    protected void uniformTexture(String name, int texture) {
        GLShaderUniforms.uniformTexture(program, name, texture);
    }
}
