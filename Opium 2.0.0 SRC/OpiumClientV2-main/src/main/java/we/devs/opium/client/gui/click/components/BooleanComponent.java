package we.devs.opium.client.gui.click.components;

import me.x150.renderer.render.Renderer2d;
import we.devs.opium.Opium;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueBoolean;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class BooleanComponent extends Component {
    private final ValueBoolean value;
    private float animationProgress = 0.0f; // For smooth toggle animation
    private static final int TOGGLE_WIDTH = 20; // Width of the toggle switch
    private static final int TOGGLE_HEIGHT = 10; // Height of the toggle switch
    private static final int RADIUS = 5; // Corner radius for rounded rectangles
    private static final int SAMPLES = 20; // Smoothness of rounded corners

    public BooleanComponent(ValueBoolean value, int offset, Frame parent) {
        super(offset, parent);
        this.value = value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        // Smooth animation for the toggle switch
        animationProgress += (value.getValue() ? 0.1f : -0.1f) * partialTicks;
        animationProgress = Math.max(0, Math.min(1, animationProgress));

        // Draw the toggle switch background
        Renderer2d.renderRoundedQuad(
                context.getMatrices(),
                Color.DARK_GRAY, // Background color
                this.getX() + this.getWidth() - TOGGLE_WIDTH - 2, this.getY() + 3,
                this.getX() + this.getWidth() - 2, this.getY() + 3 + TOGGLE_HEIGHT,
                RADIUS, RADIUS, RADIUS, RADIUS,
                SAMPLES
        );

        // Draw the toggle switch handle (animated)
        int handleX = (int) (this.getX() + this.getWidth() - TOGGLE_WIDTH - 2 + animationProgress * (TOGGLE_WIDTH - TOGGLE_HEIGHT));
        Renderer2d.renderRoundedQuad(
                context.getMatrices(),
                ModuleColor.getColor(), // Handle color
                handleX, this.getY() + 3,
                handleX + TOGGLE_HEIGHT, this.getY() + 3 + TOGGLE_HEIGHT,
                RADIUS, RADIUS, RADIUS, RADIUS,
                SAMPLES
        );

        // Draw the text label
        RenderUtils.drawString(context.getMatrices(), this.value.getTag(), this.getX() + 3, this.getY() + 2, -1);

        // Hover effect
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtils.drawRect(context.getMatrices(), this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 14, ModuleColor.getColor(10));
        }
    }

    @Override
    public void update(int mouseX, int mouseY, float partialTicks) {
        super.update(mouseX, mouseY, partialTicks);
        if (this.value.getParent() != null) {
            this.setVisible(this.value.getParent().isOpen());
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovering(mouseX, mouseY) && mouseButton == 0) {
            this.value.setValue(!this.value.getValue());
        }
    }

    public ValueBoolean getValue() {
        return this.value;
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + 14;
    }
}