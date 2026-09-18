package we.devs.opium.api.utilities.font;

import we.devs.opium.Opium;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

// rewrote by heedi

public class FontLoader {
    private static final String FONTS_FOLDER = "opium/fonts";
    private static final String[] DEFAULT_FONTS = {"font.ttf"};

    public static Font[] loadFonts() {
        return loadFonts(FONTS_FOLDER, DEFAULT_FONTS);
    }

    private static Font[] loadFonts(String folder, String[] defaultFonts) {
        File gameDir = MinecraftClient.getInstance().runDirectory;
        Path fontsDir = new File(gameDir, folder).toPath();

        try {
            Files.createDirectories(fontsDir);
        } catch (IOException e) {
            Opium.LOGGER.error("Failed to create fonts directory: " + fontsDir, e);
            return new Font[0];
        }

        for (String fontName : defaultFonts) {
            Path fontPath = fontsDir.resolve(fontName);
            if (!Files.exists(fontPath)) {
                try (InputStream inputStream = FontLoader.class.getResourceAsStream("/assets/" + FONTS_FOLDER + "/" + fontName)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, fontPath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Opium.LOGGER.warn("Default font resource not found: " + fontName);
                    }
                } catch (IOException e) {
                    Opium.LOGGER.error("Failed to copy default font: " + fontName, e);
                }
            }
        }

        // Load all fonts from the directory
        List<Font> fonts = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fontsDir, "*.{ttf,otf}")) {
            for (Path filePath : stream) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, filePath.toFile());
                    fonts.add(font);
                } catch (FontFormatException | IOException e) {
                    Opium.LOGGER.error("Error loading font: " + filePath, e);
                }
            }
        } catch (IOException e) {
            Opium.LOGGER.error("Failed to read fonts directory: " + fontsDir, e);
        }

        return fonts.toArray(new Font[0]);
    }
}
