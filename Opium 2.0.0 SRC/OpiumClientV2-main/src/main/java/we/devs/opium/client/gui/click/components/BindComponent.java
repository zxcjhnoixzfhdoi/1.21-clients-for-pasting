package we.devs.opium.client.gui.click.components;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventKey;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.values.impl.ValueBind;

public class BindComponent extends Component implements EventListener {
    private final ValueBind value;
    private boolean binding;

    public BindComponent(ValueBind value, int offset, Frame parent) {
        super(offset, parent);
        Opium.EVENT_MANAGER.register(this);
        this.value = value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        String keyName = this.value.getValue() == 0 ? "NONE" : GLFW.glfwGetKeyName(this.value.getValue(), 0);
        RenderUtils.drawString(context.getMatrices(),
                this.value.getTag() + " " + Formatting.GRAY + (this.binding ? "..." : keyName),
                this.getX() + 3,
                this.getY() + 3,
                -1
        );
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
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight()) {
            this.binding = !this.binding;
        }
    }

    @Override
    public void onKey(EventKey event) {
        if (binding) {
            if (event.getKeyCode() == GLFW.GLFW_KEY_DELETE || event.getKeyCode() == GLFW.GLFW_KEY_BACKSPACE) {
                this.value.setValue(0);
            } else if (event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) {
                this.value.setValue(event.getKeyCode());
            }
            this.binding = false;
        }
    }
}