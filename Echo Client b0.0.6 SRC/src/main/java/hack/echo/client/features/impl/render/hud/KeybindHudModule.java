package hack.echo.client.features.impl.render.hud;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.Feature;
import hack.echo.client.features.HudFeature;
import hack.echo.client.features.settings.Setting;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.strings.Concat;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

// Todo: make abstraction class to have multiple themes or wahtever
public class KeybindHudModule extends HudFeature {

    private final ModeSetting mode = new ModeSetting(Concat.of("Mode"), Concat.of("Individual"), Concat.of("Box"), Concat.of("Individual"));
    private final ModeSetting alignment = new ModeSetting(Concat.of("Alignment"), Concat.of("Left"), Concat.of("Left"), Concat.of("Middle"), Concat.of("Right"));
    private final ModeSetting valueSide = new ModeSetting(Concat.of("Value Side"), Concat.of("Right"), Concat.of("Left"), Concat.of("Right"));
    private final BoolSetting icons = new BoolSetting(Concat.of("Icons"), false);

    private final BoolSetting uglyHeaderLine = new BoolSetting(Concat.of("Header"), false, o -> mode.is(Concat.of("Box")));

    private float cachedWidth = PANEL_WIDTH_HUD_ELEMENT;
    private float cachedHeight = PANEL_HEADER_HEIGHT;

    public KeybindHudModule() {
        super(new FeatureInfo(
                Concat.of("Keybinds"),
                Concat.of("Display active keybinds"),
                Category.RENDER
        ));
    }

    @Override
    public float getWidth() {
        return cachedWidth;
    }

    @Override
    public float getHeight() {
        return cachedHeight;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onRender2D(EventRender2DGui e) {
        boolean renderIcons = icons.getValue();
        if (Fonts.inter == null) return;
        if (renderIcons && Fonts.lucide == null) return;
        if (mc.debugEntries.isOverlayVisible()) return;
        List<BindEntry> binds = getAllBinds();

        if (binds.isEmpty()) return;

        if (mode.is(Concat.of("Individual"))) {
            renderIndividual(e, binds, renderIcons);
            return;
        }

        renderBox(e, binds, renderIcons);
    }

    private void renderBox(EventRender2DGui e, List<BindEntry> binds, boolean renderIcons) {
        float width = PANEL_WIDTH_HUD_ELEMENT;
        float height = PANEL_HEADER_HEIGHT + (binds.isEmpty() ? 0 : binds.size() * HUD_ELEMENT_HEIGHT + 4);

        cachedWidth = width;
        cachedHeight = height;

        float currentX = getX();
        float currentY = getY();

        var draw = e.getDraw2D();
        Matrix4f mat = new Matrix4f();

        draw.screenImage(mat, e.getBlurTexture(), currentX, currentY, width, height, RADIUS_MD, 1f);
        draw.rect(mat, currentX, currentY, width, height, RADIUS_MD, SURFACE_PANEL.getRGB());

        if (renderIcons) {
            draw.text(Fonts.lucide, mat, TextLuicideConstants.keyboard, currentX + PADDING, centerTextY(currentY, PANEL_HEADER_HEIGHT, Fonts.lucide, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));
        }
        CharSequence title = Concat.of("Keybinds");
        draw.text(Fonts.inter, mat, title, currentX + width / 2f - Fonts.inter.getWidth(title, 8) / 2f, centerTextY(currentY, PANEL_HEADER_HEIGHT, Fonts.inter, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));
        if (uglyHeaderLine.getValue()) {
            draw.rect(mat, currentX + PADDING, currentY + PANEL_HEADER_HEIGHT - 2, width - PADDING * 2, 1, 0, argb(TEXT_ON_SURFACE_SECONDARY));
        }

        if (!binds.isEmpty()) {
            draw.pushScissor(currentX, currentY + PANEL_HEADER_HEIGHT, width, height - PANEL_HEADER_HEIGHT);

            float moduleY = currentY + PANEL_HEADER_HEIGHT + 2;

            for (BindEntry bind : binds) {
                float iconWidth = 0f;
                float indent = 0f;
                if (renderIcons) {
                    String icon = bind.isSetting ? TextLuicideConstants.cornerDownRight : bind.category.icon.toString();
                    iconWidth = Fonts.lucide.getWidth(icon, 8);
                    indent = bind.isSetting ? 2.8f : 0f;
                }

                String keyName = bind.keyName;
                float nameWidth = Fonts.inter.getWidth(bind.name, 8);
                float keyWidth = Fonts.inter.getWidth(keyName, 8);
                float iconArea = renderIcons ? iconWidth + 4f + indent : 0f;
                boolean valueLeft = valueSide.is(Concat.of("Left"));
                float keyX = valueLeft ? currentX + PADDING : currentX + width - PADDING - keyWidth;
                float nameAreaStart = valueLeft ? keyX + keyWidth + VALUE_GAP : currentX + PADDING;
                float nameAreaEnd = valueLeft ? currentX + width - PADDING : keyX - VALUE_GAP;
                float nameAreaWidth = Math.max(0f, nameAreaEnd - nameAreaStart);
                float nameBlockWidth = iconArea + nameWidth;
                float nameBlockX = alignWithin(nameAreaStart, nameAreaWidth, nameBlockWidth);

                if (renderIcons) {
                    String icon = bind.isSetting ? TextLuicideConstants.cornerDownRight : bind.category.icon.toString();
                    draw.text(Fonts.lucide, mat, icon, nameBlockX + indent, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.lucide, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));
                }

                float nameX = nameBlockX + iconArea;
                boolean rowHighlighted = bind.isSetting ? bind.bindActive : bind.featureEnabled;
                int nameColor = rowHighlighted ? ACCENT_FEATURE_ACTIVE.getRGB() : argb(TEXT_ON_SURFACE_PRIMARY);
                int keyColor = bind.isSetting && bind.bindActive ? ACCENT_FEATURE_ACTIVE.getRGB() : argb(TEXT_ON_SURFACE_SECONDARY);
                draw.text(Fonts.inter, mat, bind.name, nameX, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.inter, 8f), 8, nameColor);
                draw.text(Fonts.inter, mat, keyName, keyX, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.inter, 8f), 8, keyColor);

                moduleY += HUD_ELEMENT_HEIGHT;
            }

