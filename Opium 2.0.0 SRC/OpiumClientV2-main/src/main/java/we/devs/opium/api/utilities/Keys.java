package we.devs.opium.api.utilities;

import org.lwjgl.glfw.GLFW;

public class Keys {
    public static int resolveKeyCode(String key) {
        int glfwKeyCode = tryGLFWKeyName(key);
        if (glfwKeyCode != -1) {
            return glfwKeyCode;
        }
        return getFallbackKeyCode(key);
    }
    private static int tryGLFWKeyName(String key) {
        try {
            // Try GLFW's internal mapping
            for (int i = GLFW.GLFW_KEY_SPACE; i <= GLFW.GLFW_KEY_LAST; i++) {
                String glfwName = GLFW.glfwGetKeyName(i, 0);
                if (glfwName != null && glfwName.equalsIgnoreCase(key)) {
                    return i;
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
    public static int getFallbackKeyCode(String key) {
        String upperKey = key.toUpperCase();
        switch (upperKey) {
            case "SPACE": return GLFW.GLFW_KEY_SPACE;
            case "APOSTROPHE": return GLFW.GLFW_KEY_APOSTROPHE;
            case "COMMA": return GLFW.GLFW_KEY_COMMA;
            case "MINUS": return GLFW.GLFW_KEY_MINUS;
            case "PERIOD": return GLFW.GLFW_KEY_PERIOD;
            case "SLASH": return GLFW.GLFW_KEY_SLASH;
            case "SEMICOLON": return GLFW.GLFW_KEY_SEMICOLON;
            case "EQUAL": return GLFW.GLFW_KEY_EQUAL;
            case "LEFT_BRACKET": return GLFW.GLFW_KEY_LEFT_BRACKET;
            case "BACKSLASH": return GLFW.GLFW_KEY_BACKSLASH;
            case "RIGHT_BRACKET": return GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "GRAVE_ACCENT": return GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "ESCAPE": return GLFW.GLFW_KEY_ESCAPE;
            case "ENTER": return GLFW.GLFW_KEY_ENTER;
            case "TAB": return GLFW.GLFW_KEY_TAB;
            case "BACKSPACE": return GLFW.GLFW_KEY_BACKSPACE;
            case "INSERT": return GLFW.GLFW_KEY_INSERT;
            case "DELETE": return GLFW.GLFW_KEY_DELETE;
            case "RIGHT": return GLFW.GLFW_KEY_RIGHT;
            case "LEFT": return GLFW.GLFW_KEY_LEFT;
            case "DOWN": return GLFW.GLFW_KEY_DOWN;
            case "UP": return GLFW.GLFW_KEY_UP;
            case "PAGE_UP": return GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN": return GLFW.GLFW_KEY_PAGE_DOWN;
            case "HOME": return GLFW.GLFW_KEY_HOME;
            case "END": return GLFW.GLFW_KEY_END;
            case "CAPS_LOCK": return GLFW.GLFW_KEY_CAPS_LOCK;
            case "SCROLL_LOCK": return GLFW.GLFW_KEY_SCROLL_LOCK;
            case "NUM_LOCK": return GLFW.GLFW_KEY_NUM_LOCK;
            case "PRINT_SCREEN": return GLFW.GLFW_KEY_PRINT_SCREEN;
            case "PAUSE": return GLFW.GLFW_KEY_PAUSE;
            case "F1": return GLFW.GLFW_KEY_F1;
            case "F2": return GLFW.GLFW_KEY_F2;
            case "F3": return GLFW.GLFW_KEY_F3;
            case "F4": return GLFW.GLFW_KEY_F4;
            case "F5": return GLFW.GLFW_KEY_F5;
            case "F6": return GLFW.GLFW_KEY_F6;
            case "F7": return GLFW.GLFW_KEY_F7;
            case "F8": return GLFW.GLFW_KEY_F8;
            case "F9": return GLFW.GLFW_KEY_F9;
            case "F10": return GLFW.GLFW_KEY_F10;
            case "F11": return GLFW.GLFW_KEY_F11;
            case "F12": return GLFW.GLFW_KEY_F12;
            case "F13": return GLFW.GLFW_KEY_F13;
            case "F14": return GLFW.GLFW_KEY_F14;
            case "F15": return GLFW.GLFW_KEY_F15;
            case "F16": return GLFW.GLFW_KEY_F16;
            case "F17": return GLFW.GLFW_KEY_F17;
            case "F18": return GLFW.GLFW_KEY_F18;
            case "F19": return GLFW.GLFW_KEY_F19;
            case "F20": return GLFW.GLFW_KEY_F20;
            case "F21": return GLFW.GLFW_KEY_F21;
            case "F22": return GLFW.GLFW_KEY_F22;
            case "F23": return GLFW.GLFW_KEY_F23;
            case "F24": return GLFW.GLFW_KEY_F24;
            case "F25": return GLFW.GLFW_KEY_F25;
            case "KP_0": return GLFW.GLFW_KEY_KP_0;
            case "KP_1": return GLFW.GLFW_KEY_KP_1;
            case "KP_2": return GLFW.GLFW_KEY_KP_2;
            case "KP_3": return GLFW.GLFW_KEY_KP_3;
            case "KP_4": return GLFW.GLFW_KEY_KP_4;
            case "KP_5": return GLFW.GLFW_KEY_KP_5;
            case "KP_6": return GLFW.GLFW_KEY_KP_6;
            case "KP_7": return GLFW.GLFW_KEY_KP_7;
            case "KP_8": return GLFW.GLFW_KEY_KP_8;
            case "KP_9": return GLFW.GLFW_KEY_KP_9;
            case "KP_DECIMAL": return GLFW.GLFW_KEY_KP_DECIMAL;
            case "KP_DIVIDE": return GLFW.GLFW_KEY_KP_DIVIDE;
            case "KP_MULTIPLY": return GLFW.GLFW_KEY_KP_MULTIPLY;
            case "KP_SUBTRACT": return GLFW.GLFW_KEY_KP_SUBTRACT;
            case "KP_ADD": return GLFW.GLFW_KEY_KP_ADD;
            case "KP_ENTER": return GLFW.GLFW_KEY_KP_ENTER;
            case "KP_EQUAL": return GLFW.GLFW_KEY_KP_EQUAL;
            case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            case "LSUPER": return GLFW.GLFW_KEY_LEFT_SUPER;
            case "RSUPER": return GLFW.GLFW_KEY_RIGHT_SUPER;
            case "MENU": return GLFW.GLFW_KEY_MENU;
            default: return -1; // Invalid key
        }
    }
    public static String getFallbackKeyName(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_APOSTROPHE: return "APOSTROPHE";
            case GLFW.GLFW_KEY_COMMA: return "COMMA";
            case GLFW.GLFW_KEY_MINUS: return "MINUS";
            case GLFW.GLFW_KEY_PERIOD: return "PERIOD";
            case GLFW.GLFW_KEY_SLASH: return "SLASH";
            case GLFW.GLFW_KEY_SEMICOLON: return "SEMICOLON";
            case GLFW.GLFW_KEY_EQUAL: return "EQUAL";
            case GLFW.GLFW_KEY_LEFT_BRACKET: return "LEFT_BRACKET";
            case GLFW.GLFW_KEY_BACKSLASH: return "BACKSLASH";
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return "RIGHT_BRACKET";
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "GRAVE_ACCENT";
            case GLFW.GLFW_KEY_ESCAPE: return "ESCAPE";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE_DOWN";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS_LOCK";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "SCROLL_LOCK";
            case GLFW.GLFW_KEY_NUM_LOCK: return "NUM_LOCK";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "PRINT_SCREEN";
            case GLFW.GLFW_KEY_PAUSE: return "PAUSE";
            case GLFW.GLFW_KEY_F1: return "F1";
            case GLFW.GLFW_KEY_F2: return "F2";
            case GLFW.GLFW_KEY_F3: return "F3";
            case GLFW.GLFW_KEY_F4: return "F4";
            case GLFW.GLFW_KEY_F5: return "F5";
            case GLFW.GLFW_KEY_F6: return "F6";
            case GLFW.GLFW_KEY_F7: return "F7";
            case GLFW.GLFW_KEY_F8: return "F8";
            case GLFW.GLFW_KEY_F9: return "F9";
            case GLFW.GLFW_KEY_F10: return "F10";
            case GLFW.GLFW_KEY_F11: return "F11";
            case GLFW.GLFW_KEY_F12: return "F12";
            case GLFW.GLFW_KEY_F13: return "F13";
            case GLFW.GLFW_KEY_F14: return "F14";
            case GLFW.GLFW_KEY_F15: return "F15";
            case GLFW.GLFW_KEY_F16: return "F16";
            case GLFW.GLFW_KEY_F17: return "F17";
            case GLFW.GLFW_KEY_F18: return "F18";
            case GLFW.GLFW_KEY_F19: return "F19";
            case GLFW.GLFW_KEY_F20: return "F20";
            case GLFW.GLFW_KEY_F21: return "F21";
            case GLFW.GLFW_KEY_F22: return "F22";
            case GLFW.GLFW_KEY_F23: return "F23";
            case GLFW.GLFW_KEY_F24: return "F24";
            case GLFW.GLFW_KEY_F25: return "F25";
            case GLFW.GLFW_KEY_KP_0: return "KP_0";
            case GLFW.GLFW_KEY_KP_1: return "KP_1";
            case GLFW.GLFW_KEY_KP_2: return "KP_2";
            case GLFW.GLFW_KEY_KP_3: return "KP_3";
            case GLFW.GLFW_KEY_KP_4: return "KP_4";
            case GLFW.GLFW_KEY_KP_5: return "KP_5";
            case GLFW.GLFW_KEY_KP_6: return "KP_6";
            case GLFW.GLFW_KEY_KP_7: return "KP_7";
            case GLFW.GLFW_KEY_KP_8: return "KP_8";
            case GLFW.GLFW_KEY_KP_9: return "KP_9";
            case GLFW.GLFW_KEY_KP_DECIMAL: return "KP_DECIMAL";
            case GLFW.GLFW_KEY_KP_DIVIDE: return "KP_DIVIDE";
            case GLFW.GLFW_KEY_KP_MULTIPLY: return "KP_MULTIPLY";
            case GLFW.GLFW_KEY_KP_SUBTRACT: return "KP_SUBTRACT";
            case GLFW.GLFW_KEY_KP_ADD: return "KP_ADD";
            case GLFW.GLFW_KEY_KP_ENTER: return "KP_ENTER";
            case GLFW.GLFW_KEY_KP_EQUAL: return "KP_EQUAL";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_LEFT_SUPER: return "LSUPER";
            case GLFW.GLFW_KEY_RIGHT_SUPER: return "RSUPER";
            case GLFW.GLFW_KEY_MENU: return "MENU";
            default:
                // Try to get the key name from GLFW if possible
                String glfwKeyName = GLFW.glfwGetKeyName(keyCode, 0);
                return glfwKeyName != null ? glfwKeyName.toUpperCase() : "UNKNOWN";
        }
    }
}