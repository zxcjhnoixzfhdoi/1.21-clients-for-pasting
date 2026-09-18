package we.devs.opium.client.elements;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.gui.hud.HudEditorScreen;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;
import we.devs.opium.client.values.impl.ValueCategory;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueString;

@RegisterElement(name="Kill HUD", description="Show a message on kill")
public class ElementKillHud extends Element {


    static String[] list = {
        "LOL dumbass nn %s died to Opium Client",
        "fn %s explodes due to OpiumClient.vip",
        "pooron nn %s dies to the king client",
        "retarded ass kid %s dies to 0piumh4ck.cc",
            "allah just blessed me with this win on hvh",
            "my grandma can fight better than you",
            "nn dog pounded by opium",
            "better pray to allah next time for the win",
            "call for backup next time asshole"
    };

    int hold = 0;
    String last = "Player";
    String text = "";
    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);
        boolean show = false;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if(player == mc.player || Opium.FRIEND_MANAGER.isFriend(player.getGameProfile().getName())) continue;
            if(player.isDead()) {
                show = true;
                if(last != player.getGameProfile().getName()) text = getText().formatted(Formatting.BOLD + "" + Formatting.DARK_RED + last + Formatting.RESET + Formatting.RED);
                last = player.getGameProfile().getName();
            }
        }
        if(show || hold > 0) {
            if(ModuleFont.INSTANCE.customFonts.getValue()) {
                this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(text));
                this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(text));
            } else {
                this.frame.setWidth(mc.textRenderer.getWidth(this.getText()));
                this.frame.setHeight(mc.textRenderer.fontHeight);
            }
            RenderUtils.drawString(new MatrixStack(),text, (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
            if(hold <= 0) hold = 300;
            else hold--;
        } else if(mc.currentScreen instanceof HudEditorScreen) {
            if(ModuleFont.INSTANCE.customFonts.getValue()) {
                this.frame.setWidth(RenderUtils.getFontRenderer().getStringWidth(text));
                this.frame.setHeight(RenderUtils.getFontRenderer().getStringHeight(text));
            } else {
                this.frame.setWidth(mc.textRenderer.getWidth(this.getText()));
                this.frame.setHeight(mc.textRenderer.fontHeight);
            }
        } else {
            this.frame.setWidth(0);
            this.frame.setHeight(0);
        }
    }

    private String getText() {
        return Formatting.RED + list[(int) Math.floor(Math.random() * list.length)];
    }
}
