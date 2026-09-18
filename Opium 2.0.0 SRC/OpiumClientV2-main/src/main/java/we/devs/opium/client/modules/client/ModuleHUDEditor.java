package we.devs.opium.client.modules.client;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import net.minecraft.util.Formatting;
import we.devs.opium.client.gui.hud.ElementFrame;
import we.devs.opium.client.gui.hud.HudEditorScreen;

@RegisterModule(name="HUDEditor", tag="HUD Editor", description="The client's HUD Editor.", category=Module.Category.CLIENT)
public class ModuleHUDEditor extends Module {
    public static ModuleHUDEditor INSTANCE;

    public ModuleHUDEditor() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            return;
        }
        mc.setScreen(Opium.HUD_EDITOR);
        this.disable(false);
    }

    public Formatting getSecondColor() {
        return Formatting.WHITE;
    }

    public enum secondColors {
        Normal,
        Gray,
        DarkGray,
        White
    }
}
