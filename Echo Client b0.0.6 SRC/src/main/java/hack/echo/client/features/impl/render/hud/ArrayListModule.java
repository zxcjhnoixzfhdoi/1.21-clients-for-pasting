package hack.echo.client.features.impl.render.hud;

import hack.echo.client.config.ColorConfig;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.HudFeature;
import hack.echo.client.features.FeatureManager;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.strings.Concat;
import org.joml.Matrix4f;

import java.util.Comparator;
import java.util.List;

public class ArrayListModule extends HudFeature {

    private static final float FONT_SIZE = 8f;
    private static final float LINE_HEIGHT = 10f;
    private static final float INFO_SPACING = 2f;
    private static final float BACKGROUND_PADDING_X = 3f;
    private static final float BACKGROUND_PADDING_Y = 1f;
    private static final float TEXT_Y_OFFSET = -1f;
    private float width;
    private float height;

    public ArrayListModule() {
        super(new FeatureInfo(
                Concat.of("Array List"),
                Concat.of("Display enabled modules list"),
                Category.RENDER
        ));
        getXPosition().setValue(1f);
        getYPosition().setValue(0.01f);
    }

    @Override
    public float getWidth() { return width; }

    @Override
    public float getHeight() { return height; }

    public static float arrayListBottom = 0;

    private final BoolSetting arrayList = new BoolSetting(Concat.of("ArrayList"), true);
    private final ColorSetting arrayListPrimaryColor = new ColorSetting(Concat.of("List Primary Color"),
            ColorConfig.TEXT_WHITE.getRGB(), o -> arrayList.getValue());
    private final BoolSetting suffixEnabled = new BoolSetting(Concat.of("Suffix Enabled"), true, o -> arrayList.getValue());
    private final BoolSetting backgroundEnabled = new BoolSetting(Concat.of("Background"), true, o -> arrayList.getValue());
    private final FloatSetting backgroundRadius = new FloatSetting(Concat.of("Background Radius"), 3f, 0f, 3f, 0.5f,
            o -> arrayList.getValue() && backgroundEnabled.getValue());
    private final BoolSetting lowercaseEnabled = new BoolSetting(Concat.of("Lowercase"), false, o -> arrayList.getValue());
    private final ColorSetting arrayListSecondaryColor = new ColorSetting(Concat.of("List Secondary Color"),
            ColorConfig.TEXT_LIGHT_GRAY.getRGB(), o -> arrayList.getValue() && suffixEnabled.getValue());

    @Override
    public void onDisable() {
        arrayListBottom = 0;
        super.onDisable();
    }

    @EventSubscribe
    public void onRender2D(EventRender2DGui event) {
        if (mc.debugEntries.isOverlayVisible()) {
            arrayListBottom = 0;
            return;
        }

        if (!arrayList.getValue()) {
            arrayListBottom = 0;
            return;
        }

        Draw2D draw = event.getDraw2D();
        Matrix4f mat = new Matrix4f();
        renderClassic(draw, mat);
    }

    private void renderClassic(Draw2D draw, Matrix4f mat) {
        if (Fonts.interSemiBold == null) {
            arrayListBottom = 0;
            return;
        }

        List<Feature> features = FeatureManager.safeFeatures().stream()
                .filter(Feature::isEnabled)
                .filter(f -> f != this)
                .sorted(Comparator.comparingDouble(this::getFeatureWidth).reversed())
                .toList();

        if (features.isEmpty()) {
            arrayListBottom = 0;
            return;
        }

        float maxWidth = 0f;
        for (Feature feature : features) {
            maxWidth = Math.max(maxWidth, getFeatureWidth(feature));
        }

        this.width = maxWidth;
        height = features.size() * LINE_HEIGHT;

        float baseX = getX();
        float currentY = getY();

        for (int index = 0; index < features.size(); index++) {
            Feature feature = features.get(index);
            CharSequence name = getDisplayName(feature);
            CharSequence info = getDisplayInfo(feature);
            float itemWidth = getFeatureWidth(feature);
            float itemX = baseX + this.width - itemWidth;

            if (backgroundEnabled.getValue()) {
                float nextWidth = index + 1 < features.size() ? getFeatureWidth(features.get(index + 1)) : -1f;
                float bottomLeftRadius = getBottomLeftRadius(itemWidth, nextWidth);

                draw.rect(mat, itemX, currentY, itemWidth, LINE_HEIGHT,
                        0f, 0f, bottomLeftRadius, 0f, ColorConfig.BACKGROUND_DARK.getRGB());
            }

            float textX = itemX;
            if (backgroundEnabled.getValue()) {
                textX += BACKGROUND_PADDING_X;
            }

            float textY = currentY + (LINE_HEIGHT - FONT_SIZE) / 2f + TEXT_Y_OFFSET;
            float nameWidth = Fonts.interSemiBold.getWidth(name, FONT_SIZE);

            if (info.length() > 0) {
                float infoX = textX + nameWidth + INFO_SPACING;
                draw.textWithShadow(Fonts.interSemiBold, mat, info, infoX, textY,
                        FONT_SIZE, arrayListSecondaryColor.getARGB());
            }

            draw.textWithShadow(Fonts.interSemiBold, mat, name, textX, textY, FONT_SIZE,
                    arrayListPrimaryColor.getARGB());
            currentY += LINE_HEIGHT;
        }
        arrayListBottom = currentY;
    }

    private float getFeatureWidth(Feature feature) {
        if (Fonts.interSemiBold == null)
            return 0f;

        CharSequence name = getDisplayName(feature);
        CharSequence info = getDisplayInfo(feature);
        float width = Fonts.interSemiBold.getWidth(name, FONT_SIZE);
        if (info != null && info.length() > 0) {
            width += Fonts.interSemiBold.getWidth(info, FONT_SIZE) + INFO_SPACING;
        }

        if (!backgroundEnabled.getValue()) {
            return width;
        }

        return width + (BACKGROUND_PADDING_X * 2f);
    }

    private CharSequence getDisplayName(Feature feature) {
        return formatDisplayText(feature.getName());
    }

    private CharSequence getDisplayInfo(Feature feature) {
        CharSequence concat = feature.concat();
        if (concat == null || concat.isEmpty()) {
            return "";
        }

        return formatDisplayText(concat);
    }

    private CharSequence formatDisplayText(CharSequence text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (!lowercaseEnabled.getValue()) {
            return text;
        }

        if (text instanceof Concat concat) {
            return concat.toLowerCase();
        }

        return text.toString().toLowerCase();
    }

    private float getBottomLeftRadius(float currentWidth, float nextWidth) {
        if (nextWidth < 0f) {
            return backgroundRadius.getValue();
        }

        float exposedWidth = currentWidth - nextWidth;
        if (exposedWidth <= 0f) {
            return 0f;
        }

        return Math.min(backgroundRadius.getValue(), exposedWidth);
    }
}
