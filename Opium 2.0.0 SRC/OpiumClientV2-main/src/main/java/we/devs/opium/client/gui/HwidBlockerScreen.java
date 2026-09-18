package we.devs.opium.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import we.devs.opium.Opium;
import we.devs.opium.api.utilities.HWIDValidator;

import java.util.ArrayList;
import java.util.List;

public class HwidBlockerScreen extends Screen {

    private ButtonWidget closeGameButton;
    private float textOpacity = 1.0f; // For text fading
    private boolean fadeOut = true; // Controls fade direction
    private final List<Particle> particles = new ArrayList<>(); // Particle list
    private String toastMessage = ""; // Current toast message
    private float toastTimer = 0; // Timer for toast visibility

    public HwidBlockerScreen() {
        super(Text.literal("Access Denied"));
    }

    @Override
    protected void init() {
        super.init();

        // Initialize particles
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(this.width, this.height));
        }

        // Add "Close Game" button
        closeGameButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Close Game"), button -> MinecraftClient.getInstance().stop()).dimensions(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render solid black background
        context.fill(0, 0, this.width, this.height, 0xFF000000);

        // Render particles and fading text
        renderParticles(context, delta);  // Render particles
        super.render(context, mouseX, mouseY, delta); // Render buttons
        updateTextOpacity(delta); // Update text fading
        renderTextOnTop(context); // Render fading text

        // Render toast notifications
        renderToast(context, delta);

        // Button hover effect
        if (closeGameButton.isMouseOver(mouseX, mouseY)) {
            closeGameButton.setAlpha(0.7f); // Add transparency when hovered
        } else {
            closeGameButton.setAlpha(1.0f);
        }
    }

    private void renderParticles(DrawContext context, float delta) {
        for (Particle particle : particles) {
            particle.update(delta);
            particle.render(context);
        }
    }

    private void updateTextOpacity(float delta) {
        // Update text opacity for fading effect
        if (fadeOut) {
            textOpacity -= delta * 0.01f;
            if (textOpacity <= 0.2f) fadeOut = false;
        } else {
            textOpacity += delta * 0.01f;
            if (textOpacity >= 1.0f) fadeOut = true;
        }
    }

    private void renderTextOnTop(DrawContext context) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int alpha = (int) (textOpacity * 255) << 24; // Convert opacity to ARGB

        context.drawTextWithShadow(
                textRenderer,
                "Looks Like You're Not Opium!",
                this.width / 2 - textRenderer.getWidth("Looks Like You're Not Opium!") / 2,
                this.height / 2 - 20,
                0xFF5555 | alpha
        );
        context.drawTextWithShadow(
                textRenderer,
                "You are not permitted to use this Client.",
                this.width / 2 - textRenderer.getWidth("You are not permitted to use this Client.") / 2,
                this.height / 2,
                0xFFFFFF | alpha
        );
        context.drawTextWithShadow(
                textRenderer,
                "Please Message The Opium Devs",
                this.width / 2 - textRenderer.getWidth("Please Message The Opium Devs") / 2,
                this.height / 2 + 20,
                0xFFFFFF | alpha
        );
    }

    private void renderToast(DrawContext context, float delta) {
        if (toastTimer > 0) {
            // Decrease the timer
            toastTimer -= delta;

            // Draw the toast message at the top center of the screen
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            int toastWidth = textRenderer.getWidth(toastMessage);
            int toastHeight = 10;

            int x = (this.width - toastWidth) / 2;
            int y = 10;

            // Draw a semi-transparent background for the toast
            context.fill(x - 5, y - 5, x + toastWidth + 5, y + toastHeight + 5, 0x88000000);

            // Draw the toast message
            context.drawTextWithShadow(
                    textRenderer,
                    toastMessage,
                    x,
                    y,
                    0xFFFFFF
            );
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
        //return super.shouldCloseOnEsc();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Set a toast message when a key is pressed
        this.toastMessage = "Inputs Have Been Blocked!";
        this.toastTimer = 20;

        return true; // Block all keyboard inputs
        //return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Allow mouse interactions only if the button is clicked
        if (closeGameButton != null && closeGameButton.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button); // Allow button interaction
        }
        // Block all other mouse clicks
        return true;
    }

    @Override
    public void removed() {
         // Call the superclass method to handle default behavior
        HWIDValidator.valid = false;
        Opium.LOGGER.warn("[HWIDValidator] Screen Closed");
        super.removed();

    }

    private static class Particle {
        private float x;
        private float y;
        private final float size;
        private final float speedX;
        private final float speedY;
        private final int color;

        public Particle(int width, int height) {
            this.x = (float) Math.random() * width;
            this.y = (float) Math.random() * height;
            this.size = (float) (Math.random() * 2 + 1);
            this.speedX = (float) (Math.random() * 2 - 1);
            this.speedY = (float) (Math.random() * 2 - 1);
            this.color = 0xFFFFFF | (int) (Math.random() * 0xFF) << 24; // Random color with transparency
        }

        public void update(float delta) {
            x += speedX * delta;
            y += speedY * delta;
        }

        public void render(DrawContext context) {
            context.fill((int) x, (int) y, (int) (x + size), (int) (y + size), color);
        }
    }
}
