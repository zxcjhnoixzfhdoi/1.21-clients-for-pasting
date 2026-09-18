package vlt;

/**
 * Parsing helpers for Java numeric and boolean literal strings.
 */
public class LiteralParser {

    public static int parseIntLiteral(String raw) {
        // Java allows underscores for int's, so we strip them
        String cleaned = raw.replace("_", "");
        // Hex literals
        if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            return Integer.parseUnsignedInt(cleaned.substring(2), 16);
            // binary literals
        } else if (cleaned.startsWith("0b") || cleaned.startsWith("0B")) {
            return Integer.parseUnsignedInt(cleaned.substring(2), 2);
            // octal literal
        } else if (cleaned.startsWith("0") && cleaned.length() > 1 && !cleaned.contains(".")) {
            return Integer.parseUnsignedInt(cleaned.substring(1), 8);
        }
        return Integer.parseInt(cleaned);
    }

    public static boolean isFloatLiteral(String raw) {
        return raw.endsWith("f") || raw.endsWith("F");
    }

    public static float parseFloatLiteral(String raw) {
        String cleaned = raw.replace("_", "");
        if (cleaned.endsWith("f") || cleaned.endsWith("F")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return Float.parseFloat(cleaned);
    }

    public static double parseDoubleLiteral(String raw) {
        // Java allows underscores for int's, so we strip them
        String cleaned = raw.replace("_", "");
        if (cleaned.endsWith("d") || cleaned.endsWith("D")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return Double.parseDouble(cleaned);
    }

    public static long parseLongLiteral(String raw) {
        // Java allows underscores for int's, so we strip them
        String cleaned = raw.replace("_", "");
        // Longs in Java are end with an L
        if (cleaned.endsWith("l") || cleaned.endsWith("L")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        // Hex literals
        if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            return Long.parseUnsignedLong(cleaned.substring(2), 16);
            // Binary literals
        } else if (cleaned.startsWith("0b") || cleaned.startsWith("0B")) {
            return Long.parseUnsignedLong(cleaned.substring(2), 2);
        }
        return Long.parseLong(cleaned);
    }
}
