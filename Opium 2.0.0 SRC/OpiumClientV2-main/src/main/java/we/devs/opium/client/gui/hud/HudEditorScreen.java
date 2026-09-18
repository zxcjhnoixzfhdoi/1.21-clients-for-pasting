package we.devs.opium.client.gui.hud;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.manage.Frame;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class HudEditorScreen extends Screen {
    private final ArrayList<ElementFrame> elementFrames = new ArrayList<>();
    private final Frame frame = new Frame(20, 20);
    private ElementFrame draggingElement = null; // Track the element being dragged

    public HudEditorScreen() {
        super(Text.literal(""));
        for (Element element : Opium.ELEMENT_MANAGER.getElements()) {
            this.addElement(element);
            element.setFrame(this.getFrame(element));
        }
    }

    public void addElement(Element element) {
        this.elementFrames.add(new ElementFrame(element, 10.0f, 10.0f, 80.0f, 15.0f, this));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        this.frame.render(context, mouseX, mouseY, partialTicks);
        for (ElementFrame frame : this.elementFrames) {
            frame.render(context, mouseX, mouseY, partialTicks);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If a new element is clicked, start dragging it and stop dragging the previous one
        this.frame.mouseClicked((int) mouseX, (int) mouseY, button);
        for (ElementFrame frame : this.elementFrames) {
            if (frame.isHovering(mouseX, mouseY)) {
                if (draggingElement != null) {
                    draggingElement.mouseReleased((int) mouseX, (int) mouseY, button); // Stop dragging the previous element
                }
                draggingElement = frame; // Start dragging the clicked element
                frame.mouseClicked((int) mouseX, (int) mouseY, button);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        // Stop dragging the current element
        this.frame.mouseReleased((int) mouseX, (int) mouseY, state);
        if (draggingElement != null) {
            draggingElement.mouseReleased((int) mouseX, (int) mouseY, state);
            draggingElement = null; // Reset dragging state
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Only drag if an element is being dragged
        if (draggingElement != null) {
            draggingElement.mouseDragged((int) mouseX, (int) mouseY, button, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    // Getter for element frames
    public ElementFrame getFrame(Element element) {
        for (ElementFrame ef : this.elementFrames) {
            if (ef.getElement().equals(element)) {
                return ef;
            }
        }
        return null;
    }
}
