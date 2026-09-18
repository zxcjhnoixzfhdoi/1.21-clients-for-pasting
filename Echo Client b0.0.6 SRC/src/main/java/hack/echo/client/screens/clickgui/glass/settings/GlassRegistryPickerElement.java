package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.ScreenManager;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.strings.Concat;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassRegistryPickerElement<T> implements GlassSettingElement {

    @Getter
    private final RegistryPickerSetting<T> setting;
    private final List<RegistryPickerSetting.EntryGroup<T>> groups;
    private final Function<T, String> nameProvider;

    private float x, y;
    private boolean expanded = false;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private boolean wasHovered = false;
    private String searchQuery = "";
    private int cursorPos = 0;
    private final Animation cursorBlinkAnim = new Animation(Easing.EASE_IN_OUT_QUART, 400);
    private boolean viewingGroups = true;
    private static final int maxVisible = 8;
    private int scrollOffset = 0;

    public GlassRegistryPickerElement(RegistryPickerSetting<T> setting) {
        this.setting = setting;
        this.groups = setting.getGroups();
        this.nameProvider = setting.getNameProvider();
        this.viewingGroups = !groups.isEmpty();
        expandAnimation.start(0, 0);
    }

    private float getExpandedHeight() {
        if (!expanded)
            return 0;
        float contentH = maxVisible * (SETTING_HEIGHT - 2);
        if (groups.isEmpty())
            return SETTING_HEIGHT + SETTING_SPACING + contentH + PADDING;
        float viewToggleH = SETTING_HEIGHT * 0.7f;
        return SETTING_HEIGHT + SETTING_SPACING + viewToggleH + SETTING_SPACING + contentH + PADDING;
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;

        float target = expanded ? 1f : 0f;
        expandAnimation.updateTo(target);
        float anim = (float) expandAnimation.getDelta();

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float totalHeight = getHeight();

        boolean hovered = isInside(renderX, y, renderW, SETTING_HEIGHT, mouseX, mouseY);
        if (hovered && !wasHovered) {
            SoundUtil.playHover();
        }
        wasHovered = hovered;

        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipManager.request(setting, setting.getNameSequence(), description, mouseX, mouseY);
            }
        }

        draw.rect(mat, renderX, y, renderW, totalHeight, RADIUS_MD,
                wa((hovered ? TAB_INACTIVE_HOVER : TAB_INACTIVE).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, totalHeight - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        if (Fonts.interSemiBold != null) {
            CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 14);
            draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + SETTING_HEIGHT / 2 - 4, 6,
                    argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

            int selectedCount = setting.getSelectedCount();
            CharSequence summary = selectedCount == 0 ? Concat.of("None")
                    : Concat.of(String.valueOf(selectedCount), " sel.");
            float summaryW = Fonts.interSemiBold.getWidth(summary, 6);
            draw.text(Fonts.interSemiBold, mat, summary, renderX + renderW - PADDING - summaryW, y + SETTING_HEIGHT / 2 - 4, 6,
                    argbMul(TEXT_ON_SURFACE_MUTED, delta));
        }

        if (anim > ANIMATION_EPSILON) {
            float currentY = y + SETTING_HEIGHT;
            int alpha = (int) (anim * 255);

            float searchY = currentY;
            draw.rect(mat, renderX + PADDING, searchY, renderW - PADDING * 2, SETTING_HEIGHT, RADIUS_SM,
                    argbMul(withAlpha(SURFACE_INTERACTIVE_MUTED, alpha), delta));
            if (Fonts.interSemiBold != null) {
                CharSequence displayText = searchQuery.isEmpty() ? Concat.of("Search...") : searchQuery;
                int textColor = searchQuery.isEmpty() ? argbMul(withAlpha(TEXT_ON_SURFACE_MUTED, alpha), delta)
                        : argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta);
                draw.text(Fonts.interSemiBold, mat, displayText, renderX + PADDING + 4, searchY + SETTING_HEIGHT / 2 - 3, 6,
                        textColor);
                if (ScreenManager.focusedSetting == setting) {
                    if (cursorBlinkAnim.isFinished()) {
                        cursorBlinkAnim.start(cursorBlinkAnim.getTo(), 1.0 - cursorBlinkAnim.getTo());
                    }
                    int cursorIndex = Math.min(cursorPos, searchQuery.length());
                    float cursorX = renderX + PADDING + 4
                            + Fonts.interSemiBold.getWidth(searchQuery.substring(0, cursorIndex), 6);
                    int cursorAlpha = (int) (cursorBlinkAnim.getDelta() * alpha);
                    draw.rect(mat, cursorX, searchY + 2, 1, SETTING_HEIGHT - 4, 0,
                            argbMul(withAlpha(SURFACE_CONTROL_ELEVATED, cursorAlpha), delta));
                }
            }
            currentY += SETTING_HEIGHT + SETTING_SPACING;

            if (!groups.isEmpty()) {
                float toggleH = SETTING_HEIGHT * 0.7f;
                float toggleY = currentY;
                float toggleBtnW = (renderW - PADDING * 2 - SETTING_SPACING) / 2f;
                int groupsColor = viewingGroups ? argbMul(withAlpha(ACCENT_PRIMARY, alpha), delta)
                        : argbMul(withAlpha(TAB_INACTIVE_HOVER, alpha), delta);
                int entriesColor = !viewingGroups ? argbMul(withAlpha(ACCENT_PRIMARY, alpha), delta)
                        : argbMul(withAlpha(TAB_INACTIVE_HOVER, alpha), delta);
                draw.rect(mat, renderX + PADDING, toggleY, toggleBtnW, toggleH, RADIUS_SM, groupsColor);
                draw.rect(mat, renderX + PADDING + toggleBtnW + SETTING_SPACING, toggleY, toggleBtnW, toggleH, RADIUS_SM,
                        entriesColor);
                if (Fonts.interSemiBold != null) {
                    CharSequence groupsLabel = Concat.of("Groups");
                    CharSequence entriesLabel = Concat.of("Entries");
                    draw.text(Fonts.interSemiBold, mat, groupsLabel,
                            renderX + PADDING + toggleBtnW / 2 - Fonts.interSemiBold.getWidth(groupsLabel, 5) / 2,
                            toggleY + toggleH / 2 - 2, 5,
                            viewingGroups ? argbMul(withAlpha(TEXT_ON_ACCENT, alpha), delta) : argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta));
                    draw.text(Fonts.interSemiBold, mat, entriesLabel,
                            renderX + PADDING + toggleBtnW + SETTING_SPACING + toggleBtnW / 2
                                    - Fonts.interSemiBold.getWidth(entriesLabel, 5) / 2,
                            toggleY + toggleH / 2 - 2, 5,
                            !viewingGroups ? argbMul(withAlpha(TEXT_ON_ACCENT, alpha), delta) : argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta));
                }
                currentY += toggleH + SETTING_SPACING;
            }

            float contentY = currentY;

            if (viewingGroups) {
                List<RegistryPickerSetting.EntryGroup<T>> filtered = filterGroups(groups);
                int totalItems = filtered.size();
                boolean needsScrolling = totalItems > maxVisible;

                float itemY = contentY;
                int startIdx = needsScrolling ? scrollOffset : 0;
                int endIdx = needsScrolling ? Math.min(scrollOffset + maxVisible, filtered.size()) : filtered.size();
                for (int i = startIdx; i < endIdx; i++) {
                    RegistryPickerSetting.EntryGroup<T> group = filtered.get(i);
                    RegistryPickerSetting.GroupState state = setting.getGroupState(group);

                    if (Fonts.interSemiBold != null) {
                        CharSequence stateText = state == RegistryPickerSetting.GroupState.ALL ? Concat.of("[x]")
                                : state == RegistryPickerSetting.GroupState.PARTIAL ? Concat.of("[~]") : Concat.of("[ ]");
                        Color stateColor = state == RegistryPickerSetting.GroupState.ALL ? ACCENT_PRIMARY : TEXT_ON_SURFACE_MUTED;

                        draw.text(Fonts.interSemiBold, mat, stateText, renderX + PADDING + 4,
                                itemY + (SETTING_HEIGHT - 2) / 2 - 3, 6, argbMul(withAlpha(stateColor, alpha), delta));

                        CharSequence groupName = ClientUtil.truncateName(group.name(), 18);
                        draw.text(Fonts.interSemiBold, mat, groupName, renderX + PADDING + 20,
                                itemY + (SETTING_HEIGHT - 2) / 2 - 3, 6, argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta));
                    }

                    itemY += SETTING_HEIGHT - 2;
                }
            } else {
                List<T> entries = getFilteredEntries();
                int totalItems = entries.size();
                boolean needsScrolling = totalItems > maxVisible;

                float itemY = contentY;
                int startIdx = needsScrolling ? scrollOffset : 0;
                int endIdx = needsScrolling ? Math.min(scrollOffset + maxVisible, entries.size()) : entries.size();
                for (int i = startIdx; i < endIdx; i++) {
                    T entry = entries.get(i);
                    boolean selected = setting.isSelected(entry);

                    if (Fonts.interSemiBold != null) {
                        CharSequence checkText = selected ? Concat.of("[x]") : Concat.of("[ ]");
                        Color checkColor = selected ? ACCENT_PRIMARY : TEXT_ON_SURFACE_MUTED;
                        draw.text(Fonts.interSemiBold, mat, checkText, renderX + PADDING + 4,
                                itemY + (SETTING_HEIGHT - 2) / 2 - 3, 6, argbMul(withAlpha(checkColor, alpha), delta));

                        CharSequence displayName = ClientUtil.truncateName(nameProvider.apply(entry), 18);
                        draw.text(Fonts.interSemiBold, mat, displayName, renderX + PADDING + 20,
                                itemY + (SETTING_HEIGHT - 2) / 2 - 3, 6, argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta));
                    }

                    itemY += SETTING_HEIGHT - 2;
                }
            }
        }
    }

    private List<RegistryPickerSetting.EntryGroup<T>> filterGroups(List<RegistryPickerSetting.EntryGroup<T>> src) {
        List<RegistryPickerSetting.EntryGroup<T>> result = new ArrayList<>(src);
        result.sort(Comparator
                .comparing((RegistryPickerSetting.EntryGroup<T> g) -> {
                    RegistryPickerSetting.GroupState state = setting.getGroupState(g);
                    return state == RegistryPickerSetting.GroupState.ALL ? 0
                            : state == RegistryPickerSetting.GroupState.PARTIAL ? 1 : 2;
                })
                .thenComparing(g -> g.name().toString().toLowerCase()));

        if (searchQuery.isEmpty())
            return result;
        String query = searchQuery.toLowerCase();
        return result.stream()
                .filter(g -> g.name().toString().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    private List<T> getFilteredEntries() {
        List<T> all = new ArrayList<>();
        var filter = setting.getEntryFilter();
        for (T entry : setting.getRegistry()) {
            if (filter.test(entry)) all.add(entry);
        }
        all.sort(Comparator
                .comparing((T e) -> !setting.isSelected(e))
                .thenComparing(e -> nameProvider.apply(e).toLowerCase()));

        if (searchQuery.isEmpty())
            return all;
        String query = searchQuery.toLowerCase();
        return all.stream()
                .filter(e -> nameProvider.apply(e).toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    @Override
    public float getHeight() {
        float anim = (float) expandAnimation.getDelta();
        float baseHeight = SETTING_HEIGHT;
        if (anim > ANIMATION_EPSILON) {
            baseHeight += getExpandedHeight() * anim;
        }
        return baseHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        if (isInside(renderX, y, renderW, SETTING_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            if (expanded) {
                scrollOffset = 0;
                ScreenManager.focusedSetting = null;
            }
            SoundUtil.playExpand(expanded);
            return true;
        }

        float anim = (float) expandAnimation.getDelta();
        if (expanded && anim > EXPAND_THRESHOLD) {
            float searchY = y + SETTING_HEIGHT;
            float toggleH = SETTING_HEIGHT * 0.7f;
            float contentY = groups.isEmpty()
                    ? searchY + SETTING_HEIGHT + SETTING_SPACING
                    : searchY + SETTING_HEIGHT + SETTING_SPACING + toggleH + SETTING_SPACING;

            if (isInside(renderX + PADDING, searchY, renderW - PADDING * 2, SETTING_HEIGHT, (float) mouseX, (float) mouseY)) {
                ScreenManager.focusedSetting = setting;
                cursorBlinkAnim.start(0, 1);
                float clickX = (float) mouseX - renderX - PADDING - 4;
                cursorPos = 0;
                for (int i = 0; i <= searchQuery.length(); i++) {
                    float textW = Fonts.interSemiBold.getWidth(searchQuery.substring(0, i), 6);
                    if (clickX <= textW)
                        break;
                    cursorPos = i;
                }
                cursorPos = Math.min(cursorPos, searchQuery.length());
                return true;
            }

            if (!groups.isEmpty()) {
                float toggleY = searchY + SETTING_HEIGHT + SETTING_SPACING;
                float toggleBtnW = (renderW - PADDING * 2 - SETTING_SPACING) / 2f;
                if (isInside(renderX + PADDING, toggleY, toggleBtnW, toggleH, (float) mouseX, (float) mouseY)) {
                    viewingGroups = true;
                    scrollOffset = 0;
                    SoundUtil.playClick();
                    return true;
                } else if (isInside(renderX + PADDING + toggleBtnW + SETTING_SPACING, toggleY, toggleBtnW, toggleH, (float) mouseX, (float) mouseY)) {
                    viewingGroups = false;
                    scrollOffset = 0;
                    SoundUtil.playClick();
                    return true;
                }
            }

            if (mouseY >= contentY) {
                if (viewingGroups) {
                    List<RegistryPickerSetting.EntryGroup<T>> filtered = filterGroups(groups);
                    float itemY = contentY;
                    int startIdx = filtered.size() > maxVisible ? scrollOffset : 0;
                    int endIdx = filtered.size() > maxVisible ? Math.min(scrollOffset + maxVisible, filtered.size()) : filtered.size();
                    for (int i = startIdx; i < endIdx; i++) {
                        if (isInside(renderX + PADDING, itemY, renderW - PADDING * 2, SETTING_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                            RegistryPickerSetting.EntryGroup<T> group = filtered.get(i);
                            RegistryPickerSetting.GroupState state = setting.getGroupState(group);
                            boolean shouldEnable = state != RegistryPickerSetting.GroupState.ALL;
                            setting.applyGroup(group, shouldEnable);
                            SoundUtil.playToggle(shouldEnable);
                            return true;
                        }
                        itemY += SETTING_HEIGHT - 2;
                    }
                } else {
                    List<T> entries = getFilteredEntries();
                    float itemY = contentY;
                    int startIdx = entries.size() > maxVisible ? scrollOffset : 0;
                    int endIdx = entries.size() > maxVisible ? Math.min(scrollOffset + maxVisible, entries.size()) : entries.size();
                    for (int i = startIdx; i < endIdx; i++) {
                        if (isInside(renderX + PADDING, itemY, renderW - PADDING * 2, SETTING_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                            T entry = entries.get(i);
                            boolean wasSelected = setting.isSelected(entry);
                            setting.setSelected(entry, !wasSelected);
                            SoundUtil.playToggle(!wasSelected);
                            return true;
                        }
                        itemY += SETTING_HEIGHT - 2;
                    }
                }
            }

            ScreenManager.focusedSetting = null;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ScreenManager.focusedSetting != setting)
            return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                int newPos = wordStart(searchQuery, cursorPos);
                searchQuery = searchQuery.substring(0, newPos) + searchQuery.substring(cursorPos);
                cursorPos = newPos;
                scrollOffset = 0;
            } else if (cursorPos > 0) {
                searchQuery = searchQuery.substring(0, cursorPos - 1) + searchQuery.substring(cursorPos);
                cursorPos--;
                scrollOffset = 0;
            }
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < searchQuery.length()) {
                searchQuery = searchQuery.substring(0, cursorPos) + searchQuery.substring(cursorPos + 1);
                scrollOffset = 0;
            }
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursorPos = Math.max(0, cursorPos - 1);
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursorPos = Math.min(searchQuery.length(), cursorPos + 1);
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPos = searchQuery.length();
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            ScreenManager.focusedSetting = null;
            return true;
        }
        return false;
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (ScreenManager.focusedSetting != setting)
            return;
        if (chr >= 32 && chr != 127) {
            int insertionPoint = Math.min(cursorPos, searchQuery.length());
            searchQuery = searchQuery.substring(0, insertionPoint) + chr + searchQuery.substring(insertionPoint);
            cursorPos = insertionPoint + 1;
            scrollOffset = 0;
            cursorBlinkAnim.start(0, 1);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float anim = (float) expandAnimation.getDelta();
        if (expanded && anim > EXPAND_THRESHOLD) {
            float renderX = x + SETTING_INDENT;
            float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
            float contentY = y + SETTING_HEIGHT + SETTING_SPACING + SETTING_HEIGHT + SETTING_SPACING
                    + SETTING_HEIGHT * 0.7f + SETTING_SPACING;
            float contentH = maxVisible * (SETTING_HEIGHT - 2);

            if (isInside(renderX, contentY, renderW, contentH, (float) mouseX, (float) mouseY)) {
                int total = viewingGroups ? filterGroups(groups).size() : getFilteredEntries().size();
                if (total > maxVisible) {
                    scrollOffset = amount > 0 ? Math.max(0, scrollOffset - 1)
                            : Math.min(total - maxVisible, scrollOffset + 1);
                    SoundUtil.playScroll();
                    return true;
                }
            }
        }
        return false;
    }

    private static int wordStart(String text, int pos) {
        int i = pos;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }
}
