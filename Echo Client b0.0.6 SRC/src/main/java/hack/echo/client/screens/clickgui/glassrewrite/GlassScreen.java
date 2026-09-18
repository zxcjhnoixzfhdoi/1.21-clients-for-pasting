package hack.echo.client.screens.clickgui.glassrewrite;

import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import hack.echo.client.features.impl.misc.ClickGUI;
import hack.echo.client.mixininterface.IGameRenderer;
import hack.echo.client.api.CharacterEventCompat;
import hack.echo.client.api.GuiGraphicsCompat;
import hack.echo.client.api.ScreenRenderCompat;
import hack.echo.client.screens.ScreenManager;
import hack.echo.client.screens.clickgui.ClickGuiDock;
import hack.echo.client.screens.clickgui.ClickGuiDock.Tab;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//? if >= 26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassScreen extends ScreenRenderCompat implements Imports {

    private final Matrix4f mat = new Matrix4f();
    private final Animation animation = new Animation(Easing.EASE_OUT_BACK, 50);
    private final ClickGuiDock dock = new ClickGuiDock();
    private final GlassConfigMenu configMenu = new GlassConfigMenu(this);
    private final GlassFriendsMenu friendsMenu = new GlassFriendsMenu(this);

    private final List<GlassPanel> panels = new ObjectArrayList<>();
    public int draggingPanel = -1;
    public GlassModuleElement binding = null;
    private boolean initialized = false;
    private boolean editingPanels = false;

    private float dragOffsetX, dragOffsetY;

    private final float[] slots = new float[Category.values().length];
    private final float[] slotsY = new float[Category.values().length];
    private float globalScroll = 0f;
    private float maxGlobalScroll = 0f;
    private static final float GLOBAL_SCROLL_SPEED = 100f;

    public ClickGUI gui;

    public GlassScreen() {
        super(Component.literal(""));
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            gui = Echo.featureManager.getFeatureByClass(ClickGUI.class);

            var c = Category.values();
            for (int i = 0; i < c.length; i++) {
                panels.add(new GlassPanel(this, c[i],
                        getSlotX(i), INITIAL_Y, i));
            }
            initialized = true;
        }
        animation.start(0, 1);
    }

    public float getSlotX(int index) {
        return slots[index];
    }

    public float getSlotY(int index) {
        return slotsY[index];
    }

    //? if >= 26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor context, int mx, int my, float f) {
    *///?} else {
    @Override
    public void render(GuiGraphics context, int mx, int my, float f) {
        //?}
        if (animation.getFrom() == 1 && animation.isFinished()) {
            this.onClose();
            return;
        }

        int cmx = (int) getMouseX(mx);
        int cmy = (int) getMouseY(my);

        computeLayout();
        handleDrag(cmx, cmy);
        animatePanelPositions();
        renderPanels(cmx, cmy, (float) animation.getDelta());
    }

    private void computeLayout() {
        int panelsPerRow = Math.max(1, (int) ((RenderUtil.getScaledWidth() - 24) / (PANEL_WIDTH + PANEL_SPACING)));
        float totalRowWidth = Math.min(panels.size(), panelsPerRow) * PANEL_WIDTH
                + (Math.min(panels.size(), panelsPerRow) - 1) * PANEL_SPACING;
        float startX = (RenderUtil.getScaledWidth() - totalRowWidth) / 2f;

        List<Float> rowHeights = new ArrayList<>();
        int columnIndex = 0;
        float maxHeightInRow = 0f;

        for (GlassPanel panel : panels) {
            maxHeightInRow = Math.max(maxHeightInRow, panel.getHeight());
            columnIndex++;
            if (columnIndex >= panelsPerRow) {
                rowHeights.add(maxHeightInRow);
                maxHeightInRow = 0f;
                columnIndex = 0;
            }
        }
        if (columnIndex > 0) rowHeights.add(maxHeightInRow);

        float posX = startX;
        float posY = dock.getBottomY() - globalScroll;
        int rowIndex = 0;
        columnIndex = 0;

        for (int i = 0; i < panels.size(); i++) {
            slots[i] = posX;
            slotsY[i] = posY;

            columnIndex++;
            if (columnIndex >= panelsPerRow) {
                columnIndex = 0;
                posX = startX;
                posY += rowHeights.get(rowIndex) + PANEL_SPACING;
                rowIndex++;
            } else {
                posX += PANEL_WIDTH + PANEL_SPACING;
            }
        }

        float totalContentHeight = dock.getBottomY();
        for (Float rh : rowHeights) totalContentHeight += rh + PANEL_SPACING;
        totalContentHeight += 20;

        maxGlobalScroll = Math.max(0f, totalContentHeight - height);
        globalScroll = Math.max(0f, Math.min(globalScroll, maxGlobalScroll));
    }

    private void animatePanelPositions() {
        for (int i = 0; i < panels.size(); i++) {
            if (draggingPanel == i) continue;
            var panel = panels.get(i);
            float slotX = getSlotX(i);
            float slotY = getSlotY(i);
            if (slotX != panel.getX() || slotY != panel.getY()) {
                panel.getPositionAnimation().updateTo(slotX, slotY);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        ScreenManager.focusedSetting = null;
        TooltipManager.clear();
    }

    private void renderPanels(int mouseX, int mouseY, float openProgress) {
        ((IGameRenderer) mc.gameRenderer).echo$flushGuiState();

        var draw = Echo.draw2D;
        var blurTexture = draw.getBlurResult();

        if (dock.isTab(Tab.FEATURES)) {
            for (var panel : panels) {
                panel.render(draw, mat, mouseX, mouseY, openProgress, blurTexture);
            }
        }

        if (dock.isTab(Tab.CONFIGS)) {
            configMenu.render(draw, mat, RenderUtil.getScaledWidth(), mouseX, mouseY, openProgress, blurTexture, dock.getBottomY());
        }

        if (dock.isTab(Tab.FRIENDS)) {
            friendsMenu.render(draw, mat, RenderUtil.getScaledWidth(), mouseX, mouseY, openProgress, blurTexture, dock.getBottomY());
        }

        dock.renderHudEditor(draw, mat, RenderUtil.getScaledWidth(), height, mouseX, mouseY, openProgress);
        dock.render(draw, mat, RenderUtil.getScaledWidth(), mouseX, mouseY, glassTheme(), openProgress, blurTexture);

        TooltipManager.renderAndClear(draw, mat, RenderUtil.getScaledWidth(), height, blurTexture);
    }

    private void handleDrag(float mouseX, float mouseY) {
        if (editingPanels) {
            draggingPanel = -1;
            return;
        }

        if (draggingPanel == -1) return;

        GlassPanel dragging = panels.get(draggingPanel);
        dragging.setX(mouseX + dragOffsetX);
        dragging.setY(mouseY + dragOffsetY);

        float cx = mouseX + dragOffsetX + PANEL_WIDTH / 2f;
        float cy = mouseY + dragOffsetY + dragging.getHeight() / 2f;

        trySwap(draggingPanel - 1, cx, cy, dragging);
        trySwap(draggingPanel + 1, cx, cy, dragging);
    }

    private void trySwap(int neighborIndex, float cx, float cy, GlassPanel dragging) {
        if (neighborIndex < 0 || neighborIndex >= panels.size()) return;

        float myDist = dist(cx, cy, slots[draggingPanel] + PANEL_WIDTH / 2f, slotsY[draggingPanel]);
        float neighborDist = dist(cx, cy, slots[neighborIndex] + PANEL_WIDTH / 2f, slotsY[neighborIndex]);
        if (neighborDist < myDist) {
            GlassPanel neighbor = panels.get(neighborIndex);
            neighbor.setTarget(getSlotX(draggingPanel), getSlotY(draggingPanel));
            neighbor.setIndex(draggingPanel);
            dragging.setIndex(neighborIndex);
            Collections.swap(panels, draggingPanel, neighborIndex);
            draggingPanel = neighborIndex;
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (dock.isTab(Tab.CONFIGS) && configMenu.keyPressed(keyEvent)) {
            return true;
        }
        if (dock.isTab(Tab.FRIENDS) && friendsMenu.keyPressed(keyEvent)) {
            return true;
        }

        for (var panel : panels) {
            if (panel.keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers()))
                return true;
        }

        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (binding != null) {
                binding.getFeature().setKey(-1);
                binding = null;
                return true;
            }
            if (editingPanels) {
                editingPanels = false;
                return true;
            }
            animation.start(1, 0);
            if (draggingPanel != -1) {
                panels.get(draggingPanel).setTarget(getSlotX(draggingPanel), getSlotY(draggingPanel));
                draggingPanel = -1;
            }
            return true;
        }
        if (binding != null) {
            binding.getFeature().setKey(keyEvent.key());
            binding = null;
            SoundUtil.playKeypress();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubled) {
        float mx = getMouseX((float) click.x());
        float my = getMouseY((float) click.y());

        if (binding != null) {
            binding.getFeature().setKey(0x80000000 | (click.button() & 0xFF));
            binding = null;
            SoundUtil.playClick();
            return true;
        }

        if (dock.mouseClicked(mx, my, click.button())) return true;
        if (dock.isTab(Tab.CONFIGS)) {
            if (configMenu.mouseClicked(mx, my, click.button())) {
                return true;
            }
            return super.mouseClicked(click, doubled);
        }
        if (dock.isTab(Tab.FRIENDS)) {
            if (friendsMenu.mouseClicked(mx, my, click.button())) {
                return true;
            }
            return super.mouseClicked(click, doubled);
        }
        if (!dock.isTab(Tab.FEATURES)) return super.mouseClicked(click, doubled);

        for (int i = panels.size() - 1; i >= 0; i--) {
            GlassPanel panel = panels.get(i);

            if (isInside(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight(), mx, my)) {
                if (panel.isEditButtonHovered(mx, my)) {
                    if (click.button() == 0) {
                        toggleEditing();
                        SoundUtil.playClick();
                    }
                    return true;
                }

                if (my <= panel.getY() + PANEL_HEADER_HEIGHT) {
                    if (click.button() == 0) {
                        if (editingPanels) {
                            return true;
                        }
                        dragOffsetX = panel.getX() - mx;
                        dragOffsetY = panel.getY() - my;
                        draggingPanel = i;
                        return true;
                    } else if (click.button() == 1) {
                        panel.expanded = !panel.expanded;
                        SoundUtil.playExpand(panel.expanded);
                        panel.updateHeight();
                        return true;
                    }
                } else {
                    return panel.onClicked(new MouseButtonEvent(mx, my, click.buttonInfo()));
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent click, double deltaX, double deltaY) {
        float mx = getMouseX((float) click.x());
        float my = getMouseY((float) click.y());

        if (dock.mouseDragged(mx, my, click.button(), width, height)) return true;
        if (dock.isTab(Tab.CONFIGS) && configMenu.mouseDragged(mx, my, click.button(), width, height)) {
            return true;
        }
        if (dock.isTab(Tab.FRIENDS) && friendsMenu.mouseDragged(mx, my, click.button(), width, height)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent click) {
        float mx = getMouseX((float) click.x());
        float my = getMouseY((float) click.y());

        if (dock.mouseReleased(click.button())) return true;
        if (dock.isTab(Tab.CONFIGS) && configMenu.mouseReleased(click.button())) {
            return true;
        }
        if (dock.isTab(Tab.FRIENDS) && friendsMenu.mouseReleased(click.button())) {
            return true;
        }
        if (dock.isTab(Tab.FEATURES)) {
            if (draggingPanel != -1) {
                panels.get(draggingPanel).setTarget(getSlotX(draggingPanel), getSlotY(draggingPanel));
                draggingPanel = -1;
            }
            for (GlassPanel panel : panels) panel.mouseReleased(mx, my, click.button());
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float mx = getMouseX((float) mouseX);
        float my = getMouseY((float) mouseY);

        if (dock.isTab(Tab.CONFIGS) && configMenu.mouseScrolled(mx, my, verticalAmount)) {
            return true;
        }
        if (dock.isTab(Tab.FRIENDS) && friendsMenu.mouseScrolled(mx, my, verticalAmount)) {
            return true;
        }

        if (dock.isTab(Tab.FEATURES)) {
            for (GlassPanel panel : panels) {
                if (!panel.expanded) continue;
                if (!isInside(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight(), mx, my)) continue;
                if (panel.mouseScrolled(mx, my, verticalAmount)) return true;
            }
            if (verticalAmount != 0 && maxGlobalScroll > 0f) {
                globalScroll = Math.max(0f, Math.min(maxGlobalScroll,
                        globalScroll - (float) verticalAmount * GLOBAL_SCROLL_SPEED));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent input) {
        if (dock.isTab(Tab.CONFIGS) && configMenu.charTyped(input)) {
            return true;
        }
        if (dock.isTab(Tab.FRIENDS) && friendsMenu.charTyped(input)) {
            return true;
        }
        if (ScreenManager.focusedSetting != null) {
            for (GlassPanel panel : panels)
                panel.charTyped((char) input.codepoint(), CharacterEventCompat.modifiers(input));
            return true;
        }
        return super.charTyped(input);
    }

    private static ClickGuiDock.Theme glassTheme() {
        return new ClickGuiDock.Theme(
                CONTROL_TOGGLE_ON, SURFACE_PANEL,
                SURFACE_SETTING, SURFACE_SETTING_HOVER,
                TEXT_ON_SURFACE_SECONDARY, TEXT_ON_ACCENT
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isEditingPanel(GlassPanel panel) {
        return editingPanels;
    }

    private void toggleEditing() {
        editingPanels = !editingPanels;
    }
}