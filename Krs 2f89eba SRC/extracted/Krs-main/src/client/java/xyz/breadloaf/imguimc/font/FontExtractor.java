package xyz.breadloaf.imguimc.font;

import com.instrumentalist.krs.utils.IMinecraft;
import com.instrumentalist.krs.Client;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.resources.Identifier;

public class FontExtractor implements IMinecraft {

    public static String getFontPath(String fontNameTtf) {
        return fontDir().getPath() + "/" + fontNameTtf;
    }

    public static void extractFont() throws IOException {
        Identifier fontFile = Identifier.fromNamespaceAndPath("krs", "arial.ttf");
        try (InputStream in = FontExtractor.class.getClassLoader().getResourceAsStream(
                "assets/" + fontFile.getNamespace() + "/" + fontFile.getPath())) {
            if (in == null)
                throw new IOException("Could not find font resource: " + fontFile);

            File fontDir = fontDir();
            Path outputPath = new File(fontDir, "arial.ttf").toPath();
            Files.createDirectories(fontDir.toPath());
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File fontDir() {
        return new File(new File(mc.gameDirectory, Client.configLocation), "imgui_fonts");
    }
}
