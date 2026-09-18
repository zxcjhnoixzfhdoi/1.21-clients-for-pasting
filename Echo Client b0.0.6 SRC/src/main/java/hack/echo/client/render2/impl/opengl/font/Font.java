package hack.echo.client.render2.impl.opengl.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.instanced.GlText;
import hack.echo.client.utils.ResourceHelper;
import hack.echo.client.utils.VulkanUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

@Getter
public class Font {

    private final int width;
    private final int height;
    private final float ascender;
    private final float descender;
    private final float lineHeight;
    private final Glyph[] glyphs = new Glyph[1024 * 1024];
    private GlText glText;
    private final CrossTexture texture;

    public Font(String name) throws IOException {
        this.texture = CrossTexture.from("fonts/" + name + ".png");

        try (InputStream stream = ResourceHelper.getStream("fonts/" + name + ".json")) {
            Reader reader = new InputStreamReader(stream);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            this.width = json.getAsJsonObject("atlas").get("width").getAsInt();
            this.height = json.getAsJsonObject("atlas").get("height").getAsInt();

            JsonObject metrics = json.getAsJsonObject("metrics");
            this.ascender = metrics.get("ascender").getAsFloat();
            this.descender = metrics.get("descender").getAsFloat();
            this.lineHeight = metrics.get("lineHeight").getAsFloat();

            for (JsonElement element : json.getAsJsonArray("glyphs")) {
                JsonObject glyphJson = element.getAsJsonObject();
                Glyph glyph = Glyph.parse(glyphJson);
                this.glyphs[glyph.getUnicode()] = glyph;
            }
        }
        if (!VulkanUtil.isVulkanLoaded()) {
            glText = new GlText(this);
        }
    }


    public Glyph getGlyph(int unicode) {
        return glyphs[unicode];
    }

    public float getWidth(CharSequence text, float size) {
        float sum = 0;
        for (int i = 0; i < text.length(); i++) {
            int unicode = Character.codePointAt(text, i);
            Glyph glyph = glyphs[unicode];
            if (glyph != null) {
                sum += size * glyph.getAdvance();
            }
        }
        return sum;
    }

    public void destroy() {
        texture.cleanup();
        if (glText != null) glText.cleanup();
        Arrays.fill(glyphs, null);
    }

    public float getLineHeight(float size) {
        return lineHeight * size;
    }
}
