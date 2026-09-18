package we.devs.opium.api.manager.miscellaneous;

import me.x150.renderer.font.FontRenderer;
import we.devs.opium.api.utilities.font.FontLoader;
import we.devs.opium.api.utilities.font.FontRenderers;
import we.devs.opium.api.utilities.font.fxFontRenderer;

import java.awt.*;

// rewrote by heedi

public class FontManager {
    private static Font[] fonts;
    public static final int HUD_FONT_SIZE = 8;
    public static final int CLIENT_FONT_SIZE = 8;
    public static final FontManager INSTANCE = new FontManager();

    public FontManager() {
        if (fonts == null) {
            fonts = FontLoader.loadFonts();
        }
    }

    public void refresh() {
        fonts = FontLoader.loadFonts();
    }

    public void registerFonts() {
        if (fonts == null || fonts.length == 0) {
            refresh();
        }

        FontRenderers.fontRenderer = new FontRenderer(fonts, HUD_FONT_SIZE);
        FontRenderers.fxfontRenderer = new fxFontRenderer(fonts, CLIENT_FONT_SIZE);

        float[] sizes = {4f, 6f, 8f, 13f};
        fxFontRenderer[] renderers = {
                FontRenderers.Super_Small_fxfontRenderer,
                FontRenderers.Small_fxfontRenderer,
                FontRenderers.Mid_fxfontRenderer,
                FontRenderers.Large_fxfontRenderer
        };

        for (int i = 0; i < sizes.length; i++) {
            renderers[i] = new fxFontRenderer(fonts, sizes[i]);
        }
    }
}
