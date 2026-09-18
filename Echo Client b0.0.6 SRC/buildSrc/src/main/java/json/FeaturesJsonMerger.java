package json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Merges global literal arrays into the features.json file,
 * preserving existing content like globalStrings.
 */
public class FeaturesJsonMerger {

    public static void mergeFeaturesJson(String featuresJsonPath,
                                         List<Integer> globalInts,
                                         List<Float> globalFloats,
                                         List<Double> globalDoubles,
                                         List<Long> globalLongs,
                                         List<Boolean> globalBooleans) throws IOException {
        Path path = Paths.get(featuresJsonPath);
        String existing = "";
        if (Files.exists(path)) {
            existing = Files.readString(path);
        }

        // Parse existing JSON content to preserve globalStrings
        String globalStringsContent = extractJsonArray(existing, "globalStrings");

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Preserve existing globalStrings
        if (globalStringsContent != null) {
            json.append("  \"globalStrings\": ").append(globalStringsContent).append(",\n");
        }

        // Add int array
        json.append("  \"globalInts\": [");
        for (int i = 0; i < globalInts.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(globalInts.get(i));
        }
        json.append("],\n");

        // Add float array
        json.append("  \"globalFloats\": [");
        for (int i = 0; i < globalFloats.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(globalFloats.get(i));
        }
        json.append("],\n");

        // Add double array
        json.append("  \"globalDoubles\": [");
        for (int i = 0; i < globalDoubles.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(globalDoubles.get(i));
        }
        json.append("],\n");

        // Add long array
        json.append("  \"globalLongs\": [");
        for (int i = 0; i < globalLongs.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(globalLongs.get(i));
        }
        json.append("],\n");

        // Add boolean array
        json.append("  \"globalBooleans\": [");
        for (int i = 0; i < globalBooleans.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(globalBooleans.get(i));
        }
        json.append("]\n");

        json.append("}\n");

        Files.writeString(path, json.toString());
        System.out.println("[ValueLiteralTransformer] Merged into: " + featuresJsonPath);
    }

    /**
     * Extract a JSON array value from a JSON string by key name.
     */
    private static String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;

        int bracketStart = json.indexOf('[', colonIdx);
        if (bracketStart < 0) return null;

        int depth = 0;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(bracketStart, i + 1);
                }
            }
        }
        return null;
    }
}
