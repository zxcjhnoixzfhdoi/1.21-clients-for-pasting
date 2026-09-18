package we.devs.opium.client.gui.click.components;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.api.utilities.TimerUtils;
import we.devs.opium.api.utilities.font.FontRenderers;
import we.devs.opium.client.events.EventKey;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.values.impl.ValueString;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class StringComponent extends Component implements EventListener {
    private static final int PADDING_X = 3;
    private static final int CURSOR_WIDTH = 1;
    private static final int CURSOR_HEIGHT = 10;
    private static final int BLINK_INTERVAL = 400;
    private static final Color BRACKET_COLOR = new Color(180, 180, 180, 200);
    private static final Color TEXT_COLOR = Color.LIGHT_GRAY;

    private final ValueString value;
    private boolean listening;
    private String currentString = "";
    private final TimerUtils timer = new TimerUtils();
    private boolean cursorVisible = false;
    private int renderOffset = 0;

    public StringComponent(ValueString value, int offset, Frame parent) {
        super(offset, parent);
        Opium.EVENT_MANAGER.register(this);
        this.value = value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        updateCursorBlink();

        int textX = getX() + PADDING_X;
        int textY = getY() + 3;
        int textBoxWidth = getWidth() - 2 * PADDING_X;

        String fullText = getFullText();
        String visibleText = calculateVisibleText(fullText, textBoxWidth);

        // Draw the opening bracket '['
        RenderUtils.drawString(context.getMatrices(), "[", getX(), textY, BRACKET_COLOR.getRGB());

        // Draw the text
        drawText(context, textX, textY, textBoxWidth, visibleText);

        // Draw the closing bracket ']'
        int closingBracketX = getX() + getWidth() - (int) FontRenderers.fontRenderer.getStringWidth("]");
        RenderUtils.drawString(context.getMatrices(), "]", closingBracketX, textY, BRACKET_COLOR.getRGB());

        // Draw the blinking cursor
        if (listening && cursorVisible) {
            drawCursor(context, textX, textY, visibleText);
        }
    }

    private void updateCursorBlink() {
        if (timer.hasTimeElapsed(BLINK_INTERVAL)) {
            cursorVisible = !cursorVisible;
            timer.reset();
        }
    }

    private String getFullText() {
        return listening ? currentString : value.getValue();
    }

    private String calculateVisibleText(String fullText, int textBoxWidth) {
        int fullTextWidth = mc.textRenderer.getWidth(fullText);
        if (fullTextWidth > textBoxWidth) {
            renderOffset = Math.min(renderOffset, fullTextWidth - textBoxWidth);
            return getVisibleText(fullText, renderOffset, textBoxWidth);
        }
        renderOffset = 0;
        return fullText;
    }

    private void drawText(DrawContext context, int textX, int textY, int textBoxWidth, String visibleText) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        context.enableScissor(textX, textY, textX + textBoxWidth, textY + getHeight());
        RenderUtils.drawString(context.getMatrices(), visibleText, textX, textY, TEXT_COLOR.getRGB());
        context.disableScissor();
        matrices.pop();
    }

    private void drawCursor(DrawContext context, int textX, int textY, String visibleText) {
        int visibleTextWidth = (int) FontRenderers.fontRenderer.getStringWidth(visibleText);
        int cursorX = textX + visibleTextWidth;

        // Draw the blinking cursor
        RenderUtils.drawRect(
                context.getMatrices(),
                cursorX,
                textY,
                cursorX + CURSOR_WIDTH,
                textY + CURSOR_HEIGHT,
                BRACKET_COLOR
        );
    }

    private String getVisibleText(String fullText, int renderOffset, int textBoxWidth) {
        StringBuilder visibleText = new StringBuilder();
        int currentWidth = 0;

        for (int i = renderOffset; i < fullText.length(); i++) {
            char c = fullText.charAt(i);
            int charWidth = (int) FontRenderers.fontRenderer.getStringWidth(String.valueOf(c));

            if (currentWidth + charWidth > textBoxWidth) {
                break;
            }

            visibleText.append(c);
            currentWidth += charWidth;
        }

        return visibleText.toString();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
            listening = !listening;
            currentString = value.getValue();
        } else if (listening) {
            updateString();
            listening = false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (listening && !isHovering(mouseX, mouseY)) {
            updateString();
            listening = false;
        }
    }

    @Override
    public void onKey(EventKey event) {
        if (listening && event.getAction() == GLFW.GLFW_PRESS) {
            handleKeyEvent(event);
        }
    }

    private void handleKeyEvent(EventKey event) {
        switch (event.getKeyCode()) {
            case InputUtil.GLFW_KEY_ENTER -> {
                updateString();
                listening = false;
            }
            case InputUtil.GLFW_KEY_BACKSPACE -> {
                if (!currentString.isEmpty()) {
                    currentString = currentString.substring(0, currentString.length() - 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (renderOffset > 0) {
                    renderOffset--;
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (mc.textRenderer.getWidth(currentString) > getWidth() - 6) {
                    renderOffset++;
                }
            }
            case GLFW.GLFW_KEY_SPACE -> currentString += " ";
            case InputUtil.GLFW_KEY_V -> {
                if (isCtrlPressed()) {
                    String clipboardData = getClipboard();
                    if (clipboardData != null) {
                        currentString += clipboardData;
                    }
                }
            }
            default -> handleTypedCharacter(event);
        }
    }

    private void handleTypedCharacter(EventKey event) {
        String keyName = GLFW.glfwGetKeyName(event.getKeyCode(), GLFW.glfwGetKeyScancode(event.getKeyCode()));
        if (keyName != null && !keyName.isEmpty()) {
            char typedChar = keyName.charAt(0);
            if (isShiftPressed()) {
                typedChar = Character.toUpperCase(typedChar);
            }
            if (isValidChatCharacter(typedChar)) {
                currentString += typedChar;

                if (mc.textRenderer.getWidth(currentString) > getWidth() - 6) {
                    renderOffset++;
                }
            }
        }
    }

    private boolean isValidChatCharacter(char c) {
        return c >= ' ' && c != 127;
    }

    private void updateString() {
        value.setValue(currentString);
    }

    private String getClipboard() {
        try {
            long window = GLFW.glfwGetCurrentContext();
            CharSequence glfwClipboard = GLFW.glfwGetClipboardString(window);
            if (glfwClipboard != null) {
                return glfwClipboard.toString();
            }

            if (!GraphicsEnvironment.isHeadless()) {
                return (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .getData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
        }

        return null;
    }

    private boolean isCtrlPressed() {
        long handle = mc.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean isShiftPressed() {
        long handle = mc.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public ValueString getValue() {
        return value;
    }
}