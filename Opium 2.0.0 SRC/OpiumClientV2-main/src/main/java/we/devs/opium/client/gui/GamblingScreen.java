package we.devs.opium.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import we.devs.opium.api.utilities.RenderUtils;

import java.util.Random;

public class GamblingScreen extends Screen {
    private static final int SLOT_COUNT = 3; // Number of slots
    private static final int SLOT_WIDTH = 50; // Width of each slot
    private static final int SLOT_HEIGHT = 50; // Height of each slot
    private static final int SPIN_DURATION = 100; // Duration of the spin in ticks
    private static final int SPIN_SPEED = 5; // Speed of the spin (lower = faster)
    private static final int TITLE_BAR_HEIGHT = 25; // Height of the title bar

    private int[] slotValues = new int[SLOT_COUNT]; // Current values of the slots
    private int spinTime = 0; // Timer for the spin
    private boolean spinning = false; // Whether the slots are spinning
    private ButtonWidget spinButton; // Button to trigger the spin

    // Window properties
    private int windowX, windowY, windowWidth, windowHeight; // Window dimensions and position
    private int dragOffsetX, dragOffsetY; // Dragging offsets
    private boolean isDragging = false; // Drag flag

    // Animation properties
    private float windowOpacity = 0.0f; // Fade-in animation for the window
    private float spinSlowdown = 1.0f; // Spin slowdown factor
    private boolean isWin = false; // Whether the player won
    private boolean isLose = false; // Whether the player lost
    private float winLoseAnimationTimer = 0.0f; // Timer for win/lose animations

    // Win/Lose counters
    private int winCount = 0;
    private int loseCount = 0;

    public GamblingScreen() {
        super(Text.literal("Gambling Screen"));
    }

