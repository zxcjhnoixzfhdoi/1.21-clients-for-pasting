package hack.echo.client.screens.clickgui.glassrewrite;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.misc.ClickGUI;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.player.PlayerHeadTextures;
import hack.echo.client.utils.player.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassFriendsMenu {

    private static final float MENU_WIDTH = 238f;
    private static final float MENU_HEIGHT = 250f;
    private static final float MENU_PADDING = 10f;
    private static final float MENU_RADIUS = 14f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float HEADER_BUTTON_SIZE = 18f;
    private static final float SEARCH_HEIGHT = 16f;

    private static final float ROW_HEIGHT = 18f;
    private static final float ROW_RADIUS = 5f;
    private static final float ROW_SPACING = 4f;
    private static final float ROW_HEAD_SIZE = 12f;
    private static final float ROW_BUTTON_SIZE = 14f;

    private static final float SCROLLBAR_WIDTH = 4f;
    private static final float SCROLLBAR_RADIUS = 2f;

    private static final float POPUP_WIDTH = 202f;
    private static final float POPUP_HEIGHT = 74f;
    private static final float POPUP_RADIUS = 12f;
    private static final float POPUP_DRAG_HEIGHT = 32f;
    private static final float POPUP_ICON_SIZE = 28f;
    private static final float POPUP_INPUT_HEIGHT = 16f;
    private static final float POPUP_CONFIRM_SIZE = 18f;

    private final GlassScreen screen;
    private final List<ActionHit> actionHits = new ArrayList<>();

    private float x;
    private float y;
    private float scroll;
    private float maxScroll;
    private boolean positionInitialized;
    private boolean draggingMenu;
    private float menuDragOffsetX;
    private float menuDragOffsetY;

    private boolean searchFocused;
    private String searchText = "";
    private int searchCursorPos = 0;

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

    public GlassFriendsMenu(GlassScreen screen) {
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
        renderRows(draw, mat, baseMouseX, baseMouseY, openProgress);
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

        if (amount == 0.0) {
            return true;
        }

        if (maxScroll <= 0f) {
            return true;
        }

        scroll = (float) Math.clamp(scroll - (float) amount * getScrollSpeed(), 0f, maxScroll);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        float mx = (float) mouseX;
        float my = (float) mouseY;

        if (popupOpen && handlePopupClick(mx, my)) {
            return true;
        }

        if (handlePlusClick(mx, my)) {
            return true;
        }

        if (handleSearchClick(mx, my)) {
            return true;
        }

        for (ActionHit hit : actionHits) {
            if (!isInside(hit.x, hit.y, hit.width, hit.height, mx, my)) {
                continue;
            }

            toggleFriend(hit.name);
            return true;
        }

        if (isInside(x, y, MENU_WIDTH, HEADER_HEIGHT, mx, my)) {
            draggingMenu = true;
            menuDragOffsetX = x - mx;
            menuDragOffsetY = y - my;
            searchFocused = false;
            popupInputFocused = false;
            return true;
        }

        if (isInside(x, y, MENU_WIDTH, MENU_HEIGHT, mx, my)) {
            searchFocused = false;
            return true;
        }

        return false;
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
        char chr = (char) event.codepoint();
        if (Character.isISOControl(chr)) {
            return false;
        }

        if (popupOpen && popupInputFocused) {
            popupName = insertChar(popupName, popupCursorPos, chr);
            popupCursorPos = Math.min(popupName.length(), popupCursorPos + 1);
            return true;
        }

        if (!searchFocused) {
            return false;
        }

        searchText = insertChar(searchText, searchCursorPos, chr);
        searchCursorPos = Math.min(searchText.length(), searchCursorPos + 1);
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        if (popupOpen) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                popupOpen = false;
                popupInputFocused = false;
                return true;
            }

            if (popupInputFocused && handleTextFieldKey(event, true)) {
                return true;
            }
        }

        if (!searchFocused) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            searchFocused = false;
            return true;
        }

        return handleTextFieldKey(event, false);
    }

    private void renderMenuBackground(Draw2D draw, Matrix4f mat, CrossTexture blurTexture, float alpha) {
        if (blurTexture != null) {
            draw.screenImage(mat, blurTexture, x, y, MENU_WIDTH, MENU_HEIGHT, MENU_RADIUS, alpha);
        }

        draw.rect(mat, x, y, MENU_WIDTH, MENU_HEIGHT, MENU_RADIUS, wa(SURFACE_PANEL.getRGB(), alpha));
    }

    private void renderHeader(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float alpha) {
        if (Fonts.interSemiBold != null) {
            draw.text(Fonts.interSemiBold, mat, "Friends", x + MENU_PADDING, y + 7f, 10f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        float plusX = getPlusButtonX();
        float plusY = getPlusButtonY();
        boolean plusHovered = isInside(plusX, plusY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY);
        draw.rect(mat, plusX, plusY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, 5f,
                plusHovered ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha) : wa(SURFACE_SETTING.getRGB(), alpha));
        drawIcon(draw, mat, TextLuicideConstants.plus, plusX, plusY, HEADER_BUTTON_SIZE, 8f, alpha);

        float searchX = getSearchX();
        float searchY = getSearchY();
        float searchWidth = getSearchWidth();
        boolean searchHovered = isInside(searchX, searchY, searchWidth, SEARCH_HEIGHT, mouseX, mouseY);
        int searchColor = searchFocused
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 220), alpha)
                : searchHovered
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 190), alpha)
                : argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 160), alpha);
        draw.rect(mat, searchX, searchY, searchWidth, SEARCH_HEIGHT, 6f, searchColor);

        if (Fonts.interSemiBold != null) {
            String visible = searchText.isBlank() ? "Search" : searchText;
            int textColor = searchText.isBlank()
                    ? argbMul(TEXT_ON_SURFACE_MUTED, alpha)
                    : argbMul(TEXT_ON_SURFACE_PRIMARY, alpha);
            draw.text(Fonts.interSemiBold, mat, clip(visible, searchWidth - 12f, 6f), searchX + 6f, searchY + 4f, 6f, textColor);

            if (searchFocused && blinkOn()) {
                float cursorX = getCursorX(searchText, searchCursorPos, searchX + 6f, searchWidth - 12f, 6f);
                draw.rect(mat, cursorX, searchY + 3f, 1f, SEARCH_HEIGHT - 6f, 0f,
                        argbMul(SURFACE_CONTROL_ELEVATED, alpha));
            }
        }
    }

    private void renderRows(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float alpha) {
        actionHits.clear();

        List<FriendRow> rows = getRows();
        float contentX = x + MENU_PADDING;
        float contentY = y + HEADER_HEIGHT + MENU_PADDING;
        float contentWidth = MENU_WIDTH - MENU_PADDING * 2f - SCROLLBAR_WIDTH - 6f;
        float contentHeight = MENU_HEIGHT - HEADER_HEIGHT - MENU_PADDING * 2f;
        float totalHeight = rows.isEmpty() ? 0f : rows.size() * ROW_HEIGHT + Math.max(0, rows.size() - 1) * ROW_SPACING;

        maxScroll = Math.max(0f, totalHeight - contentHeight);
        scroll = Math.clamp(scroll, 0f, maxScroll);

        draw.pushScissor(contentX, contentY, contentWidth, contentHeight);

        if (rows.isEmpty()) {
            renderEmptyState(draw, mat, contentX, contentY, alpha);
            draw.popScissor();
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            FriendRow row = rows.get(i);
            float rowY = contentY + i * (ROW_HEIGHT + ROW_SPACING) - scroll;

            if (rowY + ROW_HEIGHT < contentY || rowY > contentY + contentHeight) {
                continue;
            }

            renderRow(draw, mat, contentX, rowY, contentWidth, row, mouseX, mouseY, alpha);
        }

        draw.popScissor();
    }

    private void renderRow(Draw2D draw, Matrix4f mat, float rowX, float rowY, float rowWidth,
                           FriendRow row, int mouseX, int mouseY, float alpha) {
        boolean hovered = isInside(rowX, rowY, rowWidth, ROW_HEIGHT, mouseX, mouseY);
        int rowColor = hovered ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha) : wa(SURFACE_SETTING.getRGB(), alpha);
        draw.rect(mat, rowX, rowY, rowWidth, ROW_HEIGHT, ROW_RADIUS, rowColor);

        float headX = rowX + 4f;
        float headY = rowY + (ROW_HEIGHT - ROW_HEAD_SIZE) / 2f;
        draw.rect(mat, headX, headY, ROW_HEAD_SIZE, ROW_HEAD_SIZE, 4f, wa(SURFACE_SETTING_HOVER.getRGB(), alpha));

        CrossTexture head = row.info == null ? null : PlayerHeadTextures.getHead(row.info);
        if (head != null) {
            draw.image(mat, head, headX, headY, ROW_HEAD_SIZE, ROW_HEAD_SIZE, 4f, alpha);
        } else {
            drawIcon(draw, mat, TextLuicideConstants.user, headX, headY, ROW_HEAD_SIZE, 6f, alpha);
        }

        float buttonX = rowX + rowWidth - 4f - ROW_BUTTON_SIZE;
        float buttonY = rowY + (ROW_HEIGHT - ROW_BUTTON_SIZE) / 2f;
        boolean buttonHovered = isInside(buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE, mouseX, mouseY);
        draw.rect(mat, buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE, 4f,
                buttonHovered ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha) : wa(TAB_INACTIVE_HOVER.getRGB(), alpha));
        drawIcon(draw, mat, row.friend ? TextLuicideConstants.minus : TextLuicideConstants.plus,
                buttonX, buttonY, ROW_BUTTON_SIZE, 8f, alpha);

        if (Fonts.interSemiBold != null) {
            float textX = headX + ROW_HEAD_SIZE + 5f;
            float textWidth = buttonX - textX - 5f;
            draw.text(Fonts.interSemiBold, mat, clip(row.name, textWidth, 7f), textX, rowY + 5f, 7f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        actionHits.add(new ActionHit(row.name, buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE));
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

        CrossTexture previewHead = getPopupPreviewHead();
        if (previewHead != null) {
            draw.image(mat, previewHead, iconX, iconY, POPUP_ICON_SIZE, POPUP_ICON_SIZE, 8f, alpha);
        } else {
            drawIcon(draw, mat, TextLuicideConstants.user, iconX, iconY, POPUP_ICON_SIZE, 10f, alpha);
        }

        if (Fonts.interSemiBold != null) {
            String preview = popupName.isBlank() ? "Add friend" : popupName;
            draw.text(Fonts.interSemiBold, mat, clip(preview, 120f, 8f), popupX + 48f, popupY + 18f, 8f,
                    argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        }

        float inputX = getPopupInputX();
        float inputY = getPopupInputY();
        float inputWidth = getPopupInputWidth();
        boolean inputHovered = isInside(inputX, inputY, inputWidth, POPUP_INPUT_HEIGHT, mouseX, mouseY);
        int inputColor = popupInputFocused
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 220), alpha)
                : inputHovered
                ? argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 190), alpha)
                : argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, 160), alpha);
        draw.rect(mat, inputX, inputY, inputWidth, POPUP_INPUT_HEIGHT, 6f, inputColor);

        if (Fonts.interSemiBold != null) {
            String visible = popupName.isBlank() ? "Player name" : popupName;
            int textColor = popupName.isBlank()
                    ? argbMul(TEXT_ON_SURFACE_MUTED, alpha)
                    : argbMul(TEXT_ON_SURFACE_PRIMARY, alpha);
            draw.text(Fonts.interSemiBold, mat, clip(visible, inputWidth - 12f, 6f), inputX + 6f, inputY + 4f, 6f, textColor);

            if (popupInputFocused && blinkOn()) {
                float cursorX = getCursorX(popupName, popupCursorPos, inputX + 6f, inputWidth - 12f, 6f);
                draw.rect(mat, cursorX, inputY + 3f, 1f, POPUP_INPUT_HEIGHT - 6f, 0f,
                        argbMul(SURFACE_CONTROL_ELEVATED, alpha));
            }
        }

        float confirmX = getPopupConfirmX();
        float confirmY = getPopupConfirmY();
        boolean confirmHovered = isInside(confirmX, confirmY, POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, mouseX, mouseY);
        draw.rect(mat, confirmX, confirmY, POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, 6f,
                confirmHovered ? wa(SURFACE_SETTING_HOVER.getRGB(), alpha) : wa(SURFACE_SETTING.getRGB(), alpha));
        drawIcon(draw, mat, TextLuicideConstants.plus, confirmX, confirmY, POPUP_CONFIRM_SIZE, 9f, alpha);
    }

    private void renderEmptyState(Draw2D draw, Matrix4f mat, float contentX, float contentY, float alpha) {
        maxScroll = 0f;
        scroll = 0f;

        if (Fonts.interSemiBold == null) {
            return;
        }

        draw.text(Fonts.interSemiBold, mat, "No players found", contentX, contentY + 10f, 8f,
                argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
        draw.text(Fonts.interSemiBold, mat, "Try search or add one manually.", contentX, contentY + 22f, 6f,
                argbMul(TEXT_ON_SURFACE_SECONDARY, alpha));
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

    private boolean handleSearchClick(float mouseX, float mouseY) {
        if (!isInside(getSearchX(), getSearchY(), getSearchWidth(), SEARCH_HEIGHT, mouseX, mouseY)) {
            return false;
        }

        searchFocused = true;
        popupInputFocused = false;
        searchCursorPos = findCursor(searchText, mouseX, getSearchX() + 6f, 6f);
        return true;
    }

    private boolean handlePopupClick(float mouseX, float mouseY) {
        if (!isInside(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            popupInputFocused = false;
            return false;
        }

        if (isInside(getPopupConfirmX(), getPopupConfirmY(), POPUP_CONFIRM_SIZE, POPUP_CONFIRM_SIZE, mouseX, mouseY)) {
            confirmPopup();
            return true;
        }

        if (isInside(getPopupInputX(), getPopupInputY(), getPopupInputWidth(), POPUP_INPUT_HEIGHT, mouseX, mouseY)) {
            popupInputFocused = true;
            popupCursorPos = findCursor(popupName, mouseX, getPopupInputX() + 6f, 6f);
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

    private boolean handleTextFieldKey(KeyEvent event, boolean popupField) {
        String text = popupField ? popupName : searchText;
        int cursor = popupField ? popupCursorPos : searchCursorPos;

        switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                    int newCursor = wordStart(text, cursor);
                    text = text.substring(0, newCursor) + text.substring(cursor);
                    cursor = newCursor;
                } else if (cursor > 0 && !text.isEmpty()) {
                    text = text.substring(0, cursor - 1) + text.substring(cursor);
                    cursor--;
                }
                setTextFieldState(popupField, text, cursor);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < text.length()) {
                    text = text.substring(0, cursor) + text.substring(cursor + 1);
                }
                setTextFieldState(popupField, text, cursor);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                setTextFieldState(popupField, text, Math.max(0, cursor - 1));
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                setTextFieldState(popupField, text, Math.min(text.length(), cursor + 1));
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                setTextFieldState(popupField, text, 0);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                setTextFieldState(popupField, text, text.length());
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (popupField) {
                    confirmPopup();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private void setTextFieldState(boolean popupField, String text, int cursor) {
        int clampedCursor = Math.clamp(cursor, 0, text.length());
        if (popupField) {
            popupName = text;
            popupCursorPos = clampedCursor;
            return;
        }

        searchText = text;
        searchCursorPos = clampedCursor;
    }

    private void confirmPopup() {
        String name = popupName.trim();
        if (name.isEmpty() || Echo.friendManager == null) {
            SoundUtil.playClick();
            return;
        }

        Echo.friendManager.addFriend(name);
        popupName = "";
        popupCursorPos = 0;
        popupOpen = false;
        popupInputFocused = false;
        SoundUtil.playClick();
    }

    private void toggleFriend(String name) {
        if (Echo.friendManager == null) {
            return;
        }

        if (Echo.friendManager.isFriend(name)) {
            Echo.friendManager.removeFriend(name);
        } else {
            Echo.friendManager.addFriend(name);
        }

        SoundUtil.playClick();
    }

    private List<FriendRow> getRows() {
        Map<String, FriendRow> rows = new LinkedHashMap<>();

        if (Minecraft.getInstance().getConnection() != null) {
            for (PlayerInfo info : Minecraft.getInstance().getConnection().getListedOnlinePlayers()) {
                String name = info.getProfile().name();
                if (name == null || name.isBlank()) {
                    continue;
                }

                if (Minecraft.getInstance().player != null
                        && name.equalsIgnoreCase(Minecraft.getInstance().player.getName().getString())) {
                    continue;
                }

                rows.put(normalize(name), new FriendRow(name, info, PlayerUtils.isFriend(name)));
            }
        }

        if (Echo.friendManager != null) {
            for (String friend : Echo.friendManager.getFriends()) {
                String key = normalize(friend);
                if (key == null) {
                    continue;
                }

                FriendRow existing = rows.get(key);
                if (existing != null) {
                    existing.friend = true;
                    continue;
                }

                rows.put(key, new FriendRow(friend, findPlayerInfo(friend), true));
            }
        }

        List<FriendRow> out = new ArrayList<>(rows.values());
        out.removeIf(row -> !matchesSearch(row.name));
        out.sort(Comparator
                .comparing((FriendRow row) -> !row.friend)
                .thenComparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private PlayerInfo findPlayerInfo(String name) {
        if (Minecraft.getInstance().getConnection() == null) {
            return null;
        }

        return Minecraft.getInstance().getConnection().getPlayerInfo(name);
    }

    private CrossTexture getPopupPreviewHead() {
        if (popupName.isBlank()) {
            return null;
        }

        PlayerInfo info = findPlayerInfo(popupName.trim());
        if (info == null) {
            return null;
        }

        return PlayerHeadTextures.getHead(info);
    }

    private boolean matchesSearch(String name) {
        String query = searchText.trim();
        if (query.isEmpty()) {
            return true;
        }

        return name.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT));
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

    private float getSearchX() {
        return x + MENU_WIDTH - MENU_PADDING - HEADER_BUTTON_SIZE - 6f - getSearchWidth();
    }

    private float getSearchY() {
        return y + 4f;
    }

    private float getSearchWidth() {
        return 88f;
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

    private String insertChar(String text, int cursor, char chr) {
        int clampedCursor = Math.clamp(cursor, 0, text.length());
        return text.substring(0, clampedCursor) + chr + text.substring(clampedCursor);
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

    private int findCursor(String text, float mouseX, float textStartX, float size) {
        if (Fonts.interSemiBold == null) {
            return text.length();
        }

        float relativeX = mouseX - textStartX;
        if (relativeX <= 0f) {
            return 0;
        }

        for (int i = 0; i <= text.length(); i++) {
            float width = Fonts.interSemiBold.getWidth(text.substring(0, i), size);
            if (relativeX <= width) {
                return i;
            }
        }

        return text.length();
    }

    private float getCursorX(String text, int cursor, float textStartX, float maxWidth, float size) {
        if (Fonts.interSemiBold == null) {
            return textStartX;
        }

        int clampedCursor = Math.clamp(cursor, 0, text.length());
        float cursorX = textStartX + Fonts.interSemiBold.getWidth(text.substring(0, clampedCursor), size);
        return Math.min(cursorX, textStartX + maxWidth);
    }

    private boolean blinkOn() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
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

    private void drawIcon(Draw2D draw, Matrix4f mat, String icon, float x, float y, float boxSize, float iconSize, float alpha) {
        if (Fonts.lucide == null) {
            return;
        }

        float iconWidth = Fonts.lucide.getWidth(icon, iconSize);
        float iconX = x + (boxSize - iconWidth) / 2f;
        float iconY = y + boxSize / 2f - iconSize / 2f;
        draw.text(Fonts.lucide, mat, icon, iconX, iconY, iconSize, argbMul(TEXT_ON_SURFACE_PRIMARY, alpha));
    }

    private String normalize(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private static class ActionHit {
        private final String name;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private ActionHit(String name, float x, float y, float width, float height) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class FriendRow {
        private final String name;
        private final PlayerInfo info;
        private boolean friend;

        private FriendRow(String name, PlayerInfo info, boolean friend) {
            this.name = name;
            this.info = info;
            this.friend = friend;
        }
    }
}
