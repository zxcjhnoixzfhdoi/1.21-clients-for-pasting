package wtf.opal.client.renderer.repository;

import net.fabricmc.loader.api.FabricLoader;
import wtf.opal.client.renderer.text.NVGTextRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public final class FontRepository {

    private static final HashMap<String, NVGTextRenderer> TEXT_RENDERER_MAP = new HashMap<>();

    public static NVGTextRenderer getFont(final String name) {
        if (TEXT_RENDERER_MAP.containsKey(name))
            return TEXT_RENDERER_MAP.get(name);

        final Path pathURL = FabricLoader.getInstance().getModContainer("opal")
                .flatMap(c -> c.findPath("assets/opal/fonts/" + name + ".ttf"))
                .orElse(null);

        try {
            if (pathURL != null) {
                TEXT_RENDERER_MAP.put(name, new NVGTextRenderer(name, Files.newInputStream(pathURL)));

                return TEXT_RENDERER_MAP.get(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Font not found: " + name);
    }
}
