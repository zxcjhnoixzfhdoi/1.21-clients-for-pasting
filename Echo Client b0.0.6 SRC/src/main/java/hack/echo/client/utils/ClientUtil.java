package hack.echo.client.utils;

import org.lwjgl.glfw.GLFW;

public final class ClientUtil {
    private ClientUtil() {}

    public static String keyLabel(int code) {
        if (code == -1) return "NONE";
        if ((code & 0x80000000) != 0) {
            int mb = code & 0xFF;
            switch (mb) {
                case 0: return "LMB";
                case 1: return "RMB";
                case 2: return "MMB";
                case 3: return "MB4";
                case 4: return "MB5";
                default: return "M" + mb;
            }
        }
        String name = GLFW.glfwGetKeyName(code, 0);
        if (name != null && !name.isEmpty()) return name.toUpperCase();
        switch (code) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
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
        }
        return String.valueOf(code);
    }

    public static int blendColors(int color1, int color2, float progress) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        int a = (int) (a1 + (a2 - a1) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Deprecated
    public static String truncateName(String name, int maxLen) {
        return name.length() > maxLen ? name.substring(0, maxLen) + "..." : name;
    }

    public static CharSequence truncateName(CharSequence name, int maxLen) {
        if (name.length() <= maxLen) return name;
        return new TruncatedCharSequence(name, maxLen);
    }

    private static final class TruncatedCharSequence implements CharSequence {
        private final CharSequence source;
        private final int len;
        private final int totalLen;

        TruncatedCharSequence(CharSequence src, int maxLen) {
            this.source = src;
            this.len = maxLen;
            this.totalLen = maxLen + 3; // for "..."
        }

        @Override public int length() { return totalLen; }

        @Override public char charAt(int index) {
            if (index < len) return source.charAt(index);
            return '.';
        }

        @Override public CharSequence subSequence(int start, int end) {
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) sb.append(charAt(i));
            return sb;
        }
    }

    // Numeric CharSequence wrappers to avoid String allocation
    // Doesnt really matter tho as it isnt that traceable
    public static CharSequence intSequence(int value) {
        return new IntCharSequence(value);
    }

    public static CharSequence floatSequence(float value, int decimals) {
        return new FloatCharSequence(value, decimals);
    }

    public static CharSequence rangeSequence(float min, float max, int decimals) {
        return new RangeCharSequence(min, max, decimals);
    }

    private static final class IntCharSequence implements CharSequence {
        private final char[] chars;
        private final int length;

        IntCharSequence(int val) {
            boolean negative = val < 0;
            int absVal = negative ? -val : val;
            int len = negative ? 1 : 0;
            int temp = absVal;
            do { len++; temp /= 10; } while (temp > 0);
            chars = new char[len];
            int idx = len - 1;
            do { chars[idx--] = (char)('0' + (absVal % 10)); absVal /= 10; } while (absVal > 0);
            if (negative) chars[0] = '-';
            length = len;
        }

        @Override public int length() { return length; }
        @Override public char charAt(int i) { return chars[i]; }
        @Override public CharSequence subSequence(int s, int e) { return new String(chars, s, e - s); }
    }

    private static final class FloatCharSequence implements CharSequence {
        private final char[] chars;
        private final int length;

        FloatCharSequence(float val, int decimals) {
            int multiplier = 1;
            for (int i = 0; i < decimals; i++) multiplier *= 10;
            long scaled = Math.round(val * multiplier);
            boolean negative = scaled < 0;
            if (negative) scaled = -scaled;
            long intPart = scaled / multiplier;
            long decPart = scaled % multiplier;
            
            int intLen = 1;
            long temp = intPart;
            while (temp >= 10) { intLen++; temp /= 10; }
            int totalLen = (negative ? 1 : 0) + intLen + 1 + decimals;
            chars = new char[totalLen];
            int idx = totalLen - 1;
            for (int i = decimals - 1; i >= 0; i--) {
                chars[idx--] = (char)('0' + (decPart % 10));
                decPart /= 10;
            }
            chars[idx--] = '.';
            do { chars[idx--] = (char)('0' + (intPart % 10)); intPart /= 10; } while (intPart > 0);
            if (negative) chars[idx] = '-';
            length = totalLen;
        }

        @Override public int length() { return length; }
        @Override public char charAt(int i) { return chars[i]; }
        @Override public CharSequence subSequence(int s, int e) { return new String(chars, s, e - s); }
    }

    private static final class RangeCharSequence implements CharSequence {
        private final FloatCharSequence min;
        private final FloatCharSequence max;
        private final int length;

        RangeCharSequence(float minVal, float maxVal, int decimals) {
            this.min = new FloatCharSequence(minVal, decimals);
            this.max = new FloatCharSequence(maxVal, decimals);
            this.length = min.length() + 1 + max.length();
        }

        @Override public int length() { return length; }
        @Override public char charAt(int i) {
            if (i < min.length()) return min.charAt(i);
            if (i == min.length()) return '-';
            return max.charAt(i - min.length() - 1);
        }
        @Override public CharSequence subSequence(int s, int e) {
            StringBuilder sb = new StringBuilder(e - s);
            for (int i = s; i < e; i++) sb.append(charAt(i));
            return sb;
        }
    }
}


