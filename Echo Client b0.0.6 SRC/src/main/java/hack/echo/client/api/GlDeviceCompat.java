package hack.echo.client.api;

import com.mojang.blaze3d.opengl.DirectStateAccess;
//? if >=26.1 {
/*import java.lang.reflect.Field;
import java.lang.reflect.Method;
*///?} else {
import com.mojang.blaze3d.opengl.GlDevice;
//?}
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public final class GlDeviceCompat {

    private GlDeviceCompat() {
    }

    public static void bindFramebuffer(RenderTarget target) {
        int framebuffer = getFramebufferId(target);
        if (framebuffer == -1) {
            return;
        }

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, target.width, target.height);
    }

    public static int getFramebufferId(RenderTarget target) {
        if (!(target.getColorTexture() instanceof GlTexture texture)) {
            return -1;
        }

        DirectStateAccess directStateAccess = getDirectStateAccess();
        if (directStateAccess == null) {
            return -1;
        }

        return texture.getFbo(directStateAccess, target.getDepthTexture());
    }

    private static DirectStateAccess getDirectStateAccess() {
        //? if >=26.1 {
        /*Object rawDevice = RenderSystem.getDevice();
        if (rawDevice == null) {
            return null;
        }

        try {
            Field backendField = rawDevice.getClass().getDeclaredField("backend");
            backendField.setAccessible(true);
            Object glDevice = backendField.get(rawDevice);
            if (glDevice == null) {
                return null;
            }

            Method dsaMethod = glDevice.getClass().getDeclaredMethod("directStateAccess");
            dsaMethod.setAccessible(true);
            return (DirectStateAccess) dsaMethod.invoke(glDevice);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        *///?} else {
        if (!(RenderSystem.getDevice() instanceof GlDevice device)) {
            return null;
        }

        return device.directStateAccess();
        //?}
    }
}
