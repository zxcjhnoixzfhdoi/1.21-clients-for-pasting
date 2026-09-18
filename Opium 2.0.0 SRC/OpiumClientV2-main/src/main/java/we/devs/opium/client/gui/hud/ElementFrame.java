package we.devs.opium.client.gui.hud;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.utilities.IMinecraft;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ElementFrame implements IMinecraft {
    private final Element element; // Associated element
    private float x, y; // Position of the frame
    private float width, height; // Size of the frame
    private float dragX, dragY; // Drag offsets for smooth dragging
    private boolean dragging; // Whether the frame is being dragged
    private boolean visible; // Visibility of the frame
    private HudEditorScreen parent; // Parent screen reference

    public Color color = Color.BLACK; // Frame color

    public ElementFrame(Element element, float x, float y, float width, float height, HudEditorScreen parent) {
        this.element = element;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.parent = parent;
        this.dragging = false;
    }

    // Render method with detailed debug logging to check bounds
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        if (this.element != null && Opium.ELEMENT_MANAGER.isElementEnabled(this.element.getName()) && mc.getWindow() != null) {
            // If dragging, update position based on mouse movement
            if (this.dragging) {
                this.x = this.dragX + (float) mouseX;
                this.y = this.dragY + (float) mouseY;
                this.x = Math.max(0, Math.min(mc.getWindow().getScaledWidth() - this.width, this.x));
                this.y = Math.max(0, Math.min(mc.getWindow().getScaledHeight() - this.height, this.y));
            }

            // Render the frame with background color
            Color bgColor = this.dragging ? new Color(100, 100, 100, 100) : new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
            RenderUtils.drawRect(context.getMatrices(), this.x, this.y, this.x + this.width, this.y + this.height, bgColor);

            // Render the element content
            this.element.onRender2D(new EventRender2D(partialTicks, context));
        }
    }

    // Handle click to start dragging the frame
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && this.isHovering(mouseX, mouseY)) {
            this.dragX = this.x - mouseX;
            this.dragY = this.y - mouseY;
            this.dragging = true;
        }
    }

    // Handle mouse release to stop dragging
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.dragging = false;
    }

    // Check if the mouse is hovering over the frame
    public boolean isHovering(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    // Implement the mouseDragged method
    public void mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging) {
            // Update position when dragging
            this.x += deltaX;
            this.y += deltaY;
        }
    }

    // Getters and setters for element properties
    public Element getElement() { return this.element; }
    public HudEditorScreen getParent() { return this.parent; }
    public float getX() { return this.x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return this.y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return this.width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return this.height; }
    public void setHeight(float height) { this.height = height; }
    public boolean isDragging() { return this.dragging; }
    public void setDragging(boolean dragging) { this.dragging = dragging; }
    public boolean isVisible() { return this.visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
