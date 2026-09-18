package we.devs.opium.client.elements;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;

import java.text.DecimalFormat;

@RegisterElement(name = "Server brand", tag = "Server brand", description = "Shows the server brand")
public class ElementServerBrand extends Element {
    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);
        if(RenderUtils.getFontRenderer() == null) return;
        if(ModuleFont.INSTANCE.customFonts.getValue()) {
            this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(getServerBrand()));
            this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(getServerBrand()));
        } else {
            this.frame.setWidth(mc.textRenderer.getWidth(this.getServerBrand()));
            this.frame.setHeight(mc.textRenderer.fontHeight);
        }
        RenderUtils.drawString(new MatrixStack(),this.getServerBrand(), (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    private String getServerBrand() {
        String serverBrand;
        ServerInfo serverData = mc.getCurrentServerEntry();
        if (serverData == null) {
            serverBrand = "Vanilla";
        } else {
            serverBrand = mc.player.networkHandler.getBrand();
            if (serverBrand == null) {
                serverBrand = "Vanilla";
            }
        }
        return serverBrand;
    }
}
