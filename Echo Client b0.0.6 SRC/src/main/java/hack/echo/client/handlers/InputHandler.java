package hack.echo.client.handlers;

import hack.echo.client.mixin.accessors.KeyboardHandlerAccessor;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.mixin.accessors.MouseHandlerAccessor;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

import static hack.echo.client.utils.Imports.mc;

public final class InputHandler {
    public static final HashMap<Integer, Boolean> mouseButtons = new HashMap<>();

    public static MouseHandlerAccessor getMouseHandler() {
        return (MouseHandlerAccessor) ((MinecraftAccessor) mc).getMouseHandler();
    }

    public static KeyboardHandlerAccessor getKeyboardHandler() {
        return (KeyboardHandlerAccessor) ((MinecraftAccessor) mc).getKeyboardHandler();
    }

    public static void simulateMouseClick(int keyCode) {
        simulateMouseClick(keyCode, 20);
    }

    public static void simulateMouseClick(int keyCode, int millis) {
        if (!isWindowFocused()) return;
        simulateMousePress(keyCode);
        simulateMouseRelease(keyCode);
    }

    public static void simulateClick(KeyMapping keyMapping, boolean useSimulation) {
        simulateClick(keyMapping, useSimulation, 20);
    }

    public static void simulateClick(KeyMapping keyMapping, boolean useSimulation, int millis) {
        if (keyMapping == null || !isWindowFocused()) return;

        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();

        if (key == null) return;

        if (key.getType() == InputConstants.Type.MOUSE) {
            int button = key.getValue();

            if (useSimulation) {
                simulateMouseClick(button, millis);
                GLFW.glfwPollEvents();
            } else {
                incrementClickCountForKeyMapping(keyMapping);
            }
        } else {
            if (useSimulation) {
                simulateKeyClick(keyMapping, millis);
                GLFW.glfwPollEvents();
            } else {
                keyMapping.setDown(true);
                incrementClickCountForKeyMapping(keyMapping);
                keyMapping.setDown(false);
            }
        }
    }

    public static void simulateKeyClick(KeyMapping keyMapping, int millis) {
        if (keyMapping == null || !isWindowFocused()) return;
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();
        if (key == null || key.getType() != InputConstants.Type.KEYSYM) return;
        
        int keyCode = key.getValue();
        simulateKeyPress(keyCode);
        simulateKeyRelease(keyCode);
    }

    public static void simulateKeyPress(KeyMapping keyMapping) {
        if (keyMapping == null) return;
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();
        if (key == null || key.getType() != InputConstants.Type.KEYSYM) {
            keyMapping.setDown(true);
            return;
        }
        simulateKeyPress(key.getValue());
    }

    public static void simulateKeyRelease(KeyMapping keyMapping) {
        if (keyMapping == null) return;
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();
        if (key == null || key.getType() != InputConstants.Type.KEYSYM) {
            keyMapping.setDown(false);
            return;
        }
        simulateKeyRelease(key.getValue());
    }

    public static void simulateKeyPress(int keyCode) {
        simulateKeyEvent(keyCode, GLFW.GLFW_PRESS);
    }

    public static void simulateKeyRelease(int keyCode) {
        simulateKeyEvent(keyCode, GLFW.GLFW_RELEASE);
    }

    private static void simulateKeyEvent(int keyCode, int action) {
        if (mc == null || mc.options == null) return;

        for (KeyMapping keyMapping : mc.options.keyMappings) {
            KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
            if (accessor != null) {
                InputConstants.Key key = accessor.getKey();
                if (key != null && key.getValue() == keyCode) {
                    if (action == GLFW.GLFW_PRESS) {
                        keyMapping.setDown(true);
                    } else {
                        keyMapping.setDown(false);
                    }
                    break;
                }
            }
        }

        if (mc.getWindow() == null) return;
        KeyboardHandlerAccessor keyboardHandler = getKeyboardHandler();
        if (keyboardHandler != null) {
            keyboardHandler.invokeKeyPress(mc.getWindow().handle(), action, new KeyEvent(keyCode, 0,1));
        }
    }

    private static boolean isWindowFocused() {
        if (mc == null || mc.getWindow() == null) return false;
        return GLFW.glfwGetWindowAttrib(mc.getWindow().handle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    public static void simulateMousePress(int keyCode) {
        simulateMouseEvent(keyCode, GLFW.GLFW_PRESS);
    }

    public static void simulateMouseRelease(int keyCode) {
        simulateMouseEvent(keyCode, GLFW.GLFW_RELEASE);
    }

    private static void simulateMouseEvent(int keyCode, int action) {
        if (mc == null) return;

        MouseHandlerAccessor mouseHandler = getMouseHandler();
        if (mouseHandler != null && mc.getWindow() != null) {
            mouseHandler.invokeOnButton(mc.getWindow().handle(), new MouseButtonInfo(keyCode, 0), action);
        }

        if (action == GLFW.GLFW_PRESS) {
            mouseButtons.put(keyCode, true);
        } else if (action == GLFW.GLFW_RELEASE) {
            mouseButtons.put(keyCode, false);
        }
    }

    public static boolean isMouseButtonPressed(int keyCode) {
        return mouseButtons.getOrDefault(keyCode, false);
    }

    public static boolean isMouseDown(int button) {
        if (mc == null) return false;
        return GLFW.glfwGetMouseButton(mc.getWindow().handle(), button) == GLFW.GLFW_PRESS
                || mouseButtons.getOrDefault(button, false);
    }

    public static boolean isKeyDown(int key) {
        if (mc == null) return false;
        return GLFW.glfwGetKey(mc.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }

    public static boolean isBindDown(int bind) {
        if (bind == -1 || bind == 0) return false;
        if ((bind & 0x80000000) != 0) {
            int mb = bind & 0xFF;
            // For mouse binds we need to use GLFW directly
            if (mc == null || mc.getWindow() == null) return false;
            return GLFW.glfwGetMouseButton(mc.getWindow().handle(), mb) == GLFW.GLFW_PRESS;
        }
        return isKeyDown(bind);
    }

    public static boolean shouldSimulateClick(boolean useSimulation, int activateKey, int simButton) {
        if (!useSimulation) return false;
        if ((activateKey & 0x80000000) == 0) return true;
        int actButton = activateKey & 0xFF;
        return actButton != simButton || !isBindDown(activateKey);
    }

    public static void incrementClickCountForKeyMapping(KeyMapping keyMapping) {
        if (keyMapping == null) return;
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();
        if (key != null) {
            KeyMapping.click(key);
        }
    }

    private static int keyToBind(InputConstants.Key key) {
        if (key == null) return -1;
        if (key.getType() == InputConstants.Type.MOUSE) {
            return 0x80000000 | key.getValue();
        }
        return key.getValue();
    }

    public static boolean isBindDown(KeyMapping keyMapping) {
        if (keyMapping == null) return false;
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        InputConstants.Key key = accessor.getKey();
        return isBindDown(keyToBind(key));
    }
}