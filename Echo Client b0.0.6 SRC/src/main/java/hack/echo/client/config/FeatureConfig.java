package hack.echo.client.config;

import com.google.gson.*;
import hack.echo.client.Echo;
import hack.echo.client.features.Feature;
import hack.echo.client.features.settings.Setting;
import hack.echo.client.features.settings.impl.*;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Base64;

/* We hash the feature names so that we never init a string into mem
* that could be used to anything from the client
*/
public class FeatureConfig {

    public record ProfileEntry(
            String name,
            String serverName,
            String serverIp,
            byte[] serverIconBytes
    ) {}

    private final File profileDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public boolean loading = false;

    public FeatureConfig() {
        this.profileDir = new File("Echo/profiles");
        if (!profileDir.exists()) profileDir.mkdirs();
    }

    public List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        File[] files = profileDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return out;
        for (File f : files) {
            String name = f.getName();
            out.add(name.substring(0, name.length() - 5)); // remove .json
        }
        return out;
    }

    public List<ProfileEntry> listProfileEntries() {
        List<ProfileEntry> entries = new ArrayList<>();
        File[] files = profileDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return entries;

        for (File file : files) {
            ProfileEntry entry = readProfileEntry(file);
            if (entry == null) continue;
            entries.add(entry);
        }

        return entries;
    }

    public boolean saveProfile(String name) {
        if (name == null || name.isBlank()) return false;
        if (Echo.featureManager == null) return false;

        File file = new File(profileDir, sanitize(name) + ".json");
        JsonObject root = new JsonObject();
        JsonArray featureArray = new JsonArray();

        List<Feature> features = Echo.featureManager.getFeatures();
        if (features == null) return false;

        for (Feature f : features) {
            JsonObject fObj = new JsonObject();
            fObj.addProperty("name", Concat.hash(f.getName()));
            fObj.addProperty("enabled", f.isEnabled());
            fObj.addProperty("keybind", f.getKey());
            fObj.addProperty("vis", f.isVisible());

            if (f.hasSettings()) {
                JsonObject sObj = new JsonObject();
                for (Setting s : f.settings) {
                    try {
                        String settingHash = Concat.hash(s.getTypeId() + s.getNameSequence() + s.getSuffixSequence());
                        
                        if (s instanceof BoolSetting bs) {
                            sObj.addProperty(settingHash, bs.getValue());
                        } else if (s instanceof IntSetting is) {
                            sObj.add(settingHash, new JsonPrimitive(is.getValue()));
                        } else if (s instanceof FloatSetting fs) {
                            sObj.addProperty(settingHash, fs.getValue());
                        } else if (s instanceof RangeSetting rs) {
                            JsonObject rangeObj = new JsonObject();
                            rangeObj.addProperty("min", rs.getMinValue());
                            rangeObj.addProperty("max", rs.getMaxValue());
                            sObj.add(settingHash, rangeObj);
                        } else if (s instanceof ModeSetting ms) {
                            sObj.addProperty(settingHash, Concat.hash(ms.getValueSequence()));
                        } else if (s instanceof MultiModeSetting mms) {
                            JsonObject mmObj = new JsonObject();
                            for (Map.Entry<CharSequence, Boolean> entry : mms.getOptionsSequenceView().entrySet()) {
                                mmObj.addProperty(Concat.hash(entry.getKey()), entry.getValue());
                            }
                            sObj.add(settingHash, mmObj);
                        } else if (s instanceof StringSetting ss) {
                            sObj.addProperty(settingHash, ss.getValue());
                        } else if (s instanceof ColorSetting cs) {
                            sObj.addProperty(settingHash, cs.getARGB());
                        } else if (s instanceof KeybindSetting ks) {
                            sObj.addProperty(settingHash, ks.getKey());
                        } else if (s instanceof RegistryPickerSetting<?> rps) {
                            JsonArray itemArray = new JsonArray();
                            for (Identifier id : rps.getSelectedIds()) {
                                itemArray.add(id.toString());
                            }
                            sObj.add(settingHash, itemArray);
                        } else if (s instanceof HotbarSelectionSetting hss) {
                            JsonArray hotbarArray = new JsonArray();
                            for (int i = 0; i < 9; i++) {
                                hotbarArray.add(hss.isSelected(i));
                            }
                            sObj.add(settingHash, hotbarArray);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to save setting: " + s.getName());
                        e.printStackTrace();
                    }
                }
                if (!sObj.entrySet().isEmpty()) fObj.add("settings", sObj);
            }

            featureArray.add(fObj);
        }

        addServerMetadata(root);
        addFriendsMetadata(root);
        root.add("settings", featureArray);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(root, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadProfile(String name) {
        if (name == null || name.isBlank()) return false;

        File file = new File(profileDir, sanitize(name) + ".json");
        if (!file.exists()) return false;

        loading = true;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("settings")) return false;

            JsonArray arr = root.getAsJsonArray("settings");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject fObj = el.getAsJsonObject();

                String fHash = fObj.get("name").getAsString();
                Feature f = getFeatureByHash(fHash);
                if (f == null) {
                    System.err.println("Feature not found for hash: " + fHash);
                    continue;
                }

                if (fObj.has("enabled")) f.setEnabled(fObj.get("enabled").getAsBoolean());
                if (fObj.has("keybind")) f.setKey(fObj.get("keybind").getAsInt());
                if (fObj.has("vis")) f.setVisible(fObj.get("vis").getAsBoolean());
                if (fObj.has("settings")) {
                    JsonObject settingsObj = fObj.getAsJsonObject("settings");

                    for (Setting s : f.settings) {
                        String settingHash = Concat.hash(s.getTypeId() + s.getNameSequence() + s.getSuffixSequence());
                        if (!settingsObj.has(settingHash)) continue;

                        try {
                            JsonElement elSetting = settingsObj.get(settingHash);

                            switch (s) {
                                case BoolSetting bs -> bs.setValue(elSetting.getAsBoolean());
                                case IntSetting is -> is.setValue(elSetting.getAsInt());
                                case FloatSetting fs -> fs.setValue(elSetting.getAsFloat());
                                case RangeSetting rs -> {
                                    JsonObject rangeObj = elSetting.getAsJsonObject();
                                    if (rangeObj.has("min")) rs.setMinValue(rangeObj.get("min").getAsFloat());
                                    if (rangeObj.has("max")) rs.setMaxValue(rangeObj.get("max").getAsFloat());
                                }
                                case ModeSetting ms -> {
                                    String modeHash = elSetting.getAsString();
                                    CharSequence matchedMode = findModeByHash(ms, modeHash);
                                    if (matchedMode != null) {
                                        ms.setValue(matchedMode);
                                    }
                                }
                                case MultiModeSetting mms -> {
                                    JsonObject mmObj = elSetting.getAsJsonObject();
                                    Map<CharSequence, Boolean> mutable = new HashMap<>(mms.getOptionsSequenceView());
                                    for (Map.Entry<String, JsonElement> entry : mmObj.entrySet()) {
                                        String optionHash = entry.getKey();
                                        CharSequence matchedOption = findOptionByHash(mms, optionHash);
                                        if (matchedOption != null) {
                                            mutable.put(matchedOption, entry.getValue().getAsBoolean());
                                        }
                                    }
                                    mms.setOptionsSequenceView(mutable);
                                }
                                case StringSetting ss -> ss.setValue(elSetting.getAsString());
                                case ColorSetting cs -> {
                                    int value = elSetting.getAsInt();
                                    if ((value >>> 24) == 0) {
                                        cs.setFromRGB(value);
                                        cs.setAlpha(255);
                                    }  else cs.setFromRGB(value);
                                }
                                case KeybindSetting ks -> ks.setKey(elSetting.getAsInt());
                                case RegistryPickerSetting<?> rps -> {
                                    if (elSetting.isJsonArray()) {
                                        JsonArray itemArray = elSetting.getAsJsonArray();
                                        Set<Identifier> ids = new HashSet<>();
                                        for (JsonElement elem : itemArray) {
                                            try {
                                                Identifier id = Identifier.parse(elem.getAsString());
                                                ids.add(id);
                                            } catch (Exception ex) {
                                                // Skip invalid IDs
                                            }
                                        }
                                        rps.setSelectedIds(ids);
                                    }
                                }
                                case HotbarSelectionSetting hss -> {
                                    if (elSetting.isJsonArray()) {
                                        JsonArray hotbarArray = elSetting.getAsJsonArray();
                                        for (int i = 0; i < 9; i++) {
                                            boolean selected = i < hotbarArray.size() && hotbarArray.get(i).getAsBoolean();
                                            hss.setSelected(i, selected);
                                        }
                                    }
                                }
                                default -> {
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load setting: " + s.getName());
                            e.printStackTrace();
                        }
                    }
                }
            }

            loadFriendsMetadata(root);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            loading = false;
        }
    }

    public boolean deleteProfile(String name) {
        File f = new File(profileDir, sanitize(name) + ".json");
        return f.exists() && f.delete();
    }

    private void addServerMetadata(JsonObject root) {
        JsonObject serverObj = new JsonObject();
        ServerData server = Minecraft.getInstance().getCurrentServer();

        if (server == null) {
            serverObj.addProperty("name", "singleplayer");
            serverObj.addProperty("ip", "");
            root.add("server", serverObj);
            return;
        }

        serverObj.addProperty("name", server.name == null ? "" : server.name);
        serverObj.addProperty("ip", server.ip == null ? "" : server.ip);

        byte[] iconBytes = server.getIconBytes();
        if (iconBytes != null && iconBytes.length > 0) {
            serverObj.addProperty("icon", Base64.getEncoder().encodeToString(iconBytes));
        }

        root.add("server", serverObj);
    }

    private void addFriendsMetadata(JsonObject root) {
        JsonArray friendsArray = new JsonArray();
        Collection<String> friends = Echo.friendManager == null ? List.of() : Echo.friendManager.getFriends();

        for (String friend : friends) {
            if (friend == null || friend.isBlank()) continue;
            friendsArray.add(friend);
        }

        root.add("friends", friendsArray);
    }

    private void loadFriendsMetadata(JsonObject root) {
        if (Echo.friendManager == null) {
            return;
        }

        List<String> friends = new ArrayList<>();
        if (root != null && root.has("friends") && root.get("friends").isJsonArray()) {
            JsonArray friendsArray = root.getAsJsonArray("friends");
            for (JsonElement element : friendsArray) {
                if (element == null || element.isJsonNull()) continue;
                friends.add(element.getAsString());
            }
        }

        Echo.friendManager.setFriends(friends);
    }

    private ProfileEntry readProfileEntry(File file) {
        String profileName = file.getName().substring(0, file.getName().length() - 5);

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                return new ProfileEntry(profileName, "", "", null);
            }

            JsonObject serverObj = root.has("server") && root.get("server").isJsonObject()
                    ? root.getAsJsonObject("server")
                    : null;

            String serverName = readString(serverObj, "name");
            String serverIp = readString(serverObj, "ip");
            byte[] serverIconBytes = readIconBytes(serverObj);
            return new ProfileEntry(profileName, serverName, serverIp, serverIconBytes);
        } catch (Exception ignored) {
            return new ProfileEntry(profileName, "", "", null);
        }
    }

    private String readString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "";
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) return "";
        return element.getAsString();
    }

    private byte[] readIconBytes(JsonObject serverObj) {
        if (serverObj == null || !serverObj.has("icon")) {
            return null;
        }

        JsonElement iconElement = serverObj.get("icon");
        if (iconElement == null || iconElement.isJsonNull()) {
            return null;
        }

        try {
            return Base64.getDecoder().decode(iconElement.getAsString());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Feature getFeatureByHash(String hash) {
        if (hash == null) return null;
        if (Echo.featureManager == null || Echo.featureManager.getFeatures() == null) return null;
        for (Feature f : Echo.featureManager.getFeatures()) {
            if (f.getName() != null) {
                String featureHash = Concat.hash(f.getName());
                if (hash.equals(featureHash)) {
                    return f;
                }
            }
        }
        return null;
    }

    private CharSequence findModeByHash(ModeSetting ms, String modeHash) {
        if (modeHash == null) return null;
        for (CharSequence mode : ms.getModes()) {
            if (modeHash.equals(Concat.hash(mode))) {
                return mode;
            }
        }
        return null;
    }

    private CharSequence findOptionByHash(MultiModeSetting mms, String optionHash) {
        if (optionHash == null) return null;
        for (CharSequence option : mms.getOptionsSequenceView().keySet()) {
            if (optionHash.equals(Concat.hash(option))) {
                return option;
            }
        }
        return null;
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