            draw.popScissor();
        }
    }

    private void renderIndividual(EventRender2DGui e, List<BindEntry> binds, boolean renderIcons) {
        float rowSpacing = MODULE_SPACING;
        float headerWidth = PANEL_WIDTH_HUD_ELEMENT;
        float rowsHeight = binds.isEmpty() ? 0f : 2f + binds.size() * HUD_ELEMENT_HEIGHT + Math.max(0, binds.size() - 1) * rowSpacing;
        float height = PANEL_HEADER_HEIGHT + rowsHeight;

        cachedWidth = headerWidth;
        cachedHeight = height;

        float currentX = getX();
        float currentY = getY();

        var draw = e.getDraw2D();
        Matrix4f mat = new Matrix4f();

        draw.screenImage(mat, e.getBlurTexture(), currentX, currentY, headerWidth, PANEL_HEADER_HEIGHT, RADIUS_MD, 1f);
        draw.rect(mat, currentX, currentY, headerWidth, PANEL_HEADER_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());

        if (renderIcons) {
            draw.text(Fonts.lucide, mat, TextLuicideConstants.keyboard, currentX + PADDING, centerTextY(currentY, PANEL_HEADER_HEIGHT, Fonts.lucide, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));
        }
        CharSequence title = Concat.of("Keybinds");
        draw.text(Fonts.inter, mat, title, currentX + headerWidth / 2f - Fonts.inter.getWidth(title, 8) / 2f, centerTextY(currentY, PANEL_HEADER_HEIGHT, Fonts.inter, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));

        float moduleY = currentY + PANEL_HEADER_HEIGHT + 2f;
        for (BindEntry bind : binds) {
            float rowWidth = getIndividualRowWidth(bind, renderIcons);
            float rowX = currentX + getIndividualRowOffset(headerWidth, rowWidth);
            float namePillWidth = getIndividualNamePillWidth(bind, renderIcons);
            float keyPillWidth = getIndividualKeyPillWidth(bind);
            boolean valueLeft = valueSide.is(Concat.of("Left"));
            float namePillX = valueLeft ? rowX + keyPillWidth + INDIVIDUAL_PILL_GAP : rowX;
            float keyPillX = valueLeft ? rowX : rowX + namePillWidth + INDIVIDUAL_PILL_GAP;

            draw.screenImage(mat, e.getBlurTexture(), namePillX, moduleY, namePillWidth, HUD_ELEMENT_HEIGHT, RADIUS_MD, 1f);
            draw.rect(mat, namePillX, moduleY, namePillWidth, HUD_ELEMENT_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());

            float iconWidth = 0f;
            float indent = 0f;
            if (renderIcons) {
                String icon = bind.isSetting ? TextLuicideConstants.cornerDownRight : bind.category.icon.toString();
                iconWidth = Fonts.lucide.getWidth(icon, 8);
                indent = bind.isSetting ? 2.8f : 0f;
                draw.text(Fonts.lucide, mat, icon, namePillX + PADDING + indent, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.lucide, 8f), 8, argb(TEXT_ON_SURFACE_PRIMARY));
            }

            float nameX = namePillX + PADDING + (renderIcons ? iconWidth + 4f + indent : 0f);
            boolean rowHighlighted = bind.isSetting ? bind.bindActive : bind.featureEnabled;
            int nameColor = rowHighlighted ? ACCENT_FEATURE_ACTIVE.getRGB() : argb(TEXT_ON_SURFACE_PRIMARY);
            draw.text(Fonts.inter, mat, bind.name, nameX, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.inter, 8f), 8, nameColor);

            String keyName = bind.keyName;
            float keyWidth = Fonts.inter.getWidth(keyName, 8);
            draw.screenImage(mat, e.getBlurTexture(), keyPillX, moduleY, keyPillWidth, HUD_ELEMENT_HEIGHT, RADIUS_MD, 1f);
            draw.rect(mat, keyPillX, moduleY, keyPillWidth, HUD_ELEMENT_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());
            int keyColor = bind.isSetting && bind.bindActive ? ACCENT_FEATURE_ACTIVE.getRGB() : argb(TEXT_ON_SURFACE_SECONDARY);
            draw.text(Fonts.inter, mat, keyName, keyPillX + (keyPillWidth - keyWidth) / 2f, centerTextY(moduleY, HUD_ELEMENT_HEIGHT, Fonts.inter, 8f), 8, keyColor);

            moduleY += HUD_ELEMENT_HEIGHT + rowSpacing;
        }
    }

    private float getIndividualRowWidth(BindEntry bind, boolean renderIcons) {
        float leftPillWidth = getIndividualNamePillWidth(bind, renderIcons);
        float keyPillWidth = getIndividualKeyPillWidth(bind);
        return leftPillWidth + INDIVIDUAL_PILL_GAP + keyPillWidth;
    }

    private float centerTextY(float y, float height, Font font, float size) {
        return y + (height - font.getLineHeight(size)) / 2f;
    }

    private float getIndividualNamePillWidth(BindEntry bind, boolean renderIcons) {
        float iconWidth = 0f;
        float indent = 0f;
        if (renderIcons) {
            String icon = bind.isSetting ? TextLuicideConstants.cornerDownRight : bind.category.icon.toString();
            iconWidth = Fonts.lucide.getWidth(icon, 8);
            indent = bind.isSetting ? 2.8f : 0f;
        }

        float nameWidth = Fonts.inter.getWidth(bind.name, 8);
        float iconArea = renderIcons ? iconWidth + 4f + indent : 0f;
        return PADDING + iconArea + nameWidth + PADDING;
    }

    private float getIndividualKeyPillWidth(BindEntry bind) {
        float keyWidth = Fonts.inter.getWidth(bind.keyName, 8);
        return keyWidth + 8f;
    }

    private float getIndividualRowOffset(float headerWidth, float rowWidth) {
        if (alignment.is(Concat.of("Middle"))) {
            return (headerWidth - rowWidth) / 2f;
        }
        if (alignment.is(Concat.of("Right"))) {
            return headerWidth - rowWidth;
        }
        return 0f;
    }

    private float alignWithin(float start, float available, float contentWidth) {
        if (alignment.is(Concat.of("Middle"))) {
            return start + (available - contentWidth) / 2f;
        }
        if (alignment.is(Concat.of("Right"))) {
            return start + (available - contentWidth);
        }
        return start;
    }

    private List<BindEntry> getAllBinds() {
        List<BindEntry> list = new ArrayList<>();

        for (Feature feature : Echo.featureManager.getFeatures()) {
            boolean featureHasBind = feature.getKey() != -1;
            List<BindEntry> settingBinds = new ArrayList<>();

            if (feature.isEnabled()) {
                for (Setting setting : feature.settings) {
                    if (setting instanceof KeybindSetting keySetting) {
                        if (keySetting.getKey() != -1) {
                            int bindCode = keySetting.getKey();
                            settingBinds.add(new BindEntry(
                                keySetting.getName(),
                                ClientUtil.keyLabel(bindCode),
                                feature.getCategory(),
                                true,
                                feature.isEnabled(),
                                InputHandler.isBindDown(bindCode)
                            ));
                        }
                    }
                }
            }

            if (featureHasBind || !settingBinds.isEmpty()) {
                String keyLabel = featureHasBind ? ClientUtil.keyLabel(feature.getKey()) : Concat.of("-").toString();
                list.add(new BindEntry(feature.getName(), keyLabel, feature.getCategory(), false, feature.isEnabled(), false));

                list.addAll(settingBinds);
            }
        }

//        list.sort(Comparator.comparing((BindEntry e) -> e.name.toString()));

        return list;
    }

    private record BindEntry(CharSequence name, String keyName, Category category, boolean isSetting, boolean featureEnabled, boolean bindActive) {}

}
