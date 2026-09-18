package we.devs.opium.client.elements;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;

import java.text.DecimalFormat;

@RegisterElement(name = "Direction", tag = "Direction", description = "Shows your direction")
public class ElementDirection extends Element {
    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);
        if(ModuleFont.INSTANCE.customFonts.getValue()) {
            this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(getText()));
            this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(getText()));
        } else {
            this.frame.setWidth(mc.textRenderer.getWidth(this.getText()));
            this.frame.setHeight(mc.textRenderer.fontHeight);
        }
        RenderUtils.drawString(new MatrixStack(),this.getText(), (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    private String getFacing(String input) {
        return switch (input.toLowerCase()) {
            case "north" -> "-Z";
            case "east" -> "+X";
            case "south" -> "+Z";
            default -> "-X";
        };
    }

    String getText() {
        return "Facing: " + this.getFacing(mc.player.getHorizontalFacing().getName());
    }

}
