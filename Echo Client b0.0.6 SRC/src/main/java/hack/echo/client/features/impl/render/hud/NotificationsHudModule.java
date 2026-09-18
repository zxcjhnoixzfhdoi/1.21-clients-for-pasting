package hack.echo.client.features.impl.render.hud;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.FeatureManager;
import hack.echo.client.features.HudFeature;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.strings.Concat;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class NotificationsHudModule extends HudFeature {
    private static final float FONT_SIZE = 7f;
    private static final float ICON_SIZE = 7f;
    private static final float NOTIFICATION_HEIGHT = 16f;
    private static final float MARGIN_RIGHT = 10f;
    private static final float MARGIN_BOTTOM = 10f;
    private static final float NOTIFICATION_SPACING = 4f;
    private static final float ACCENT_WIDTH = 1.5f;
    private static final float CONTENT_SPACING = 6f;
    private static final float ICON_SPACING = 3f;
    private static final long DISPLAY_DURATION_MS = 2200L;
    private static final float SLIDE_DURATION_MS = 220f;
    private static final int MAX_NOTIFICATIONS = 6;

    private final Deque<Notification> notifications = new ArrayDeque<>();

    public NotificationsHudModule() {
        super(new FeatureInfo(
            Concat.of("Notifications"),
            Concat.of("Show module toggle notifications"),
            Category.RENDER
        ));
        getXPosition().setValue(1f);
        getYPosition().setValue(1f);
    }

    private float width;
    private float height;

    @Override
    public float getWidth() { return width; }

    @Override
    public float getHeight() { return height; }

    public static void notifyToggle(Feature feature, boolean enabled) {
        FeatureManager.safeGetFeature(NotificationsHudModule.class)
            .ifPresent(module -> module.enqueue(feature, enabled));
    }

    private void enqueue(Feature feature, boolean enabled) {
        if (!isEnabled() || feature == null) return;
        notifications.addFirst(new Notification(feature.getName(), enabled));
        while (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.removeLast();
        }
    }

    @Override
    public void onDisable() {
        notifications.clear();
        super.onDisable();
    }

    @EventSubscribe
    private void onRender2D(EventRender2DGui event) {
        if (Fonts.inter == null) return;
        if (mc.debugEntries.isOverlayVisible()) return;
        if (notifications.isEmpty()) return;

        float baseRightX = getX() + getWidth();
        float currentY = getY() + getHeight() - NOTIFICATION_HEIGHT;

        float maxNotifWidth = 0f;
        int visibleCount = 0;

        var draw = event.getDraw2D();
        Matrix4f mat = new Matrix4f();
        var blurred = event.getBlurTexture();
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            notification.update(baseRightX, event.getScaledWidth());
            if (notification.isExpired()) {
                iterator.remove();
                continue;
            }

            maxNotifWidth = Math.max(maxNotifWidth, notification.getWidth());
            visibleCount++;

            float leftX = notification.getRightX() - notification.getWidth();
            drawNotification(draw, mat, blurred, notification, leftX, currentY);
            currentY -= NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }

        if (maxNotifWidth > 0f) width = maxNotifWidth;
        if (visibleCount > 0) height = visibleCount * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING);
    }

    private void drawNotification(Draw2D draw, Matrix4f mat, CrossTexture blurred, Notification notification, float x, float y) {
        int accentColor = notification.isEnabled() ? ACCENT_PRIMARY.getRGB() : SURFACE_INTERACTIVE_MUTED.getRGB();
        draw.screenImage(mat, blurred, x, y, notification.getWidth(), NOTIFICATION_HEIGHT, RADIUS_MD, 1f);
        draw.rect(mat, x, y, notification.getWidth(), NOTIFICATION_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());
        draw.rect(mat, x, y + 2f, ACCENT_WIDTH, NOTIFICATION_HEIGHT - 4f, 0f, accentColor);

        float contentX = x + ACCENT_WIDTH + PADDING;
        float textY = y + NOTIFICATION_HEIGHT / 2f - 4.5f;
        float iconY = y + NOTIFICATION_HEIGHT / 2f - 3.5f;

        if (notification.hasIcon() && Fonts.lucide != null) {
            draw.text(Fonts.lucide, mat, notification.getIcon(), contentX, iconY, ICON_SIZE, argb(TEXT_ON_SURFACE_SECONDARY));
            contentX += notification.getIconWidth() + ICON_SPACING;
        }

        draw.text(Fonts.inter, mat, notification.getName(), contentX, textY, FONT_SIZE, argb(TEXT_ON_SURFACE_PRIMARY));
        draw.text(Fonts.inter, mat, notification.getStatusText(),
            x + notification.getWidth() - PADDING - notification.getStatusWidth(),
            textY, FONT_SIZE, argb(TEXT_ON_SURFACE_SECONDARY));
    }

    private static class Notification {
        private final CharSequence name;
        private final boolean enabled;
        private final String statusText;
        private final String icon;
        private final long createdAt;
        private final Animation animation = new Animation(Easing.EASE_OUT_CUBIC, SLIDE_DURATION_MS);

        private boolean exiting;
        private boolean initialized;
        private float rightX;
        private float width;
        private float nameWidth;
        private float statusWidth;
        private float iconWidth;

        private Notification(CharSequence name, boolean enabled) {
            this.name = name != null ? name : "";
            this.enabled = enabled;
            this.statusText = enabled ? "Enabled" : "Disabled";
            this.icon = TextLuicideConstants.shieldAlert;
            this.createdAt = System.currentTimeMillis();
        }

        private void update(float targetRightX, float screenWidth) {
            updateMetrics();
            float offscreenRightX = screenWidth + MARGIN_RIGHT + width;

            if (!initialized) {
                animation.start(offscreenRightX, targetRightX);
                initialized = true;
            }

            float currentRightX = (float) animation.getDelta();
            long now = System.currentTimeMillis();
            if (!exiting && now - createdAt > DISPLAY_DURATION_MS) {
                exiting = true;
                animation.start(currentRightX, offscreenRightX);
                currentRightX = (float) animation.getDelta();
            }

            rightX = currentRightX;
        }

        private void updateMetrics() {
            nameWidth = Fonts.inter.getWidth(name, FONT_SIZE);
            statusWidth = Fonts.inter.getWidth(statusText, FONT_SIZE);
            iconWidth = (Fonts.lucide != null && icon != null)
                ? Fonts.lucide.getWidth(icon, ICON_SIZE)
                : 0f;

            width = ACCENT_WIDTH + PADDING * 2 + nameWidth + statusWidth + CONTENT_SPACING;
            if (iconWidth > 0f) {
                width += iconWidth + ICON_SPACING;
            }
        }

        private boolean isExpired() {
            return exiting && animation.isFinished();
        }

        private float getRightX() {
            return rightX;
        }

        private float getWidth() {
            return width;
        }

        private CharSequence getName() {
            return name;
        }

        private boolean isEnabled() {
            return enabled;
        }

        private String getStatusText() {
            return statusText;
        }

        private float getStatusWidth() {
            return statusWidth;
        }

        private boolean hasIcon() {
            return iconWidth > 0f;
        }

        private String getIcon() {
            return icon;
        }

        private float getIconWidth() {
            return iconWidth;
        }
    }
}
