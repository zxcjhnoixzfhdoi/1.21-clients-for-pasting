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

@RegisterElement(name = "Speed", tag = "Speed", description = "Shows your speed")
public class ElementSpeed extends Element {
    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);

        String text = getText();

        if(ModuleFont.INSTANCE.customFonts.getValue()) {
            this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(text));
            this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(text));
        } else {
            this.frame.setWidth(mc.textRenderer.getWidth(text));
            this.frame.setHeight(mc.textRenderer.fontHeight);
        }

        RenderUtils.drawString(new MatrixStack(), text, (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    String getText() {
        DecimalFormat df = new DecimalFormat("#.#");

        // Calculate movement distance per tick
        assert mc.player != null;
        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;

        // Compute speed in blocks per tick, then convert to blocks per second (20 ticks per second)
        double speedBlocksPerSecond = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ)) * 20;

        return "Speed: " + df.format(speedBlocksPerSecond) + " Blocks/s";
    }
}
