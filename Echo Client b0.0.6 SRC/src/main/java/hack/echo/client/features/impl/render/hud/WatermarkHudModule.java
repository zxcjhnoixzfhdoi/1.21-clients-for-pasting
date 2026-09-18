package hack.echo.client.features.impl.render.hud;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.HudFeature;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.config.ColorConfig;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.strings.Concat;
import org.joml.Matrix4f;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class WatermarkHudModule extends HudFeature {

    private final ModeSetting watermarkMode = new ModeSetting(Concat.of("Watermark Mode"), "Glass", "Glass", "Classic");
    private final BoolSetting watermarkShowFps = new BoolSetting(Concat.of("Show FPS"), true);
    private final BoolSetting watermarkShowFrameTime = new BoolSetting(Concat.of("Show Frame Time"), true);
    private final BoolSetting watermarkShowPing = new BoolSetting(Concat.of("Show Ping"), false);
    private final ColorSetting watermarkPrimaryColor = new ColorSetting(Concat.of("Watermark Primary Color"), ColorConfig.SECONDARY_ACCENT.getRGB(), o -> watermarkMode.is("Classic"));
    private final ColorSetting watermarkSecondaryColor = new ColorSetting(Concat.of("Watermark Secondary Color"), ColorConfig.TEXT_LIGHT_GRAY.getRGB(), o -> watermarkMode.is("Classic"));

    private final PerformanceTracker performanceTracker = new PerformanceTracker();

    private float cachedWidth = 60f;
    private float cachedHeight = SETTING_HEIGHT;

    public WatermarkHudModule() {
        super(new FeatureInfo(
            Concat.of("Watermark"),
            Concat.of("Display client watermark with FPS"),
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

    @Override
    public void onEnable() {
        super.onEnable();
        performanceTracker.reset();
    }

    @EventSubscribe
    public void onRender2D(EventRender2DGui event) {
        if (mc.debugEntries.isOverlayVisible()) return;

        performanceTracker.updateFrameTime();
        performanceTracker.updateFps();

        var draw = event.getDraw2D();
        Matrix4f mat = new Matrix4f();
        if (watermarkMode.is("Glass")) {
            renderWatermarkGlass(draw, mat, event);
        } else {
            renderWatermarkClassic(draw, mat, event);
        }
    }

    private void renderWatermarkGlass(Draw2D draw, Matrix4f mat, EventRender2DGui event) {
        if (Fonts.inter == null) return;
        if (watermarkShowPing.getValue() && Fonts.lucide == null) return;

        Concat text = Concat.of("Echo");
        float fontSize = 7f;
        float textWidth = Fonts.inter.getWidth(text, fontSize);

        float accentBarWidth = 1.5f;
        float spacing = 6f;
        float width = accentBarWidth + spacing + textWidth;

        float fpsWidth = 0f;
        Concat fpsText = null;
        if (watermarkShowFps.getValue()) {
            fpsText = Concat.of(Concat.ofInt(performanceTracker.getFps()), Concat.of(" fps"));
            fpsWidth = Fonts.inter.getWidth(fpsText, fontSize);
            width += spacing + fpsWidth;
        }

        float frameTimeWidth = 0f;
        Concat frameTimeText = null;
        if (watermarkShowFrameTime.getValue()) {
            frameTimeText = Concat.of(Concat.ofFixed(performanceTracker.getFrameTimeMs(), 1), Concat.of(" ms"));
            frameTimeWidth = Fonts.inter.getWidth(frameTimeText, fontSize);
            width += spacing + frameTimeWidth;
        }

        float pingWidth = 0f;
        float pingIconWidth = 0f;
        Concat pingText = null;
        if (watermarkShowPing.getValue()) {
            int ping = getPing();
            Concat pingValue = ping < 0 ? Concat.of("-") : Concat.ofInt(ping);
            pingText = Concat.of(pingValue, Concat.of(" ms"));
            pingWidth = Fonts.inter.getWidth(pingText, fontSize);
            pingIconWidth = Fonts.lucide.getWidth(TextLuicideConstants.ethernetPort, fontSize);
            width += spacing + pingIconWidth + 3f + pingWidth;
        }

        width += PADDING;
        cachedWidth = width;
        cachedHeight = SETTING_HEIGHT;

        float x = getX();
        float y = getY();

        draw.screenImage(mat, event.getBlurTexture(), x, y, width, SETTING_HEIGHT, RADIUS_MD, 1f);
        draw.rect(mat, x, y, width, SETTING_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());
        draw.rect(mat, x, y + 2, accentBarWidth, SETTING_HEIGHT - 4, 0, ACCENT_PRIMARY.getRGB());

        float currentX = x + accentBarWidth + spacing;
        float textY = y + SETTING_HEIGHT / 2f - 4.5f;
        float iconY = y + SETTING_HEIGHT / 2f - 3.5f;
        draw.text(Fonts.inter, mat, text, currentX, y + SETTING_HEIGHT / 2 - 4.5f, fontSize, argb(TEXT_ON_SURFACE_PRIMARY));
        currentX += textWidth;

        if (watermarkShowFps.getValue() && fpsText != null) {
            currentX += spacing;
            draw.text(Fonts.inter, mat, fpsText, currentX, y + SETTING_HEIGHT / 2 - 4.5f, fontSize, argb(TEXT_ON_SURFACE_SECONDARY));
            currentX += fpsWidth;
        }

        if (watermarkShowFrameTime.getValue() && frameTimeText != null) {
            currentX += spacing;
            draw.text(Fonts.inter, mat, frameTimeText, currentX, y + SETTING_HEIGHT / 2 - 4.5f, fontSize, argb(TEXT_ON_SURFACE_SECONDARY));
            currentX += frameTimeWidth;
        }

        if (watermarkShowPing.getValue() && pingText != null) {
            currentX += spacing;
            draw.text(Fonts.lucide, mat, TextLuicideConstants.ethernetPort, currentX, iconY, fontSize, argb(TEXT_ON_SURFACE_SECONDARY));
            currentX += pingIconWidth + 3f;
            draw.text(Fonts.inter, mat, pingText, currentX, textY, fontSize, argb(TEXT_ON_SURFACE_SECONDARY));
        }
    }

    private void renderWatermarkClassic(Draw2D draw, Matrix4f mat, EventRender2DGui event) {
        if (Fonts.interSemiBold == null) return;
        if (watermarkShowPing.getValue() && Fonts.lucide == null) return;

        Concat text = Concat.of("Echo");
        float fontSize = 9f;
        float textWidth = Fonts.interSemiBold.getWidth(text, fontSize);
        float spacing = 4f;

        float fpsWidth = 0f;
        Concat fpsText = null;
        if (watermarkShowFps.getValue()) {
            fpsText = Concat.of(Concat.ofInt(performanceTracker.getFps()), Concat.of(" fps"));
            fpsWidth = Fonts.interSemiBold.getWidth(fpsText, fontSize);
        }

        Concat frameTimeText = null;
        if (watermarkShowFrameTime.getValue()) {
            frameTimeText = Concat.of(Concat.ofFixed(performanceTracker.getFrameTimeMs(), 1), Concat.of(" ms"));
        }

        Concat pingText = null;
        float pingIconWidth = 0f;
        float pingTextWidth = 0f;
        if (watermarkShowPing.getValue()) {
            int ping = getPing();
            Concat pingValue = ping < 0 ? Concat.of("-") : Concat.ofInt(ping);
            pingText = Concat.of(pingValue, Concat.of(" ms"));
            pingIconWidth = Fonts.lucide.getWidth(TextLuicideConstants.ethernetPort, fontSize);
            pingTextWidth = Fonts.interSemiBold.getWidth(pingText, fontSize);
        }

        float width = textWidth;
        if (watermarkShowFps.getValue() && fpsText != null) {
            width += spacing + fpsWidth;
        }
        if (watermarkShowFrameTime.getValue() && frameTimeText != null) {
            width += spacing + Fonts.interSemiBold.getWidth(frameTimeText, fontSize);
        }
        if (watermarkShowPing.getValue() && pingText != null) {
            width += spacing + pingIconWidth + 3f + pingTextWidth;
        }

        cachedWidth = width;
        cachedHeight = fontSize + 2f;

        float x = getX();
        float y = getY();

        float currentX = x;
        draw.textWithShadow(Fonts.interSemiBold, mat, text, currentX, y, fontSize, watermarkPrimaryColor.getARGB());
        currentX += textWidth;

        if (watermarkShowFps.getValue() && fpsText != null) {
            currentX += spacing;
            draw.textWithShadow(Fonts.interSemiBold, mat, fpsText, currentX, y, fontSize, watermarkSecondaryColor.getARGB());
            currentX += fpsWidth;
        }

        if (watermarkShowFrameTime.getValue() && frameTimeText != null) {
            currentX += spacing;
            draw.textWithShadow(Fonts.interSemiBold, mat, frameTimeText, currentX, y, fontSize, watermarkSecondaryColor.getARGB());
            currentX += Fonts.interSemiBold.getWidth(frameTimeText, fontSize);
        }

        if (watermarkShowPing.getValue() && pingText != null) {
            currentX += spacing;
            draw.textWithShadow(Fonts.lucide, mat, TextLuicideConstants.ethernetPort, currentX, y + 1f, fontSize, watermarkSecondaryColor.getARGB());
            currentX += pingIconWidth + 3f;
            draw.textWithShadow(Fonts.interSemiBold, mat, pingText, currentX, y, fontSize, watermarkSecondaryColor.getARGB());
        }
    }

    private int getPing() {
        if (mc.player == null || mc.getConnection() == null) return -1;
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) return -1;
        return info.getLatency();
    }

    private static class PerformanceTracker {
        private static final long NANOS_PER_SECOND = 1_000_000_000L;
        private static final long NANOS_PER_MILLISECOND = 1_000_000L;
        private static final long FPS_UPDATE_INTERVAL_MS = 100L;

        private int fps = 0;
        private float frameTimeMs = 0f;

        private long lastSystemUpdateTime = 0;
        private long lastFpsUpdateTime = 0;
        private long lastFrameTimeUpdateTime = 0;
        private long lastFrameStartTime = 0;

        void reset() {
            long now = System.nanoTime();
            lastSystemUpdateTime = now;
            lastFpsUpdateTime = now;
            lastFrameTimeUpdateTime = now;
            lastFrameStartTime = now;
            fps = 0;
            frameTimeMs = 0f;
        }

        void updateFps() {
            long currentTime = System.nanoTime();

            if (lastSystemUpdateTime == 0) {
                lastSystemUpdateTime = currentTime;
                lastFpsUpdateTime = currentTime;
                return;
            }

            long deltaTime = currentTime - lastSystemUpdateTime;
            if (deltaTime <= 0) {
                lastSystemUpdateTime = currentTime;
                return;
            }

            long intervalNs = FPS_UPDATE_INTERVAL_MS * NANOS_PER_MILLISECOND;
            if ((currentTime - lastFpsUpdateTime) >= intervalNs) {
                fps = (int) (NANOS_PER_SECOND / deltaTime);
                lastFpsUpdateTime = currentTime;
            }

            lastSystemUpdateTime = currentTime;
        }

        void updateFrameTime() {
            long currentTime = System.nanoTime();

            if (lastFrameStartTime == 0) {
                lastFrameStartTime = currentTime;
                lastFrameTimeUpdateTime = currentTime;
                return;
            }

            long deltaTime = currentTime - lastFrameStartTime;
            if (deltaTime <= 0) {
                lastFrameStartTime = currentTime;
                return;
            }

            float newFrameTime = deltaTime / (float) NANOS_PER_MILLISECOND;

            long intervalNs = FPS_UPDATE_INTERVAL_MS * NANOS_PER_MILLISECOND;
            if ((currentTime - lastFrameTimeUpdateTime) >= intervalNs) {
                frameTimeMs = newFrameTime;
                lastFrameTimeUpdateTime = currentTime;
            }

            lastFrameStartTime = currentTime;
        }

        int getFps() {
            return fps;
        }

        float getFrameTimeMs() {
            return frameTimeMs;
        }
    }
}
