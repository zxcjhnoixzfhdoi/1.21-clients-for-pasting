package we.devs.opium.client.gui.click.components;

import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.values.impl.ValueCategory;
import net.minecraft.client.gui.DrawContext;

public class CategoryComponent extends Component {
    private final ValueCategory value;

    public CategoryComponent(ValueCategory value, int offset, Frame parent) {
        super(offset, parent);
        this.value = value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawString(new MatrixStack(), this.value.getName(), this.getX() + 3, this.getY() + 3, -1);
        RenderUtils.drawString(new MatrixStack(), this.value.isOpen() ? "-" : "+", (int) ((float)(this.getX() + this.getWidth() - 3) - mc.textRenderer.getWidth(this.value.isOpen() ? "+" : "-")), this.getY() + 3, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovering(mouseX, mouseY) && mouseButton == 1) {
            this.value.setOpen(!this.value.isOpen());
            this.getParent().refresh();
        }
    }
}