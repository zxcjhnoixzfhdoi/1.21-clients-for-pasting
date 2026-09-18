package we.devs.opium.client.gui.config;

import me.x150.renderer.render.Renderer2d;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.miscellaneous.ConfigManager;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.api.utilities.SnowflakeRenderer;  // Import the SnowflakeRenderer

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManagerScreen extends Screen {

    private int windowX, windowY, windowWidth, windowHeight; // Window dimensions and position
    private int dragOffsetX, dragOffsetY; // Dragging offsets
    private boolean isDragging = false; // Drag flag
    private static final int TITLE_BAR_HEIGHT = 25; // Adjusted title bar height
    private List<String> configFiles;  // List of config files

    private long animationStartTime = 0L; // Animation timer
    private String loadedConfig = null; // Keep track of the loaded config

    private final SnowflakeRenderer snowflakeRenderer; // Declare SnowflakeRenderer

    public ConfigManagerScreen() {
        super(Text.literal("Config Manager"));
        snowflakeRenderer = new SnowflakeRenderer(); // Initialize SnowflakeRenderer
    }

    @Override
    public void init() {
        File configDirectory = new File(String.valueOf(Path.of(ConfigManager.CONFIG_DIRECTORY)));
        configFiles = new ArrayList<>();
        if (configDirectory.exists() && configDirectory.isDirectory()) {
            String[] files = configDirectory.list((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (String file : files) {
                    configFiles.add(file.replace(".json", ""));
                }
            }
        }

        // Window size and position
        windowWidth = (int) (this.width * 0.5); // 50% of screen width
        windowHeight = (int) (this.height * 0.6); // 60% of screen height
        windowX = (this.width - windowWidth) / 2;
        windowY = (this.height - windowHeight) / 2;

        snowflakeRenderer.initializeSnowflakes(this.width, this.height); // Initialize snowflakes
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);

        // Render snowflakes in the background
        snowflakeRenderer.renderSnowflakes(context, this.width, this.height);

        // Draw background
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF1E1E1E);

        // Title bar
        context.fill(windowX, windowY, windowX + windowWidth, windowY + TITLE_BAR_HEIGHT, 0xFF333333);
        RenderUtils.drawCenteredString(context.getMatrices(), "Config Manager", windowX + (float) windowWidth / 2, windowY + 10, 0xFFFFFFFF);

        // Draw the "Create" button
        drawModernButton(context, windowX + 3, windowY + 5, 100, 15, "Create", false);

        // Configs and Buttons
        int itemY = windowY + TITLE_BAR_HEIGHT + 10;
        int boxHeight = 35;

        for (String configFile : configFiles) {
            int boxWidth = windowWidth - 40;
            int boxX = windowX + 20;

            // Highlight on hover
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= itemY && mouseY <= itemY + boxHeight;
            Color boxColor = isHovered ? new Color(50, 50, 50) : new Color(40, 40, 40);
            Renderer2d.renderRoundedQuad(context.getMatrices(), boxColor, boxX, itemY, boxX + boxWidth, itemY + boxHeight, 8, 50);

            // Config name
            RenderUtils.drawString(context.getMatrices(), configFile, boxX + 10, itemY + 10, 0xFFFFFFFF);

            // Only apply green glow to the loaded config
            if (configFile.equals(loadedConfig)) {
                drawSuccessAnimation(context, boxX, itemY, boxWidth, boxHeight);
            }

            // Buttons
            int buttonWidth = 70, buttonHeight = 25;
            int deleteX = boxX + boxWidth - buttonWidth - 10;
            int loadX = deleteX - buttonWidth - 10;

            drawModernButton(context, loadX, itemY + 5, buttonWidth, buttonHeight, "Load", isHovered);
            drawModernButton(context, deleteX, itemY + 5, buttonWidth, buttonHeight, "Delete", isHovered);

            itemY += boxHeight + 10;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int itemY = windowY + TITLE_BAR_HEIGHT + 10;
        int boxHeight = 35;
        if (button == 0) { // Left mouse button
            // Check if the "Create" button is clicked
            int buttonWidth = 100;  // Corrected width of the button
            int buttonHeight = 15;  // Corrected height of the button
            int buttonX = windowX + 3;
            int buttonY = windowY + 5;

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                // Trigger the desired method
                onCreateButtonClick();
                return true; // Avoid further processing
            }

            if (mouseX >= windowX && mouseX <= windowX + windowWidth && mouseY >= windowY && mouseY <= windowY + TITLE_BAR_HEIGHT) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - windowX);
                dragOffsetY = (int) (mouseY - windowY);
                return true; // Avoid further processing
            }
        }

        for (String configFile : new ArrayList<>(configFiles)) {
            int boxWidth = windowWidth - 40;
            int boxX = windowX + 20;

            int buttonWidth = 70, buttonHeight = 25;
            int deleteX = boxX + boxWidth - buttonWidth - 10;
            int loadX = deleteX - buttonWidth - 10;

            if (mouseX >= loadX && mouseX <= loadX + buttonWidth && mouseY >= itemY + 5 && mouseY <= itemY + 5 + buttonHeight) {
                System.out.println("Load button clicked for config: " + configFile); // Add logging
                loadConfig(configFile);
                return true; // Avoid further processing
            }

            if (mouseX >= deleteX && mouseX <= deleteX + buttonWidth && mouseY >= itemY + 5 && mouseY <= itemY + 5 + buttonHeight) {
                System.out.println("Delete button clicked for config: " + configFile); // Add logging
                deleteConfig(configFile);
                return true; // Avoid further processing
            }

            itemY += boxHeight + 10;
        }

        return false; // Return false to prevent closing
    }




    private void onCreateButtonClick() {
        int numb = 0;
        while (true) {
            if (!Opium.CONFIG_MANAGER.getAvailableConfigs().contains("newConfig" + numb)) {
                try {
                    Opium.CONFIG_MANAGER.saveConfig("newConfig" + numb);
                    configFiles.add("newConfig" + numb);
                    break;
                } catch (IOException e) {
                    Opium.LOGGER.error("Failed to save new config", e);
                }
            }
            numb++;
        }
    }

    private void drawModernButton(DrawContext context, int x, int y, int width, int height, String label, boolean hovered) {
        Color bgColor = hovered ? new Color(70, 130, 180) : new Color(50, 60, 70);
        Renderer2d.renderRoundedQuad(context.getMatrices(), bgColor, x, y, x + width, y + height, 6, 100);
        RenderUtils.drawCenteredString(context.getMatrices(), label, x + (float) width / 2, y + (float) (height) / 2, 0xFFFFFFFF);
    }

    private void drawSuccessAnimation(DrawContext context, int x, int y, int width, int height) {
        int timeElapsed = (int) (System.currentTimeMillis() - animationStartTime);
        int alpha = Math.max(255 - (timeElapsed * 255 / 800), 0); // Fades out over 800ms
        Color glow = new Color(0, 255, 0, alpha); // Green glow
        Renderer2d.renderRoundedQuad(context.getMatrices(), glow, x, y, x + width, y + height, 10, 100);

        // Ensure the animation remains visible for at least 800ms
        if (timeElapsed > 800) {
            // Reset the animation once it completes
            loadedConfig = null;
        }
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
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void loadConfig(String configName) {
        try {
            Opium.CONFIG_MANAGER.loadConfig(configName);
            loadedConfig = configName;  // Set the loaded config
            animationStartTime = System.currentTimeMillis(); // Start the animation timer

            // Optionally, show a confirmation message or feedback to the user
            ChatUtils.sendMessage("Config " + configName + " loaded successfully!");
        } catch (IOException e) {
            Opium.LOGGER.error("Failed to load config: {}", configName);
            ChatUtils.sendMessage("Failed to load config: " + configName);
        }
    }


    private void deleteConfig(String configName) {
        Opium.CONFIG_MANAGER.delete(configName);
        configFiles.remove(configName);
    }
}
