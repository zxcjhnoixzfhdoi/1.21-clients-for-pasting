package we.devs.opium.api.manager.miscellaneous;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.friend.Friend;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.client.values.Value;
import we.devs.opium.client.values.impl.*;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class ConfigManager {
    public static final String CONFIG_DIRECTORY = "Opium/Configs/";

    private boolean saving = false;

    public void delete(String name) {
        try {
            Files.deleteIfExists(Paths.get(CONFIG_DIRECTORY, name + ".json"));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public ArrayList<Object> getAvailableConfigs() {
        ArrayList<Object> configNames = new ArrayList<>();
        File configDirectory = new File(CONFIG_DIRECTORY);

        if (configDirectory.exists() && configDirectory.isDirectory()) {
            File[] files = configDirectory.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String configName = fileName.substring(0, fileName.lastIndexOf("."));
                    configNames.add(configName);
                }
            }
        } else {
            Opium.LOGGER.error("Config directory does not exist: " + CONFIG_DIRECTORY);
        }

        return configNames;
    }

    public void save(String name) {
        if(saving) return;
        saving = true;
        try {
            Path path = Path.of(CONFIG_DIRECTORY);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                Opium.LOGGER.atInfo().log("Created Opium directory");
            }
            this.saveConfig(name);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        saving = false;
    }

    public void saveConfig(String configName) throws IOException {
        if (configName == null || configName.isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be null or empty!");
        }

        // Ensure the config directory exists
        Files.createDirectories(Paths.get(CONFIG_DIRECTORY));

        // Construct the file path
        Path configFilePath = Paths.get(CONFIG_DIRECTORY, configName + ".json");

        // Use JsonWriter for streaming-based writing
        try (JsonWriter writer = new JsonWriter(
                new OutputStreamWriter(Files.newOutputStream(configFilePath), StandardCharsets.UTF_8))) {

            // For nicer formatting
            writer.setIndent("  ");

            writer.beginObject(); // root {

            writer.name("Modules");
            writer.beginObject(); // "Modules": {
            for (Module module : Opium.MODULE_MANAGER.getModules()) {
                writer.name(module.getName());
                writer.beginObject(); // moduleName: {

                writer.name("Name").value(module.getName());
                writer.name("Status").value(module.isToggled());

                writer.name("Values");
                writer.beginObject();
                // Instead of building a JsonObject, write out each Value
                writeValues(writer, module.getValues());
                writer.endObject(); // Values }

                writer.endObject(); // moduleName }
            }
            writer.endObject(); // Modules }

            writer.name("Elements");
            writer.beginObject(); // "Elements": {
            for (Element element : Opium.ELEMENT_MANAGER.getElements()) {
                writer.name(element.getName());
                writer.beginObject(); // elementName: {

                writer.name("Name").value(element.getName());
                writer.name("Status").value(element.isToggled());

                writer.name("Values");
                writer.beginObject();
                writeValues(writer, element.getValues());
                writer.endObject(); // Values }

                // Save position
                writer.name("Positions");
                writer.beginObject();
                writer.name("X").value(element.frame.getX());
                writer.name("Y").value(element.frame.getY());
                writer.endObject(); // Positions }

                writer.endObject(); // elementName }
            }
            writer.endObject(); // Elements }

            writer.name("Client");
            writer.beginObject(); // Client {
            writer.name("Prefix").value(Opium.COMMAND_MANAGER.getPrefix());

            // Friends array
            writer.name("Friends");
            writer.beginArray();
            for (Friend friend : Opium.FRIEND_MANAGER.getFriends()) {
                writer.value(friend.getName());
            }
            writer.endArray(); // Friends ]

            writer.endObject(); // Client }

            writer.endObject(); // root }

        }

        Opium.LOGGER.info("Config saved as: " + configName);
    }

    public void loadConfig(String configName) throws IOException {
        if (configName == null || configName.isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be null or empty!");
        }

        Path configFilePath = Paths.get(CONFIG_DIRECTORY, configName + ".json");

        if (!Files.exists(configFilePath)) {
            Opium.LOGGER.error("Config not found: " + configName);
            return;
        }

        JsonObject rootJson;
        try (InputStream stream = Files.newInputStream(configFilePath);
             InputStreamReader reader = new InputStreamReader(stream)) {
            rootJson = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IllegalStateException | JsonSyntaxException exception) {
            exception.printStackTrace();
            Opium.LOGGER.error("Failed to load config: " + configName);
            return;
        }

        // Load Modules
        if (rootJson.has("Modules")) {
            JsonObject modulesJson = rootJson.getAsJsonObject("Modules");
            for (Map.Entry<String, JsonElement> entry : modulesJson.entrySet()) {
                String moduleName = entry.getKey();
                JsonObject moduleJson = entry.getValue().getAsJsonObject();

                Optional<Module> optionalModule = findModuleByName(moduleName);
                if (optionalModule.isPresent()) {
                    Module module = optionalModule.get();
                    if (moduleJson.has("Status")) {
                        boolean status = moduleJson.get("Status").getAsBoolean();
                        if (status) {
                            module.enable(false);
                        } else {
                            module.disable(false);
                        }
                    }
                    if (moduleJson.has("Values")) {
                        JsonObject valueJson = moduleJson.getAsJsonObject("Values");
                        this.loadValues(valueJson, module.getValues());  // Using your existing loadValues
                    }
                }
            }
        }

        // Load Elements
        if (rootJson.has("Elements")) {
            JsonObject elementsJson = rootJson.getAsJsonObject("Elements");
            for (Map.Entry<String, JsonElement> entry : elementsJson.entrySet()) {
                String elementName = entry.getKey();
                JsonObject elementJson = entry.getValue().getAsJsonObject();

                Optional<Element> optionalElement = findElementByName(elementName);
                if (optionalElement.isPresent()) {
                    Element element = optionalElement.get();
                    if (elementJson.has("Status")) {
                        boolean status = elementJson.get("Status").getAsBoolean();
                        if (status) {
                            element.enable(false);
                        } else {
                            element.disable(false);
                        }
                    }
                    if (elementJson.has("Values")) {
                        JsonObject valueJson = elementJson.getAsJsonObject("Values");
                        this.loadValues(valueJson, element.getValues());  // Using your existing loadValues
                    }
                    if (elementJson.has("Positions")) {
                        JsonObject positionJson = elementJson.getAsJsonObject("Positions");
                        if (positionJson.has("X") && positionJson.has("Y")) {
                            element.frame.setX(positionJson.get("X").getAsFloat());
                            element.frame.setY(positionJson.get("Y").getAsFloat());
                        }
                    }
                }
            }
        }

        // Load Client Data
        if (rootJson.has("Client")) {
            JsonObject clientJson = rootJson.getAsJsonObject("Client");

            // Load Prefix
            if (clientJson.has("Prefix")) {
                Opium.COMMAND_MANAGER.setPrefix(clientJson.get("Prefix").getAsString());
            }

            // Load Friends
            if (clientJson.has("Friends")) {
                JsonArray friendArray = clientJson.getAsJsonArray("Friends");
                friendArray.forEach(friend -> Opium.FRIEND_MANAGER.addFriend(friend.getAsString()));
            }
        }

        Opium.LOGGER.info("Config loaded: " + configName);
    }

    private Optional<Module> findModuleByName(String moduleName) {
        return Opium.MODULE_MANAGER.getModules()
                .stream()
                .filter(module -> module.getName().equalsIgnoreCase(moduleName))
                .findFirst();
    }

    private Optional<Element> findElementByName(String elementName) {
        return Opium.ELEMENT_MANAGER.getElements()
                .stream()
                .filter(element -> element.getName().equalsIgnoreCase(elementName))
                .findFirst();
    }

    private void writeValues(JsonWriter writer, ArrayList<Value> values) throws IOException {
        for (Value value : values) {
            if (value instanceof ValueBoolean) {
                writer.name(value.getName()).value(((ValueBoolean) value).getValue());
            } else if (value instanceof ValueNumber) {
                writer.name(value.getName()).value(((ValueNumber) value).getValue());
            } else if (value instanceof ValueEnum) {
                writer.name(value.getName()).value(((ValueEnum) value).getValue().name());
            } else if (value instanceof ValueString) {
                writer.name(value.getName()).value(((ValueString) value).getValue());
            } else if (value instanceof ValueColor vc) {
                Color color = vc.getValue();

                // same naming scheme as your original code
                writer.name(value.getName()).value(color.getRGB());
                writer.name(value.getName() + "-Alpha").value(color.getAlpha());
                writer.name(value.getName() + "-Rainbow").value(vc.isRainbow());
                writer.name(value.getName() + "-Sync").value(vc.isSync());

            } else if (value instanceof ValueBind) {
                writer.name(value.getName()).value(((ValueBind) value).getValue());
            }
        }
    }

    private void loadValues(JsonObject valueJson, ArrayList<Value> values) {
        for (Value value : values) {
            JsonElement dataObject = valueJson.get(value.getName());
            if (dataObject == null || !dataObject.isJsonPrimitive()) continue;

            switch (value) {
                case ValueBoolean valueBoolean -> valueBoolean.setValue(dataObject.getAsBoolean());
                case ValueNumber valueNumber -> {
                    if (valueNumber.getType() == 1) {
                        ((ValueNumber) value).setValue(dataObject.getAsInt());
                    } else if (((ValueNumber) value).getType() == 2) {
                        ((ValueNumber) value).setValue(dataObject.getAsDouble());
                    } else if (((ValueNumber) value).getType() == 3) {
                        ((ValueNumber) value).setValue(dataObject.getAsFloat());
                    }
                }
                case ValueEnum valueEnum -> valueEnum.setValue(valueEnum.getEnum(dataObject.getAsString()));
                case ValueString valueString -> valueString.setValue(dataObject.getAsString());
                case ValueColor vc -> {
                    vc.setValue(new Color(dataObject.getAsInt()));
                    if (valueJson.get(value.getName() + "-Rainbow") != null) {
                        vc.setRainbow(valueJson.get(value.getName() + "-Rainbow").getAsBoolean());
                    }
                    if (valueJson.get(value.getName() + "-Alpha") != null) {
                        Color c = vc.getValue();
                        vc.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                valueJson.get(value.getName() + "-Alpha").getAsInt()));
                    }
                    if (valueJson.get(value.getName() + "-Sync") != null) {
                        vc.setSync(valueJson.get(value.getName() + "-Sync").getAsBoolean());
                    }
                }
                case ValueBind valueBind -> valueBind.setValue(dataObject.getAsInt());
                default -> {
                }
            }
        }
    }
}
