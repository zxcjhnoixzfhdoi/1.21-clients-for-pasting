package hack.echo.client.utils.keyboard;

import org.lwjgl.glfw.GLFW;

import static hack.echo.client.utils.Imports.mc;

public class KeyUtils {
    public static boolean isKeyPressed(int keyCode) {
        if (mc != null && mc.getWindow() != null) {
            return GLFW.glfwGetKey(mc.getWindow().handle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
