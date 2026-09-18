package hack.echo.client.screens.clickgui.glassrewrite;

import hack.echo.client.Echo;
import hack.echo.client.config.FeatureConfig.ProfileEntry;
import hack.echo.client.features.impl.misc.ClickGUI;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.ChatUtils;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassConfigMenu {

    private static final float MENU_WIDTH = 344f;
    private static final float MENU_HEIGHT = 232f;
    private static final float MENU_PADDING = 10f;
    private static final float MENU_RADIUS = 10f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float HEADER_BUTTON_SIZE = 18f;

    private static final float CARD_HEIGHT = 72f;
    private static final float CARD_RADIUS = 8f;
    private static final float CARD_SPACING = 6f;
    private static final float CARD_BUTTON_HEIGHT = 14f;
    private static final float CARD_BUTTON_GAP = 6f;
    private static final float CARD_BUTTON_RADIUS = 4f;

    private static final float SCROLLBAR_WIDTH = 4f;
    private static final float SCROLLBAR_RADIUS = 2f;
    private static final int CARDS_PER_ROW = 3;

    private static final float POPUP_WIDTH = 230f;
    private static final float POPUP_HEIGHT = 76f;
    private static final float POPUP_RADIUS = 12f;
    private static final float POPUP_ICON_SIZE = 28f;
    private static final float POPUP_INPUT_HEIGHT = 16f;
    private static final float POPUP_CONFIRM_SIZE = 18f;
    private static final float POPUP_DRAG_HEIGHT = 32f;

    private final GlassScreen screen;

    private float x;
    private float y;
    private float scroll;
    private float maxScroll;
    private final Map<String, CrossTexture> iconTextures = new HashMap<>();
    private final List<ButtonHit> buttonHits = new ArrayList<>();
    private CrossTexture popupServerIcon;
    private byte[] popupServerIconBytes;
    private boolean positionInitialized;
    private boolean draggingMenu;
    private float menuDragOffsetX;
    private float menuDragOffsetY;

    private boolean popupOpen;
    private boolean popupInitialized;
    private float popupX;
    private float popupY;
    private boolean draggingPopup;
    private float popupDragOffsetX;
    private float popupDragOffsetY;
    private boolean popupInputFocused;
    private String popupName = "";
    private int popupCursorPos = 0;

    public GlassConfigMenu(GlassScreen screen) {
        this.screen = screen;
    }

    public void render(Draw2D draw, Matrix4f mat, float screenWidth, int mouseX, int mouseY,
                       float openProgress, CrossTexture blurTexture, float topY) {
        ensureMenuPosition(screenWidth, topY);
        ensurePopupPosition();

        int baseMouseX = mouseX;
        int baseMouseY = mouseY;
        if (popupOpen && isInside(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            baseMouseX = Integer.MIN_VALUE;
            baseMouseY = Integer.MIN_VALUE;
        }

        renderMenuBackground(draw, mat, blurTexture, openProgress);
        renderHeader(draw, mat, baseMouseX, baseMouseY, openProgress);
        renderCards(draw, mat, baseMouseX, baseMouseY, openProgress);
        renderScrollbar(draw, mat, openProgress);

        if (popupOpen) {
            renderPopup(draw, mat, blurTexture, mouseX, mouseY, openProgress);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (popupOpen && isInside(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, (float) mouseX, (float) mouseY)) {
            return true;
        }

        if (!isInside(x, y, MENU_WIDTH, MENU_HEIGHT, (float) mouseX, (float) mouseY)) {
            return false;
        }

        if (maxScroll <= 0f || amount == 0.0) {
            return true;
        }

        float scrollSpeed = getScrollSpeed();
        float nextScroll = (float) Math.clamp(scroll - (float) amount * scrollSpeed, 0f, maxScroll);
        scroll = nextScroll;
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (popupOpen && handlePopupClick((float) mouseX, (float) mouseY)) {
            return true;
        }

        if (handlePlusClick((float) mouseX, (float) mouseY)) {
            return true;
        }

        if (isInside(x, y, MENU_WIDTH, HEADER_HEIGHT, (float) mouseX, (float) mouseY)) {
            draggingMenu = true;
            menuDragOffsetX = x - (float) mouseX;
            menuDragOffsetY = y - (float) mouseY;
            popupInputFocused = false;
            return true;
        }

        for (ButtonHit hit : buttonHits) {
            if (!isInside(hit.x, hit.y, hit.width, hit.height, (float) mouseX, (float) mouseY)) {
                continue;
            }

            return handleButton(hit);
        }

        return isInside(x, y, MENU_WIDTH, MENU_HEIGHT, (float) mouseX, (float) mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
        if (button != 0) {
            return false;
        }

        if (draggingPopup) {
            popupX = clampX((float) mouseX + popupDragOffsetX, POPUP_WIDTH, screenWidth);
            popupY = clampY((float) mouseY + popupDragOffsetY, POPUP_HEIGHT, screenHeight);
            return true;
        }

        if (draggingMenu) {
            x = clampX((float) mouseX + menuDragOffsetX, MENU_WIDTH, screenWidth);
            y = clampY((float) mouseY + menuDragOffsetY, MENU_HEIGHT, screenHeight);
            return true;
        }

        return false;
    }

    public boolean mouseReleased(int button) {
        if (button != 0) {
            return false;
        }

        boolean wasDragging = draggingMenu || draggingPopup;
        draggingMenu = false;
        draggingPopup = false;
        return wasDragging;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!popupOpen || !popupInputFocused) {
            return false;
        }

        char chr = (char) event.codepoint();
        if (Character.isISOControl(chr)) {
            return false;
        }

        int cursor = Math.clamp(popupCursorPos, 0, popupName.length());
        popupName = popupName.substring(0, cursor) + chr + popupName.substring(cursor);
        popupCursorPos = cursor + 1;
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!popupOpen) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            popupOpen = false;
            popupInputFocused = false;
            return true;
        }

        if (!popupInputFocused) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                int newPos = wordStart(popupName, popupCursorPos);
                popupName = popupName.substring(0, newPos) + popupName.substring(popupCursorPos);
                popupCursorPos = newPos;
            } else if (popupCursorPos > 0 && !popupName.isEmpty()) {
                popupName = popupName.substring(0, popupCursorPos - 1) + popupName.substring(popupCursorPos);
                popupCursorPos--;
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_DELETE) {
            if (popupCursorPos < popupName.length()) {
                popupName = popupName.substring(0, popupCursorPos) + popupName.substring(popupCursorPos + 1);
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            popupCursorPos = Math.max(0, popupCursorPos - 1);
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            popupCursorPos = Math.min(popupName.length(), popupCursorPos + 1);
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_HOME) {
            popupCursorPos = 0;
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_END) {
            popupCursorPos = popupName.length();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            confirmPopup();
            return true;
        }

        return false;
    }

    private void renderMenuBackground(Draw2D draw, Matrix4f mat, CrossTexture blurTexture, float alpha) {
        if (blurTexture != null) {
            draw.screenImage(mat, blurTexture, x, y, MENU_WIDTH, MENU_HEIGHT, MENU_RADIUS, alpha);
        }

        draw.rect(mat, x, y, MENU_WIDTH, MENU_HEIGHT, MENU_RADIUS, wa(SURFACE_PANEL.getRGB(), alpha));
    }

    private void renderHeader(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float alpha) {
        if (Fonts.interSemiBold != null) {
            draw.text(Fonts.interSemiBold, mat, "Configs", x + MENU_PADDING, y + 7f, 10f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        float buttonX = x + MENU_WIDTH - MENU_PADDING - HEADER_BUTTON_SIZE;
        float buttonY = y + 3f;
        boolean hovered = isInside(buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY);
        int buttonColor = hovered
                ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha)
                : wa(SURFACE_SETTING.getRGB(), alpha);
        draw.rect(mat, buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, 5f,
                buttonColor);

        if (Fonts.lucide != null) {
            float iconSize = 8f;
            String icon = TextLuicideConstants.plus;
            float iconWidth = Fonts.lucide.getWidth(icon, iconSize);
            float iconX = buttonX + (HEADER_BUTTON_SIZE - iconWidth) / 2f;
            float iconY = buttonY + HEADER_BUTTON_SIZE / 2f - iconSize / 2f;
            draw.text(Fonts.lucide, mat, icon, iconX, iconY, iconSize,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

//        draw.rect(mat, x, y + HEADER_HEIGHT, MENU_WIDTH, 1f, 0f, wa(BORDER_PANEL.getRGB(), alpha));
    }

    private void renderPopup(Draw2D draw, Matrix4f mat, CrossTexture blurTexture, int mouseX, int mouseY, float alpha) {
        if (blurTexture != null) {
            draw.screenImage(mat, blurTexture, popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS, alpha);
        }

        draw.rect(mat, popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS,
                wa(SURFACE_PANEL.getRGB(), alpha));

        float iconX = popupX + 10f;
        float iconY = popupY + 10f;
        draw.rect(mat, iconX, iconY, POPUP_ICON_SIZE, POPUP_ICON_SIZE, 8f,
                wa(SURFACE_SETTING_HOVER.getRGB(), alpha));

        CrossTexture popupIcon = getPopupServerIcon();
        if (popupIcon != null) {
            draw.image(mat, popupIcon, iconX, iconY, POPUP_ICON_SIZE, POPUP_ICON_SIZE, 8f, alpha);
        } else if (Fonts.lucide != null) {
            float iconSize = 10f;
            String icon = TextLuicideConstants.files;
            float iconWidth = Fonts.lucide.getWidth(icon, iconSize);
            float drawX = iconX + (POPUP_ICON_SIZE - iconWidth) / 2f;
            float drawY = iconY + POPUP_ICON_SIZE / 2f - iconSize / 2f;
            draw.text(Fonts.lucide, mat, icon, drawX, drawY, iconSize,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        if (Fonts.interSemiBold != null) {
            String preview = popupName.isBlank() ? "New Config" : popupName;
            draw.text(Fonts.interSemiBold, mat, clip(preview, 130f, 8f), popupX + 48f, popupY + 18f, 8f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        float inputX = popupX + 10f;
        float inputY = popupY + POPUP_HEIGHT - POPUP_INPUT_HEIGHT - 10f;
        float inputWidth = POPUP_WIDTH - 10f - 10f - POPUP_CONFIRM_SIZE - 8f;
        boolean inputHovered = isInside(inputX, inputY, inputWidth, POPUP_INPUT_HEIGHT, mouseX, mouseY);
        int inputColor = popupInputFocused
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 220), alpha)
                : inputHovered
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 190), alpha)
                : argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 160), alpha);
        draw.rect(mat, inputX, inputY, inputWidth, POPUP_INPUT_HEIGHT, 6f, inputColor);

        if (Fonts.interSemiBold != null) {
            String inputText = popupName.isBlank() ? "Config name" : popupName;
            int inputTextColor = popupName.isBlank()
                    ? argbMul(TEXT_ON_SURFACE_MUTED, alpha)
                    : argbMul(TEXT_ON_SURFACE_PRIMARY, alpha);
            draw.text(Fonts.interSemiBold, mat, clip(inputText, inputWidth - 12f, 6f), inputX + 6f, inputY + 4f, 6f,
                    inputTextColor);

            if (popupInputFocused && blinkOn()) {
                int cursor = Math.clamp(popupCursorPos, 0, popupName.length());
                float cursorX = inputX + 6f + Fonts.interSemiBold.getWidth(popupName.substring(0, cursor), 6f);
                float maxCursorX = inputX + inputWidth - 6f;
                cursorX = Math.min(cursorX, maxCursorX);
                draw.rect(mat, cursorX, inputY + 3f, 1f, POPUP_INPUT_HEIGHT - 6f, 0f,
                        argbMul(SURFACE_CONTROL_ELEVATED, alpha));
            }
        }

        float confirmX = inputX + inputWidth + 8f;
        float confirmY = inputY - 1f;
        boolean confirmHovered = isInside(confirmX, confirmY, POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, mouseX, mouseY);
        int confirmColor = confirmHovered
                ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha)
                : wa(SURFACE_SETTING.getRGB(), alpha);
        draw.rect(mat, confirmX, confirmY, POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, 6f, confirmColor);

        if (Fonts.lucide != null) {
            float iconSize = 9f;
            String icon = TextLuicideConstants.check;
            float iconWidth = Fonts.lucide.getWidth(icon, iconSize);
            float drawX = confirmX + (POPUP_CONFIRM_SIZE - iconWidth) / 2f;
            float drawY = confirmY + POPUP_CONFIRM_SIZE / 2f - iconSize / 2f;
            draw.text(Fonts.lucide, mat, icon, drawX, drawY, iconSize,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }
    }

    private void renderCards(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float alpha) {
        buttonHits.clear();

        List<ProfileEntry> profiles = getProfiles();
        if (profiles.isEmpty()) {
            renderEmptyState(draw, mat, alpha);
            return;
        }

        float contentX = x + MENU_PADDING;
        float contentY = y + HEADER_HEIGHT + MENU_PADDING;
        float contentWidth = MENU_WIDTH - MENU_PADDING * 2f - SCROLLBAR_WIDTH - 6f;
        float contentHeight = MENU_HEIGHT - HEADER_HEIGHT - MENU_PADDING * 2f;
        float cardWidth = (contentWidth - CARD_SPACING * (CARDS_PER_ROW - 1)) / CARDS_PER_ROW;
        float totalHeight = getContentHeight(profiles.size());

        maxScroll = Math.max(0f, totalHeight - contentHeight);
        scroll = Math.clamp(scroll, 0f, maxScroll);

        draw.pushScissor(contentX, contentY, contentWidth, contentHeight);

        for (int i = 0; i < profiles.size(); i++) {
            int column = i % CARDS_PER_ROW;
            int row = i / CARDS_PER_ROW;

            float cardX = contentX + column * (cardWidth + CARD_SPACING);
            float cardY = contentY + row * (CARD_HEIGHT + CARD_SPACING) - scroll;

            if (cardY + CARD_HEIGHT < contentY || cardY > contentY + contentHeight) {
                continue;
            }

            ProfileEntry profile = profiles.get(i);
            boolean hovered = isInside(cardX, cardY, cardWidth, CARD_HEIGHT, mouseX, mouseY);
            renderCard(draw, mat, cardX, cardY, cardWidth, alpha, hovered, profile, mouseX, mouseY);
        }

        draw.popScissor();
    }

    private void renderCard(Draw2D draw, Matrix4f mat, float x, float y, float width,
                            float alpha, boolean hovered, ProfileEntry profile, int mouseX, int mouseY) {
        int cardColor = hovered ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha) : wa(SURFACE_SETTING.getRGB(), alpha);
        draw.rect(mat, x, y, width, CARD_HEIGHT, CARD_RADIUS, cardColor);

        float previewSize = 22f;
        float previewX = x + 8f;
        float previewY = y + 8f;
        draw.rect(mat, previewX, previewY, previewSize, previewSize, 5f, wa(SURFACE_SETTING_HOVER.getRGB(), alpha));

        CrossTexture iconTexture = getIconTexture(profile);
        if (iconTexture != null) {
            draw.image(mat, iconTexture, previewX, previewY, previewSize, previewSize, 5f, alpha);
        } else if (Fonts.lucide != null) {
            float iconSize = 8f;
            String icon = TextLuicideConstants.earth;
            float iconWidth = Fonts.lucide.getWidth(icon, iconSize);
            float iconX = previewX + (previewSize - iconWidth) / 2f;
            float iconY = previewY + previewSize / 2f - iconSize / 2f;
            draw.text(Fonts.lucide, mat, icon, iconX, iconY, iconSize, argbMul(TEXT_ON_ACCENT, alpha));
        }

        float textX = previewX + previewSize + 6f;
        float titleWidth = width - (textX - x) - 8f;

        if (Fonts.interSemiBold != null) {
            draw.text(Fonts.interSemiBold, mat, clip(profile.name(), titleWidth, 8f), textX, y + 9f, 8f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
            draw.text(Fonts.interSemiBold, mat, clip(getServerLabel(profile), titleWidth, 6f), x + 8f, y + 31f, 6f,
                    argbMul(TEXT_ON_SURFACE_SECONDARY, alpha));
            draw.text(Fonts.interSemiBold, mat, "local", x + 8f, y + 40f, 6f,
                    argbMul(TEXT_ON_SURFACE_MUTED, alpha));
        }

        float buttonsX = x + 8f;
        float buttonsY = y + CARD_HEIGHT - CARD_BUTTON_HEIGHT - 8f;
        float buttonWidth = (width - 16f - CARD_BUTTON_GAP) / 2f;
        boolean saveHovered = isInside(buttonsX, buttonsY, buttonWidth, CARD_BUTTON_HEIGHT, mouseX, mouseY);
        boolean loadHovered = isInside(buttonsX + buttonWidth + CARD_BUTTON_GAP, buttonsY,
                buttonWidth, CARD_BUTTON_HEIGHT, mouseX, mouseY);

        renderButton(draw, mat, buttonsX, buttonsY, buttonWidth, alpha, "Save", saveHovered);
        renderButton(draw, mat, buttonsX + buttonWidth + CARD_BUTTON_GAP, buttonsY, buttonWidth, alpha, "Load",
                loadHovered);
        buttonHits.add(new ButtonHit(profile.name(), ButtonType.SAVE, buttonsX, buttonsY, buttonWidth, CARD_BUTTON_HEIGHT));
        buttonHits.add(new ButtonHit(profile.name(), ButtonType.LOAD,
                buttonsX + buttonWidth + CARD_BUTTON_GAP, buttonsY, buttonWidth, CARD_BUTTON_HEIGHT));
    }

    private void renderButton(Draw2D draw, Matrix4f mat, float x, float y, float width, float alpha,
                              String text, boolean hovered) {
        int background = hovered
                ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha)
                : wa(TAB_INACTIVE_HOVER.getRGB(), alpha);
        draw.rect(mat, x, y, width, CARD_BUTTON_HEIGHT, CARD_BUTTON_RADIUS, background);

        if (Fonts.interSemiBold == null) {
            return;
        }

        float textWidth = Fonts.interSemiBold.getWidth(text, 6f);
        float textX = x + (width - textWidth) / 2f;
        float textY = y + CARD_BUTTON_HEIGHT / 2f - 3f;
        draw.text(Fonts.interSemiBold, mat, text, textX, textY, 6f, argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
    }

    private void renderScrollbar(Draw2D draw, Matrix4f mat, float alpha) {
        if (maxScroll <= 0f) {
            return;
        }

        float trackX = x + MENU_WIDTH - MENU_PADDING - SCROLLBAR_WIDTH;
        float trackY = y + HEADER_HEIGHT + MENU_PADDING;
        float trackHeight = MENU_HEIGHT - HEADER_HEIGHT - MENU_PADDING * 2f;
        draw.rect(mat, trackX, trackY, SCROLLBAR_WIDTH, trackHeight, SCROLLBAR_RADIUS,
                wa(SURFACE_SETTING.getRGB(), alpha));

        float visibleRatio = trackHeight / (trackHeight + maxScroll);
        float thumbHeight = Math.max(18f, trackHeight * visibleRatio);
        float thumbTravel = trackHeight - thumbHeight;
        float thumbOffset = maxScroll <= 0f ? 0f : thumbTravel * (scroll / maxScroll);

        draw.rect(mat, trackX, trackY + thumbOffset, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_RADIUS,
                wa(SURFACE_CONTROL_ELEVATED.getRGB(), alpha));
    }

    private void renderEmptyState(Draw2D draw, Matrix4f mat, float alpha) {
        maxScroll = 0f;
        scroll = 0f;

        float textX = x + MENU_PADDING;
        float textY = y + HEADER_HEIGHT + MENU_PADDING + 10f;

        if (Fonts.interSemiBold == null) {
            return;
        }

        draw.text(Fonts.interSemiBold, mat, "No saved configs yet", textX, textY, 8f,
                argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        draw.text(Fonts.interSemiBold, mat, "Use the top-right button later.", textX, textY + 12f, 6f,
                argbMul(TEXT_ON_SURFACE_SECONDARY, alpha));
    }

    private boolean handleButton(ButtonHit hit) {
        if (Echo.featureConfig == null) {
            return true;
        }

        boolean ok = switch (hit.type) {
            case SAVE -> Echo.featureConfig.saveProfile(hit.profileName);
            case LOAD -> Echo.featureConfig.loadProfile(hit.profileName);
        };

        SoundUtil.playClick();
        if (ok) {
            ChatUtils.chat(Concat.of(hit.type == ButtonType.SAVE ? "saved " : "loaded ", hit.profileName));
        } else {
            ChatUtils.chat(Concat.of("failed to ", hit.type == ButtonType.SAVE ? "save " : "load ", hit.profileName));
        }
        return true;
    }

    private boolean handlePlusClick(float mouseX, float mouseY) {
        if (!isInside(getPlusButtonX(), getPlusButtonY(), HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY)) {
            return false;
        }

        popupOpen = true;
        popupInputFocused = true;
        popupCursorPos = popupName.length();
        ensurePopupPosition();
        SoundUtil.playClick();
        return true;
    }

    private boolean handlePopupClick(float mouseX, float mouseY) {
        if (!isInside(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            popupInputFocused = false;
            return false;
        }

        float confirmX = getPopupConfirmX();
        float confirmY = getPopupConfirmY();
        if (isInside(confirmX, confirmY, POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, mouseX, mouseY)) {
            confirmPopup();
            return true;
        }

        float inputX = getPopupInputX();
        float inputY = getPopupInputY();
        float inputWidth = getPopupInputWidth();
        if (isInside(inputX, inputY, inputWidth, POPUP_INPUT_HEIGHT, mouseX, mouseY)) {
            popupInputFocused = true;
            popupCursorPos = findPopupCursor(mouseX, inputX + 6f);
            return true;
        }

        if (isInside(popupX, popupY, POPUP_WIDTH, POPUP_DRAG_HEIGHT, mouseX, mouseY)) {
            draggingPopup = true;
            popupDragOffsetX = popupX - mouseX;
            popupDragOffsetY = popupY - mouseY;
            popupInputFocused = false;
            return true;
        }

        popupInputFocused = false;
        return true;
    }

    private void confirmPopup() {
        String name = popupName.trim();
        if (name.isEmpty()) {
            ChatUtils.chat("enter a config name");
            SoundUtil.playClick();
            return;
        }

        if (Echo.featureConfig == null) {
            SoundUtil.playClick();
            return;
        }

        boolean ok = Echo.featureConfig.saveProfile(name);
        SoundUtil.playClick();

        if (!ok) {
            ChatUtils.chat(Concat.of("failed to save ", name));
            return;
        }

        popupOpen = false;
        popupInputFocused = false;
        popupName = "";
        popupCursorPos = 0;
        ChatUtils.chat(Concat.of("saved ", name));
    }

    private float getScrollSpeed() {
        ClickGUI gui = screen.gui;
        if (gui == null) {
            return SCROLL_MULTIPLIER;
        }

        return gui.getScrollSpeed();
    }

    private void ensureMenuPosition(float screenWidth, float topY) {
        if (positionInitialized) {
            return;
        }

        x = (screenWidth - MENU_WIDTH) / 2f;
        y = topY + 8f;
        positionInitialized = true;
    }

    private void ensurePopupPosition() {
        if (popupInitialized) {
            return;
        }

        popupX = x + MENU_WIDTH + 12f;
        popupY = y + 18f;
        popupInitialized = true;
    }

    private float clampX(float value, float width, int screenWidth) {
        float maxX = Math.max(0f, screenWidth - width);
        return Math.clamp(value, 0f, maxX);
    }

    private float clampY(float value, float height, int screenHeight) {
        float maxY = Math.max(0f, screenHeight - height);
        return Math.clamp(value, 0f, maxY);
    }

    private float getContentHeight(int profileCount) {
        int rows = (int) Math.ceil(profileCount / (float) CARDS_PER_ROW);
        float cardsHeight = rows * CARD_HEIGHT;
        float gapsHeight = Math.max(0, rows - 1) * CARD_SPACING;
        return cardsHeight + gapsHeight;
    }

    private float getPlusButtonX() {
        return x + MENU_WIDTH - MENU_PADDING - HEADER_BUTTON_SIZE;
    }

    private float getPlusButtonY() {
        return y + 3f;
    }

    private float getPopupInputX() {
        return popupX + 10f;
    }

    private float getPopupInputY() {
        return popupY + POPUP_HEIGHT - POPUP_INPUT_HEIGHT - 10f;
    }

    private float getPopupInputWidth() {
        return POPUP_WIDTH - 10f - 10f - POPUP_CONFIRM_SIZE - 8f;
    }

    private float getPopupConfirmX() {
        return getPopupInputX() + getPopupInputWidth() + 8f;
    }

    private float getPopupConfirmY() {
        return getPopupInputY() - 1f;
    }

    private String getServerLabel(ProfileEntry profile) {
        if (profile.serverIp() != null && !profile.serverIp().isBlank()) {
            return profile.serverIp();
        }

        if (profile.serverName() != null && !profile.serverName().isBlank()) {
            return profile.serverName();
        }

        return "singleplayer";
    }

    private CrossTexture getIconTexture(ProfileEntry profile) {
        byte[] iconBytes = profile.serverIconBytes();
        if (iconBytes == null || iconBytes.length == 0) {
            return null;
        }

        return iconTextures.computeIfAbsent(profile.name(), key -> CrossTexture.from(iconBytes));
    }

    private CrossTexture getPopupServerIcon() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null) {
            return null;
        }

        byte[] iconBytes = server.getIconBytes();
        if (iconBytes == null || iconBytes.length == 0) {
            return null;
        }

        if (popupServerIcon != null && java.util.Arrays.equals(popupServerIconBytes, iconBytes)) {
            return popupServerIcon;
        }

        popupServerIconBytes = iconBytes.clone();
        popupServerIcon = CrossTexture.from(popupServerIconBytes);
        return popupServerIcon;
    }

    private boolean blinkOn() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    private int findPopupCursor(float mouseX, float textStartX) {
        if (Fonts.interSemiBold == null) {
            return popupName.length();
        }

        float relativeX = mouseX - textStartX;
        if (relativeX <= 0f) {
            return 0;
        }

        for (int i = 0; i <= popupName.length(); i++) {
            float width = Fonts.interSemiBold.getWidth(popupName.substring(0, i), 6f);
            if (relativeX <= width) {
                return i;
            }
        }

        return popupName.length();
    }

    private int wordStart(String text, int pos) {
        int i = Math.clamp(pos, 0, text.length());
        while (i > 0 && text.charAt(i - 1) == ' ') {
            i--;
        }
        while (i > 0 && text.charAt(i - 1) != ' ') {
            i--;
        }
        return i;
    }

    private CharSequence clip(String text, float maxWidth, float size) {
        if (Fonts.interSemiBold == null) {
            return text;
        }

        if (Fonts.interSemiBold.getWidth(text, size) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        for (int end = text.length() - 1; end > 0; end--) {
            String clipped = text.substring(0, end) + ellipsis;
            if (Fonts.interSemiBold.getWidth(clipped, size) <= maxWidth) {
                return clipped;
            }
        }

        return ellipsis;
    }

    private List<ProfileEntry> getProfiles() {
        if (Echo.featureConfig == null) {
            return List.of();
        }

        List<ProfileEntry> profiles = new ArrayList<>(Echo.featureConfig.listProfileEntries());
        profiles.removeIf(profile -> "_autosave".equals(profile.name()));
        profiles.sort(Comparator.comparing(ProfileEntry::name));
        return profiles;
    }

    private enum ButtonType {
        SAVE,
        LOAD
    }

    private static class ButtonHit {
        private final String profileName;
        private final ButtonType type;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private ButtonHit(String profileName, ButtonType type, float x, float y, float width, float height) {
            this.profileName = profileName;
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
