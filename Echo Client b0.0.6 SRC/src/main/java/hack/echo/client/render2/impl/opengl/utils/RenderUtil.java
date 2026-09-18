package hack.echo.client.render2.impl.opengl.utils;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import hack.echo.client.api.TupleCompat;
import hack.echo.client.render2.impl.opengl.api.GlState;
import hack.echo.client.api.GlDeviceCompat;
import hack.echo.client.utils.VulkanUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL20.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

public class RenderUtil {

    private static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static Matrix4f getProjectionMatrix() {
        Minecraft minecraft = getMinecraft();
        if (minecraft == null) {
            return new Matrix4f();
        }

        return new Matrix4f().ortho(0, getScaledWidth(), getScaledHeight(), 0, -1000, 1000, VulkanUtil.isVulkanLoaded());
    }

    public static int getScaledWidth() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getWidth() / 2.0);
    }

    public static int getScaledHeight() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getHeight() / 2.0);
    }

    public static float getTime() {
        Minecraft minecraft = getMinecraft();
        if (minecraft == null || minecraft.level == null) {
            return 0f;
        }

        return (minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20f;
    }


    public static void bindMainFramebuffer() {
        Minecraft minecraft = getMinecraft();
        if (minecraft == null) {
            return;
        }

        RenderTarget fbo = minecraft.getMainRenderTarget();
        GlDeviceCompat.bindFramebuffer(fbo);
    }

    public static int getFramebufferId(RenderTarget fbo) {
        return GlDeviceCompat.getFramebufferId(fbo);
    }

    public static int getBoundFramebuffer() {
        return glGetInteger(GL_FRAMEBUFFER_BINDING);
    }

    @Deprecated
    public static int getGlId(Identifier texture) {
        Minecraft minecraft = getMinecraft();
        if (minecraft == null) {
            return -1;
        }

        TextureManager manager = minecraft.getTextureManager();
        AbstractTexture abstractTexture = manager.getTexture(texture);
        if (abstractTexture.getTexture() instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        return -1;
    }


    public static int createFramebuffer() {
        return glGenFramebuffers();
    }

    public static int createTexture() {
        return glGenTextures();
    }


    public static void resize(int fbo, int tex, int depth, int width, int height) {
        var state = new GlState();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        if (depth != -1) {
            glBindRenderbuffer(GL_RENDERBUFFER, depth);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
        if (depth != -1) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        if (depth != -1)
            glBindRenderbuffer(GL_RENDERBUFFER, 0);

        state.restore();
    }

    public static TupleCompat<Vec3, Boolean> project(Matrix4f modelView, Matrix4f projection, Vec3 vector) {
        Minecraft minecraft = getMinecraft();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return new TupleCompat<>(vector, false);
        }

//        Vec3d camPos = vector.subtract(mc.gameRenderer.getCamera().getPos());
        Vec3 camPos = vector.subtract(minecraft.gameRenderer.getMainCamera().position());
        Vector4f vec = new Vector4f((float) camPos.x, (float) camPos.y, (float) camPos.z, 1F);

        vec.mul(modelView);
        vec.mul(projection);

        boolean isVisible = vec.w() > 0.0;

        if (vec.w() != 0) {
            vec.x /= vec.w();
            vec.y /= vec.w();
            vec.z /= vec.w();
        }

        double screenX = (vec.x() * 0.5 + 0.5) * getScaledWidth();
        double screenY = (0.5 - vec.y() * 0.5) * getScaledHeight();

        Vec3 position = new Vec3(screenX, screenY, vec.z());

        return new TupleCompat<>(position, isVisible);
    }

}
