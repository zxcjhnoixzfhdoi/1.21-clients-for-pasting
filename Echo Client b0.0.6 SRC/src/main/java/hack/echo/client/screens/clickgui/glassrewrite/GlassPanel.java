package hack.echo.client.screens.clickgui.glassrewrite;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.animation.Vec2Animation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix4f;

import java.util.List;

import static hack.echo.client.Echo.featureManager;
import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassPanel implements Imports {

    private static final float HEADER_ICON_SIZE = 8f;
    private static final float HEADER_ICON_GAP = 4f;

    private final Category category;
    private final List<GlassModuleElement> moduleElements = new ObjectArrayList<>();

    @Getter
    @Setter
    private float x;
    @Getter
    @Setter
    private float y;
    @Getter
    private final float width = PANEL_WIDTH;
    @Getter
    private float height;

    private float scroll = 0f;
    private float targetScroll = 0f;
    private float maxScroll = 0f;
    private static final float SCROLL_ANIMATION_MS = 320f;
    private final Animation scrollAnimation = new Animation(Easing.EASE_OUT_BACK, SCROLL_ANIMATION_MS);

    public boolean expanded = true;
    private GlassModuleElement expandedModule = null;

    @Setter
    private int index;

    private final GlassScreen screen;
    @Getter
    private final Vec2Animation positionAnimation;
    private final Animation heightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 400);

    public GlassPanel(GlassScreen screen, Category category, float x, float y, int i) {
        this.screen = screen;
        this.category = category;
        this.x = x;
        this.y = y;
        this.height = PANEL_HEADER_HEIGHT;
        this.positionAnimation = new Vec2Animation(Easing.EASE_OUT_CUBIC, 500);
        this.positionAnimation.start(this.x, this.y, screen.getSlotX(i), y);
        this.index = i;

        for (Feature feature : featureManager.getFeaturesByCategory(category)) {
            moduleElements.add(new GlassModuleElement(screen, feature));
        }

        updateHeight();
    }

    public void setTarget(float x, float y) {
        this.positionAnimation.start(this.x, this.y, x, y);
    }

    public void render(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float alpha, CrossTexture texture) {
        if (!positionAnimation.isFinished() && screen.draggingPanel != this.index) {
            this.x = positionAnimation.getX();
            this.y = positionAnimation.getY();
        }

        updateHeight();
        this.height = (float) heightAnimation.getDelta();

        int bg = wa(SURFACE_PANEL.getRGB(), alpha);
        if (texture != null) {
            draw.screenImage(mat, texture, x, y, width, height, RADIUS_LG, alpha);
        }

        draw.rect(mat, x, y, width, height, RADIUS_LG, bg);

        int textColor = argbMul(TEXT_ON_SURFACE_PRIMARY, alpha);
        draw.text(Fonts.interSemiBold, mat, category.name.toString(), x + PADDING, y + PANEL_HEADER_HEIGHT / 2 - 5, 8, textColor);

        float rightEdge = x + PANEL_WIDTH - PADDING;
        if (!category.icon.isEmpty() && Fonts.lucide != null) {
            float categoryIconWidth = Fonts.lucide.getWidth(category.icon.toString(), HEADER_ICON_SIZE);
            float categoryIconX = rightEdge - categoryIconWidth;
            draw.text(
                    Fonts.lucide,
                    mat,
                    category.icon,
                    categoryIconX,
                    y + PANEL_HEADER_HEIGHT / 2 - 4,
                    HEADER_ICON_SIZE,
                    textColor
            );
            rightEdge = categoryIconX - HEADER_ICON_GAP;
        }

        if (shouldRenderEditButton(mouseX, mouseY) && Fonts.lucide != null) {
            float pencilWidth = Fonts.lucide.getWidth(TextLuicideConstants.pencil, HEADER_ICON_SIZE);
            int pencilColor = screen.isEditingPanel(this)
                    ? argbMul(TEXT_ON_SURFACE_PRIMARY, alpha)
                    : argbMul(TEXT_ON_SURFACE_SECONDARY, alpha);

            draw.text(
                    Fonts.lucide,
                    mat,
                    TextLuicideConstants.pencil,
                    rightEdge - pencilWidth,
                    y + PANEL_HEADER_HEIGHT / 2 - 4,
                    HEADER_ICON_SIZE,
                    pencilColor
            );
        }

        if (height - PANEL_HEADER_HEIGHT - 4 <= 0) {
            return;
        }

        draw.pushScissor(x, y + PANEL_HEADER_HEIGHT, PANEL_WIDTH, height - PANEL_HEADER_HEIGHT);
        renderModules(draw, mat, x, y, mouseX, mouseY, alpha);
        draw.popScissor();
    }

    private void renderModules(Draw2D draw, Matrix4f mat, float x, float y, int mouseX, int mouseY, float alpha) {
        List<GlassModuleElement> displayElements = getDisplayElements();

        float totalHeight = 0f;
        for (GlassModuleElement element : displayElements) {
            element.updatePosition(x, 0f);
            totalHeight += getElementHeight(element) + MODULE_SPACING;
        }

        maxScroll = Math.max(0f, totalHeight - (height - PANEL_HEADER_HEIGHT - 4));

        scroll = (float) scrollAnimation.getDelta();
        if (scroll > maxScroll) {
            scroll = maxScroll;
            targetScroll = maxScroll;
            scrollAnimation.start(maxScroll, maxScroll);
        }

        float moduleY = y + PANEL_HEADER_HEIGHT + 2 - scroll;
        boolean editing = screen.isEditingPanel(this);

        for (GlassModuleElement element : displayElements) {
            element.updatePosition(x, moduleY);
            float elementHeight = getElementHeight(element);

            if (moduleY + elementHeight >= y + PANEL_HEADER_HEIGHT && moduleY <= y + height) {
                element.render(draw, mat, mouseX, mouseY, alpha, editing);
            }

            moduleY += elementHeight + MODULE_SPACING;
        }
    }

    public void updateHeight() {
        float target = PANEL_HEADER_HEIGHT;

        if (expanded) {
            target += 2f;
            for (GlassModuleElement element : getDisplayElements()) {
                target += getElementHeight(element) + MODULE_SPACING;
                if (target >= MAX_PANEL_HEIGHT) {
                    target = MAX_PANEL_HEIGHT;
                    break;
                }
            }
        }

        heightAnimation.updateTo(target);
    }

    public boolean onClicked(MouseButtonEvent event) {
        if (!expanded) {
            return false;
        }

        boolean editing = screen.isEditingPanel(this);

        for (GlassModuleElement element : getDisplayElements()) {
            float elementHeight = getElementHeight(element);
            if (!isInside(element.getX(), element.getY(), element.getWidth(), elementHeight, (float) event.x(), (float) event.y())) {
                continue;
            }

            if (editing) {
                return handleVisibilityEditClick(element, event);
            }

            if (event.y() <= element.getY() + MODULE_HEIGHT) {
                return handleNormalModuleClick(element, event);
            }

            return element.onClicked(event);
        }

        return false;
    }

    private boolean handleVisibilityEditClick(GlassModuleElement element, MouseButtonEvent event) {
        if (event.button() != 0) {
            return true;
        }

        boolean visible = !element.getFeature().isVisible();
        element.getFeature().setVisible(visible);

        if (!visible && expandedModule == element) {
            expandedModule.expanded = false;
            expandedModule = null;
        }

        updateHeight();
        SoundUtil.playClick();
        return true;
    }

    private boolean handleNormalModuleClick(GlassModuleElement element, MouseButtonEvent event) {
        if (event.button() == 0) {
            element.getFeature().toggle();
            SoundUtil.playToggle(element.getFeature().isEnabled());
            return true;
        }

        if (event.button() == 1 && !element.getFeature().settings.isEmpty()) {
            boolean opening = !element.expanded;
            if (opening && expandedModule != null && expandedModule != element) {
                expandedModule.expanded = false;
                SoundUtil.playExpand(false);
            }

            element.expanded = opening;
            expandedModule = opening ? element : null;
            SoundUtil.playExpand(opening);
            updateHeight();
            return true;
        }

        if (event.button() == 2) {
            screen.binding = element;
            SoundUtil.playClick();
            return true;
        }

        return false;
    }

    public void mouseReleased(double x, double y, int button) {
        moduleElements.stream().anyMatch(element -> element.mouseReleased(x, y, button));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!screen.isEditingPanel(this)) {
            float moduleY = y + PANEL_HEADER_HEIGHT + 2 - scroll;
            for (GlassModuleElement element : getDisplayElements()) {
                float elementHeight = getElementHeight(element);
                if (element.expanded && mouseY >= moduleY && mouseY <= moduleY + elementHeight) {
                    if (element.mouseScrolled(mouseX, mouseY, amount)) {
                        return true;
                    }
                }
                moduleY += elementHeight + MODULE_SPACING;
            }
        }

        float mul = screen.gui != null ? screen.gui.glassScrollSpeed.getValue() : SCROLL_MULTIPLIER;
        float currentScroll = (float) scrollAnimation.getDelta();
        float nextTarget = (float) Math.clamp(targetScroll - amount * mul, 0, maxScroll);
        if (nextTarget == targetScroll) {
            return false;
        }

        targetScroll = nextTarget;
        scrollAnimation.start(currentScroll, targetScroll);
        SoundUtil.playScroll();
        return true;
    }

    public void charTyped(char chr, int modifiers) {
        moduleElements.stream().anyMatch(element -> element.charTyped(chr, modifiers));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return moduleElements.stream().anyMatch(element -> element.keyPressed(keyCode, scanCode, modifiers));
    }

    public boolean isEditButtonHovered(double mouseX, double mouseY) {
        if (Fonts.lucide == null) {
            return false;
        }

        if (!screen.isEditingPanel(this) && !isInside(x, y, width, PANEL_HEADER_HEIGHT, (float) mouseX, (float) mouseY)) {
            return false;
        }

        float categoryIconWidth = category.icon.isEmpty() ? 0f : Fonts.lucide.getWidth(category.icon.toString(), HEADER_ICON_SIZE);
        float pencilWidth = Fonts.lucide.getWidth(TextLuicideConstants.pencil, HEADER_ICON_SIZE);
        float gap = categoryIconWidth > 0f ? HEADER_ICON_GAP : 0f;
        float pencilX = x + PANEL_WIDTH - PADDING - categoryIconWidth - gap - pencilWidth;
        float pencilY = y + PANEL_HEADER_HEIGHT / 2 - 4;

        return isInside(pencilX - 2f, pencilY - 1f, pencilWidth + 4f, HEADER_ICON_SIZE + 2f, (float) mouseX, (float) mouseY);
    }

    private boolean shouldRenderEditButton(int mouseX, int mouseY) {
        if (Fonts.lucide == null) {
            return false;
        }

        if (screen.isEditingPanel(this)) {
            return true;
        }

        return isInside(x, y, width, height, mouseX, mouseY);
    }

    private List<GlassModuleElement> getDisplayElements() {
        if (screen.isEditingPanel(this)) {
            return moduleElements;
        }

        List<GlassModuleElement> visibleElements = new ObjectArrayList<>();
        for (GlassModuleElement element : moduleElements) {
            if (!element.getFeature().isVisible()) {
                continue;
            }
            visibleElements.add(element);
        }
        return visibleElements;
    }

    private float getElementHeight(GlassModuleElement element) {
        if (screen.isEditingPanel(this)) {
            return MODULE_HEIGHT;
        }

        return element.getHeight();
    }
}
