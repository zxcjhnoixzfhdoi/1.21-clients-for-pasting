package we.devs.opium.asm.mixins;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import we.devs.opium.api.utilities.SnowflakeRenderer;

import java.util.*;

@Mixin(LogoDrawer.class)
public class MixinLogoDrawer {

    @Shadow
    @Final
    private boolean ignoreAlpha; // Shadowed field for alpha transparency handling

    @Unique
    private static final int INITIAL_SNOWFLAKE_COUNT = 100; // Initial snowflake count
    @Unique
    private static final Random RANDOM = new Random(); // Random number generator for effects

    @Unique
    private long nextLightningTime = 0; // Time for the next lightning effect
    @Unique
    private long shakeEndTime = 0; // End time for screen shake effect
    @Unique
    private static String text = "0piumh4ck.cc"; // Text to display on the screen

    // Using new utility-based SnowflakeManager for handling snowflake data
    @Unique
    private final SnowflakeRenderer snowflakeManager = new SnowflakeRenderer();

    static {
        // Fetch the mod metadata to set the text dynamically if available
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("opium");
        if (modContainer.isPresent()) {
            StringBuilder builder = new StringBuilder("0piumh4ck.cc by ");
            Iterator<Person> i = modContainer.get().getMetadata().getAuthors().iterator();
            while (i.hasNext()) {
                Person next = i.next();
                builder.append(next.getName());
                if (i.hasNext()) builder.append(" & ");
            }
            text = builder.toString(); // Set the dynamic text to include authors
        }
    }

    /**
     * @author Cxiy
     * @reason On Top
     */
    @Overwrite
    public void draw(DrawContext context, int screenWidth, float alpha, int y) {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        // Handle screen shake effect (random offset)
        int shakeOffsetX = 0, shakeOffsetY = 0;
        if (System.currentTimeMillis() < shakeEndTime) {
            shakeOffsetX = RANDOM.nextInt(10) - 5;
            shakeOffsetY = RANDOM.nextInt(10) - 5;
        }

        // Apply the shake effect (if any)
        context.getMatrices().push();
        context.getMatrices().translate(shakeOffsetX, shakeOffsetY, 0);

        // Draw background gradient (e.g., darkened background)
        context.fillGradient(0, 0, screenWidth, screenHeight, 0x55000000, 0x33000000);

        // Initialize or update snowflakes using the new utility
        snowflakeManager.resizeSnowflakesIfNecessary(screenWidth);
        snowflakeManager.renderSnowflakes(context, screenWidth, screenHeight);

        // Draw lightning effect if needed
        handleLightning(context, screenWidth, screenHeight);

        // Draw the logo first to ensure it’s behind the text
        drawLogo(context, screenWidth, alpha, y);

        // Now, draw the text above the logo
        drawText(context, text, screenWidth);

        // Pop the transformation matrix to reset after the drawing
        context.getMatrices().pop();
    }


    private void drawLogo(DrawContext context, int screenWidth, float alpha, int y) {
        float pulsationFactor = 1.0f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        int width = 506, height = 75;
        float scaledWidth = width * pulsationFactor;
        float scaledHeight = height * pulsationFactor;
        float centerX = (screenWidth - scaledWidth) / 2.0f;

        context.getMatrices().push();
        context.getMatrices().translate(centerX + scaledWidth / 2.0f, y + scaledHeight / 2.0f, 0.0);
        context.getMatrices().scale(pulsationFactor, pulsationFactor, 1.0f);
        context.getMatrices().translate(-width / 2.0f, -height / 2.0f, 0.0);

        context.setShaderColor(1.0F, 1.0F, 1.0F, ignoreAlpha ? 1.0F : alpha);
        context.drawTexture(
                Identifier.of("opium", "icons/title.png"),
                0, 0, 0, 0, width, height, width, height
        );

        context.getMatrices().pop();
    }

    private void handleLightning(DrawContext context, int screenWidth, int screenHeight) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextLightningTime) {
            nextLightningTime = currentTime + RANDOM.nextInt(5000) + 3000;
            int startX = RANDOM.nextInt(screenWidth);
            drawLightning(context, startX, screenHeight);
            shakeEndTime = currentTime + 200;
        }
    }

    private void drawLightning(DrawContext context, int startX, int endY) {
        int boltWidth = 2;
        int currentX = startX;
        int currentY = 0;

        while (currentY < endY) {
            int nextX = Math.max(0, Math.min(currentX + RANDOM.nextInt(20) - 10, MinecraftClient.getInstance().getWindow().getScaledWidth()));
            int nextY = Math.min(currentY + RANDOM.nextInt(30), endY);
            context.fill(currentX, currentY, nextX + boltWidth, nextY + boltWidth, 0x60FFFFFF);

            if (RANDOM.nextFloat() < 0.3) {
                drawBranch(context, currentX, currentY, endY / 2);
            }

            currentX = nextX;
            currentY = nextY;
        }
    }

    private void drawBranch(DrawContext context, int startX, int startY, int branchLength) {
        int currentX = startX;
        int currentY = startY;
        for (int i = 0; i < branchLength; i++) {
            int nextX = Math.max(0, Math.min(currentX + RANDOM.nextInt(15) - 7, MinecraftClient.getInstance().getWindow().getScaledWidth()));
            int nextY = currentY + RANDOM.nextInt(15);
            context.fill(currentX, currentY, nextX, nextY, 0x40FFFFFF);

            currentX = nextX;
            currentY = nextY;

            if (RANDOM.nextFloat() < 0.1) {
                break;
            }
        }
    }

    private void drawText(DrawContext context, String text, int screenWidth) {
        int x = 2;
        int yPosition = 10;
        int color = 0xFF808080;

        for (int i = 0; i < text.length(); i++) {
            int charX = x + MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, i));
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, String.valueOf(text.charAt(i)), charX, yPosition, color);
        }

        long time = System.currentTimeMillis();
        int charIndex = (int) ((time / 100) % text.length());
        int glintColor = 0xFFFFFFFF;

        for (int i = 0; i < 5; i++) {
            int currentIndex = (charIndex + i) % text.length();
            int glintCharX = x + MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, currentIndex));
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, String.valueOf(text.charAt(currentIndex)), glintCharX, yPosition, glintColor);
        }
    }
}
