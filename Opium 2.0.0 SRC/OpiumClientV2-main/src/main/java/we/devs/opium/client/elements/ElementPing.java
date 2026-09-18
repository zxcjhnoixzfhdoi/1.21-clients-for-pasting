package we.devs.opium.client.elements;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;

@RegisterElement(name = "Ping", tag = "Ping", description = "Shows your ping")
public class ElementPing extends Element {
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

    String getText() {
        return "Ping: " + getPing();
    }

    private int getPing() {
        int ping;
        if (mc.player == null || mc.getNetworkHandler() == null || mc.getNetworkHandler().getPlayerListEntry(mc.player.getGameProfile().getName()) == null) {
            ping = -1;
        } else {
            PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(mc.player.getGameProfile().getName());
            ping = playerInfo != null ? playerInfo.getLatency() : -1;
        }
        return ping;
    }
}
