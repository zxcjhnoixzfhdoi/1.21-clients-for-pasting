package we.devs.opium.client.elements;

import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;

@RegisterElement(name = "FPS", tag = "FPS", description = "Shows your fps")
public class ElementFPS extends Element {
    @Override
    public void onRender2D(EventRender2D event) {
        super.onRender2D(event);
        if(RenderUtils.getFontRenderer() == null) return;
        if(ModuleFont.INSTANCE.customFonts.getValue()) {
            this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(getText()));
            this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(getText()));
        } else {
            this.frame.setWidth(mc.textRenderer.getWidth(this.getText()));
            this.frame.setHeight(mc.textRenderer.fontHeight);
        }
        RenderUtils.drawString(new MatrixStack(),this.getText(), (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    String getText() {
        return "FPS: " + mc.getCurrentFps();
    }
}
