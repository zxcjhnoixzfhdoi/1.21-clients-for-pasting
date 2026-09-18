package hack.echo.client.render2.impl.opengl.api;

import lombok.Getter;

import static org.lwjgl.opengl.GL33.*;

public class GlState {

    @Getter
    private final boolean blend;
    @Getter
    private final boolean depthTest;
    @Getter
    private final boolean cullFace;
    @Getter
    private final boolean scissorTest;
    @Getter
    private final boolean stencilTest;

    private final int blendSrcRGB;
    private final int blendDstRGB;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final int blendEquationRGB;
    private final int blendEquationAlpha;

    private final int depthFunc;
    private final boolean depthMask;

    private final int stencilFunc;
    private final int stencilRef;
    private final int stencilMask;
    private final int stencilFail;
    private final int stencilPassDepthFail;
    private final int stencilPassDepthPass;
    private final int stencilWriteMask;

    private final int ubo7;
    private final int[] viewport = new int[4];

    private final int framebuffer;

    private final int program;

    private final int vao;

    private final int activeTexture;
    private final int[] boundTextures = new int[10];

    public GlState() {
        blend = glIsEnabled(GL_BLEND);
        depthTest = glIsEnabled(GL_DEPTH_TEST);
        cullFace = glIsEnabled(GL_CULL_FACE);
        scissorTest = glIsEnabled(GL_SCISSOR_TEST);
        stencilTest = glIsEnabled(GL_STENCIL_TEST);

        blendSrcRGB = glGetInteger(GL_BLEND_SRC_RGB);
        blendDstRGB = glGetInteger(GL_BLEND_DST_RGB);
        blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        blendEquationRGB = glGetInteger(GL_BLEND_EQUATION_RGB);
        blendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);

        depthFunc = glGetInteger(GL_DEPTH_FUNC);
        depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        stencilFunc = glGetInteger(GL_STENCIL_FUNC);
        stencilRef = glGetInteger(GL_STENCIL_REF);
        stencilMask = glGetInteger(GL_STENCIL_VALUE_MASK);
        stencilFail = glGetInteger(GL_STENCIL_FAIL);
        stencilPassDepthFail = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL);
        stencilPassDepthPass = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS);
        stencilWriteMask = glGetInteger(GL_STENCIL_WRITEMASK);

        glGetIntegerv(GL_VIEWPORT, viewport);

        framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);

        program = glGetInteger(GL_CURRENT_PROGRAM);

        vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);

        activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        ubo7 = glGetIntegeri(GL_UNIFORM_BUFFER_BINDING, 7);

        for (int i = 0; i < 10; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            boundTextures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
        }
        glActiveTexture(activeTexture);
    }

    public void restore() {
        setEnabled(GL_BLEND, blend);
        setEnabled(GL_DEPTH_TEST, depthTest);
        setEnabled(GL_CULL_FACE, cullFace);
        setEnabled(GL_SCISSOR_TEST, scissorTest);
        setEnabled(GL_STENCIL_TEST, stencilTest);

        glBlendFuncSeparate(blendSrcRGB, blendDstRGB, blendSrcAlpha, blendDstAlpha);
        glBlendEquationSeparate(blendEquationRGB, blendEquationAlpha);

        glDepthFunc(depthFunc);
        glDepthMask(depthMask);

        glStencilFunc(stencilFunc, stencilRef, stencilMask);
        glStencilOp(stencilFail, stencilPassDepthFail, stencilPassDepthPass);
        glStencilMask(stencilWriteMask);

        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        glUseProgram(program);

        glBindVertexArray(vao);
        glBindBufferBase(GL_UNIFORM_BUFFER, 7, ubo7);
        for (int i = 0; i < 10; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, boundTextures[i]);
        }
        glActiveTexture(activeTexture);
    }

    private static void setEnabled(int cap, boolean enabled) {
        if (enabled) {
            glEnable(cap);
        } else {
            glDisable(cap);
        }
    }

    public int[] getViewport() {
        return viewport.clone();
    }
}
