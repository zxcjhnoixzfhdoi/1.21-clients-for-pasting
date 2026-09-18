package we.devs.opium.client.elements;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;
import we.devs.opium.client.values.impl.ValueBoolean;

import java.text.DecimalFormat;

@RegisterElement(name = "Coords", tag = "Coords", description = "Shows your coordinates")
public class ElementCoords extends Element {

    ValueBoolean netherCoords = new ValueBoolean("NetherCoords", "Nether Coords", "", true);

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

    DecimalFormat format = new DecimalFormat("#.#");
    String getText() {String coordsText = "XYZ" + " " + this.format.format(mc.player.getX()) + ", " + this.format.format(mc.player.getY()) + ", " + this.format.format(mc.player.getZ()) +
            (this.netherCoords.getValue() ? " [" + this.format.format(mc.player.getWorld().getRegistryKey().getValue().equals("minecraft:the_nether") ? mc.player.getX() * 8.0 : mc.player.getX() / 8.0) + ", " + this.format.format(mc.player.getWorld().getRegistryKey().getValue().equals("minecraft:the_nether") ? mc.player.getZ() * 8.0 : mc.player.getZ() / 8.0) + "]" : "");

        return "" + coordsText;
    }

}
