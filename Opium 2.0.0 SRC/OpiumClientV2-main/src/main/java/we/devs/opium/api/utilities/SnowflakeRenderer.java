package we.devs.opium.api.utilities;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class SnowflakeRenderer {

    private static final Random RANDOM = new Random();
    private final List<Map<String, Object>> snowflakes = new ArrayList<>();
    private static final int INITIAL_SNOWFLAKE_COUNT = 100;
    private static final int MAX_SNOWFLAKE_SIZE = 4;
    private static final float MIN_SPEED = 0.5F;
    private static final float MAX_SPEED = 2.5F;
    private static final float MIN_ROTATION_SPEED = -1.0F;
    private static final float MAX_ROTATION_SPEED = 1.0F;
    private static final float OPACITY_DECAY = 0.002F;

    public void initializeSnowflakes(int screenWidth, int screenHeight) {
        snowflakes.clear();
        for (int i = 0; i < INITIAL_SNOWFLAKE_COUNT; i++) {
            snowflakes.add(createSnowflake(screenWidth, screenHeight));
        }
    }

    private Map<String, Object> createSnowflake(int screenWidth, int screenHeight) {
        Map<String, Object> snowflake = new HashMap<>();
        snowflake.put("x", (float) RANDOM.nextInt(screenWidth));
        snowflake.put("y", (float) -RANDOM.nextInt(50));
        snowflake.put("speedY", MIN_SPEED + RANDOM.nextFloat() * (MAX_SPEED - MIN_SPEED));
        snowflake.put("size", RANDOM.nextInt(MAX_SNOWFLAKE_SIZE) + 1);
        snowflake.put("rotationAngle", RANDOM.nextFloat() * 360);
        snowflake.put("rotationSpeed", MIN_ROTATION_SPEED + RANDOM.nextFloat() * (MAX_ROTATION_SPEED - MIN_ROTATION_SPEED));
        snowflake.put("opacity", 1.0F);
        return snowflake;
    }

    public void resizeSnowflakesIfNecessary(int screenWidth) {
        int targetSnowflakeCount = screenWidth / 10;
        if (snowflakes.size() < targetSnowflakeCount) {
            for (int i = snowflakes.size(); i < targetSnowflakeCount; i++) {
                snowflakes.add(createSnowflake(screenWidth, getScreenHeight()));
            }
        } else if (snowflakes.size() > targetSnowflakeCount) {
            snowflakes.subList(targetSnowflakeCount, snowflakes.size()).clear();
        }
    }

    private void updateSnowflake(Map<String, Object> snowflake, int screenWidth, int screenHeight) {
        float y = (float) snowflake.get("y") + (float) snowflake.get("speedY");
        float opacity = (float) snowflake.get("opacity");

        if (y > screenHeight || opacity <= 0) {
            snowflake.clear();
            snowflake.putAll(createSnowflake(screenWidth, screenHeight));
        } else {
            snowflake.put("y", y);
            snowflake.put("rotationAngle", (float) snowflake.get("rotationAngle") + (float) snowflake.get("rotationSpeed"));
            snowflake.put("opacity", Math.max(0, opacity - OPACITY_DECAY));
        }
    }

    public void renderSnowflakes(DrawContext context, int screenWidth, int screenHeight) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        for (Map<String, Object> snowflake : snowflakes) {
            updateSnowflake(snowflake, screenWidth, screenHeight);
            drawSnowflake(snowflake, context);
        }
        matrices.pop();
    }

    private void drawSnowflake(Map<String, Object> snowflake, DrawContext context) {
        int alpha = (int) ((float) snowflake.get("opacity") * 0xFF);
        int size = (int) snowflake.get("size");
        int color = (alpha << 24) | 0xFFFFFF;
        float centerX = (float) snowflake.get("x") + size / 2f;
        float centerY = (float) snowflake.get("y") + size / 2f;
        float rotationAngle = (float) snowflake.get("rotationAngle");

        // Draw a more complex snowflake shape
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(rotationAngle + (i * 60));
            float armLength = size / 2f;
            float endX = centerX + (float) (armLength * Math.cos(angle));
            float endY = centerY + (float) (armLength * Math.sin(angle));
            context.fill((int) centerX, (int) centerY, (int) endX, (int) endY, color);

            // Add smaller branches for a more realistic snowflake
            float branchLength = armLength / 2f;
            float branchX = centerX + (float) (branchLength * Math.cos(angle + Math.toRadians(30)));
            float branchY = centerY + (float) (branchLength * Math.sin(angle + Math.toRadians(30)));
            context.fill((int) centerX, (int) centerY, (int) branchX, (int) branchY, color);

            branchX = centerX + (float) (branchLength * Math.cos(angle - Math.toRadians(30)));
            branchY = centerY + (float) (branchLength * Math.sin(angle - Math.toRadians(30)));
            context.fill((int) centerX, (int) centerY, (int) branchX, (int) branchY, color);
        }
    }

    private int getScreenHeight() {
        return net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight();
    }
}