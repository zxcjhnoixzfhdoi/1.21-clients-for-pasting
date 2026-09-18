
package hack.echo.client.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// For el website
public class Exporter {
    //? if debug {
    /*public static JsonObject collect() {
        JsonObject build = new JsonObject();

        for (Category category : Category.values()) {
            JsonObject categoryData = new JsonObject();
            Echo.featureManager.getFeatures().stream()
                    .filter(feature -> feature.getCategory() == category)
                    .forEach(module -> {
                        categoryData.addProperty(module.getName().toString(), module.getDescription().toString());
                    });
            build.add(category.name.toString(), categoryData);
        }

        return build;
    }

    public static void saveExport(JsonObject object) {
        try {
            File exportDir = new File(Minecraft.getInstance().gameDirectory, Echo.MOD_ID);
            exportDir.mkdirs();
            File exportFile = new File(exportDir, "export.json");
            Files.writeString(exportFile.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(object), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    *///?}
}