    @Override
    protected void init() {
        super.init();

        // Initialize window size and position
        windowWidth = 300; // Fixed width for the window
        windowHeight = 200; // Fixed height for the window
        windowX = (this.width - windowWidth) / 2; // Center the window horizontally
        windowY = (this.height - windowHeight) / 2; // Center the window vertically

        // Initialize the spin button
        spinButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Spin"), button -> {
            if (!spinning) {
                startSpin();
            }
        }).dimensions(windowX + (windowWidth - 100) / 2, windowY + windowHeight - 40, 100, 20).build());
    }

    private void startSpin() {
        spinning = true;
        spinTime = 0;
        spinSlowdown = 1.0f; // Reset spin slowdown
        isWin = false; // Reset win state
        isLose = false; // Reset lose state
        winLoseAnimationTimer = 0.0f; // Reset animation timer
        for (int i = 0; i < SLOT_COUNT; i++) {
            slotValues[i] = new Random().nextInt(10); // Randomize initial slot values
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fade-in animation for the window
        if (windowOpacity < 1.0f) {
            windowOpacity = Math.min(windowOpacity + delta * 0.05f, 1.0f);
        }

        // Render the window and its contents
        renderWindow(context, mouseX, mouseY, delta);

        // Render the spin button and other elements
        super.render(context, mouseX, mouseY, delta);

        // Render win/lose vignette animation (screen-wide)
        if (isWin || isLose) {
            renderWinLoseVignette(context, delta);
        }

        // Render win/lose counters in the bottom-left corner
        renderWinLoseCounters(context);
    }

    private void renderWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the window background with opacity
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, (int) (windowOpacity * 255) << 24 | 0x1E1E1E);

        // Render the title bar
        context.fill(windowX, windowY, windowX + windowWidth, windowY + TITLE_BAR_HEIGHT, (int) (windowOpacity * 255) << 24 | 0x333333);
        RenderUtils.drawCenteredString(context.getMatrices(), "The Key To Happiness", windowX + (float) windowWidth / 2, windowY + 10, 0xFFFFFFFF);

        // Render the slot interface
        renderSlots(context);

        // Update the spin state
        if (spinning) {
            updateSpin(delta);
        }

        // Render the spin button with modern styling
        renderModernButton(context, spinButton, mouseX, mouseY);
    }

    private void renderModernButton(DrawContext context, ButtonWidget button, int mouseX, int mouseY) {
        int buttonX = button.getX();
        int buttonY = button.getY();
        int buttonWidth = button.getWidth();
        int buttonHeight = button.getHeight();

        // Check if the mouse is hovering over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

        // Button background color (gradient for modern look)
        int startColor = isHovered ? 0xFF4CAF50 : 0xFF2196F3; // Green when hovered, blue otherwise
        int endColor = isHovered ? 0xFF45A049 : 0xFF1E88E5; // Darker green when hovered, darker blue otherwise

        // Draw gradient background
        context.fillGradient(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, startColor, endColor);

        // Draw button text
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                button.getMessage(),
                buttonX + buttonWidth / 2,
                buttonY + (buttonHeight - 8) / 2,
                0xFFFFFFFF
        );
    }

    private void renderSlots(DrawContext context) {
        int startX = windowX + (windowWidth - (SLOT_COUNT * SLOT_WIDTH + (SLOT_COUNT - 1) * 10)) / 2;
        int startY = windowY + TITLE_BAR_HEIGHT + 20;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = startX + i * (SLOT_WIDTH + 10);
            int slotY = startY;

            // Draw slot background
            context.fill(slotX, slotY, slotX + SLOT_WIDTH, slotY + SLOT_HEIGHT, 0xFF000000);

            // Draw slot value
            if (spinning) {
                slotValues[i] = (slotValues[i] + 1) % 10; // Animate the slot value
            }
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    String.valueOf(slotValues[i]),
                    slotX + SLOT_WIDTH / 2,
                    slotY + SLOT_HEIGHT / 2 - 5,
                    0xFFFFFF
            );
        }
    }

    private void updateSpin(float delta) {
        spinTime++;
        if (spinTime >= SPIN_DURATION) {
            spinning = false;
            checkWinCondition(); // Check if the player won
        } else {
            // Gradually slow down the spin
            spinSlowdown = Math.max(spinSlowdown - delta * 0.01f, 0.1f);
        }
    }

    private void checkWinCondition() {
        // Check if all slot values are the same (win condition)
        boolean win = true;
        for (int i = 1; i < SLOT_COUNT; i++) {
            if (slotValues[i] != slotValues[0]) {
                win = false;
                break;
            }
        }

        // Set win/lose state and update counters
        if (win) {
            isWin = true;
            winCount++; // Increment win counter
        } else {
            isLose = true;
            loseCount++; // Increment lose counter
        }

        // Start the win/lose animation timer
        winLoseAnimationTimer = 0.0f;
    }

    private void renderWinLoseVignette(DrawContext context, float delta) {
        winLoseAnimationTimer += delta;

        // Determine the color for the vignette
        int color;
        if (isWin) {
            color = 0xFF00FF00; // Green for win
        } else {
            color = 0xFFFF0000; // Red for lose
        }

        // Calculate the flashing effect (alternate between visible and invisible)
        float flashSpeed = 10.0f; // Speed of the flashing effect
        float opacity = (float) Math.abs(Math.sin(winLoseAnimationTimer * flashSpeed)) * 0.5f + 0.5f; // Flashing effect
        int alpha = (int) (opacity * 127) << 24; // Half transparency for the vignette

        // Draw the vignette effect (screen-wide)
        context.fillGradient(
                0, 0, this.width, this.height,
                color | alpha, color | alpha
        );

        // End the animation after 2 seconds
        if (winLoseAnimationTimer >= 2.0f) {
            isWin = false;
            isLose = false;
        }
    }

    private void renderWinLoseCounters(DrawContext context) {
        // Render win/lose counters in the bottom-left corner
        String winText = "Wins: " + winCount;
        String loseText = "Losses: " + loseCount;

        int textX = 10; // 10 pixels from the left edge
        int textY = this.height - 30; // 30 pixels from the bottom edge

        // Draw win counter
        context.drawTextWithShadow(
                this.textRenderer,
                winText,
                textX,
                textY,
                0x00FF00 // Green color for wins
        );

        // Draw lose counter
        context.drawTextWithShadow(
                this.textRenderer,
                loseText,
                textX,
                textY + 12, // 12 pixels below the win counter
                0xFF0000 // Red color for losses
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left mouse button
            // Check if the title bar is clicked
            if (mouseX >= windowX && mouseX <= windowX + windowWidth && mouseY >= windowY && mouseY <= windowY + TITLE_BAR_HEIGHT) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - windowX);
                dragOffsetY = (int) (mouseY - windowY);
                return true; // Avoid further processing
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left mouse button
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 0) { // Left mouse button
            windowX = (int) (mouseX - dragOffsetX);
            windowY = (int) (mouseY - dragOffsetY);

            // Update button position to stay within the window
            spinButton.setX(windowX + (windowWidth - spinButton.getWidth()) / 2);
            spinButton.setY(windowY + windowHeight - 40);

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // Allow the screen to be closed with the ESC key
    }

    @Override
    public void applyBlur(float delta) {
        // Override to prevent blur
    }
}