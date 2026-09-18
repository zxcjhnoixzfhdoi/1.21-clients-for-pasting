package hack.echo.client.render2.impl.opengl.post;

import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.api.GlState;
import hack.echo.client.render2.impl.opengl.utils.GLShaderUniforms;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.render2.impl.opengl.utils.ShaderUtil;

import static org.lwjgl.opengl.GL33.*;

/**
 * Implementation of Dual Kawase blur
 * 
 * @see <a href="https://blog.frost.kiwi/dual-kawase/">Article</a>
 */
public class GlBlur {
    private final int max = 8;
    private final int down;
    private final int up;
    private final int vao;
    private final int sampler;
    private final int[] fbo = new int[max];
    private final int[] texture = new int[max];
    private final int[] width = new int[max];
    private final int[] height = new int[max];
    private int lwidth = -1;
    private int lheight = -1;

    public GlBlur() {
        vao = glGenVertexArrays();
        down = ShaderUtil.createShader("gl/post/kawase_down.frag", "gl/post/post.vert");
        up = ShaderUtil.createShader("gl/post/kawase_up.frag", "gl/post/post.vert");

        sampler = glGenSamplers();
        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        for (int i = 0; i < max; i++) {
            fbo[i] = glGenFramebuffers();
            texture[i] = glGenTextures();
        }
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteProgram(down);
        glDeleteProgram(up);
        glDeleteSamplers(sampler);
        for (int i = 0; i < max; i++) {
            glDeleteFramebuffers(fbo[i]);
            glDeleteTextures(texture[i]);
        }
    }

    private void resize(int screenWidth, int screenHeight) {
        if (lwidth != screenWidth || lheight != screenHeight) {
            lwidth = screenWidth;
            lheight = screenHeight;

            for (int i = 0; i < max; i++) {
                int w = Math.max(1, screenWidth >> (i + 1));
                int h = Math.max(1, screenHeight >> (i + 1));

                width[i] = w;
                height[i] = h;

                RenderUtil.resize(fbo[i], texture[i], -1, w, h);
            }
        }
    }

    /**
     *
     * @param sourceTexture The texture to blur
     * @param sourceWidth   width of the texture
     * @param sourceHeight  height of the texture
     * @param levels        how many blur passes to do
     * @return the texture containing the blurred result of sourceTexture. Use
     *         screenImage to render the texture
     */
    public int blur(CrossTexture sourceTexture, int sourceWidth, int sourceHeight, int levels, float offsetMul) {
        var state = new GlState();
        resize(sourceWidth, sourceHeight);
        levels = Math.min(levels, max);


        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glBindVertexArray(vao);

        glBindSampler(8, sampler);
        glBindSampler(9, sampler);

        glUseProgram(down);
        GLShaderUniforms.uniform1i(down, "tex", 8);
        for (int i = 0; i < levels; i++) {
            glBindFramebuffer(GL_FRAMEBUFFER, fbo[i]);
            glViewport(0, 0, width[i], height[i]);

            int tex = (i == 0) ? sourceTexture.glId : texture[i - 1];
            glActiveTexture(GL_TEXTURE8);
            glBindTexture(GL_TEXTURE_2D, tex);

            GLShaderUniforms.uniform1f(down, "offset", (i + 0.5f) * offsetMul);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        }

        glUseProgram(up);
        GLShaderUniforms.uniform1i(up, "tex", 9);
        for (int i = levels - 2; i >= 0; i--) {
            glBindFramebuffer(GL_FRAMEBUFFER, fbo[i]);
            glViewport(0, 0, width[i], height[i]);

            glActiveTexture(GL_TEXTURE9);
            glBindTexture(GL_TEXTURE_2D, texture[i + 1]);

            GLShaderUniforms.uniform1f(up, "offset", (i + 0.5f) * offsetMul);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        }

        glBindSampler(8, 0);
        glBindSampler(9, 0);

        state.restore();
        return texture[0];
    }
}
