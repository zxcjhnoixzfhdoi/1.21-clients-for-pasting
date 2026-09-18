package hack.echo.client.auth;

import hack.echo.client.utils.strings.Concat;
//? if auth {
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hack.echo.client.Echo;
//?}

/**
 * Central hub for all global strings.
 * All Concat.of() calls get transformed to getGlobalString(index) at build time.
 * The backend sends encoded strings which decode on-the-fly during charAt().
 *
 * @author Cyde
 */
public class CentralFeatureInfoHub {

    //? if auth {
    // Global strings array - populated from backend response
    public static Concat[] globalStrings = new Concat[0];

    private static final Gson gson = new Gson();
    private static boolean initialized = false;

    // Response key (minified for obfuscation)
    private static final String KEY_G = "g";  // Global strings array

    // XOR encoding seed - must match server (0xEC40)
    private static final int ENCODING_SEED = 0xEC40;

    /**
     * Get a global string by index.
     * Called by transformed code: CentralFeatureInfoHub.getGlobalString(42)
     * Returns empty Concat if not initialized or index out of bounds
     */
    public static Concat getGlobalString(int index) {
        if (index < 0 || index >= globalStrings.length) {
            // Not initialized yet or invalid index - return empty
            return Concat.of("");
        }
        return globalStrings[index];
    }

    // Convert JsonArray of encoded ints to int[]
    private static int[] jsonArrayToIntArray(JsonArray arr) {
        int[] result = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).getAsInt();
        }
        return result;
    }

    public static void initializeFromResponse(String jsonResponse) {
        if (initialized) return;
        try {
            JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);

            // Response structure: { type: 'features_response', g: [...] }
            JsonArray stringsArr = json.getAsJsonArray(KEY_G);
            if (stringsArr == null || stringsArr.isEmpty()) {
                //? if debug
                //Echo.LOGGER.warn("[CentralFeatureInfoHub] No global strings in response");
                return;
            }

            globalStrings = new Concat[stringsArr.size()];

            for (int i = 0; i < stringsArr.size(); i++) {
                // Each string gets its own seed based on index
                int stringSeed = (ENCODING_SEED + i * 7919) & 0x7FFFFFFF;
                JsonArray encodedString = stringsArr.get(i).getAsJsonArray();
                //globalStrings[i] = Concat.ofEncoded(jsonArrayToIntArray(encodedString), stringSeed);
            }

            initialized = true;
            //? if debug
            //Echo.LOGGER.info("[CentralFeatureInfoHub] Initialized {} global strings from backend", globalStrings.length);
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("[CentralFeatureInfoHub] Error parsing global strings", e);
        }
    }

    public static JsonObject createFeaturesRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "get_features");
        return request;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getStringCount() {
        return globalStrings.length;
    }
    //?}

    //? if !auth {
    
    /*// non-auth = dev mode, strings stay inline via Concat.of()
    // This method exists but should never be called in dev mode
    public static Concat getGlobalString(int index) {
        return Concat.of("");
    }

    public static boolean isInitialized() {
        return true;
    }

    public static int getStringCount() {
        return 0;
    }
    *///?}
}
