package we.devs.opium.client.modules.client;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;
import java.awt.*;

@RegisterModule(name="Font", description="Manages the client's global font.", category= Module.Category.CLIENT, persistent=true)
public class ModuleFont extends Module {

    public static ModuleFont INSTANCE;

    public ValueBoolean customFonts = new ValueBoolean("CustomFont", "Custom Font", "Render a custom font.", true);
    public ValueBoolean textShadows = new ValueBoolean("TextShadows", "Text Shadows", "Render a text shadow (currently doesn't work with custom fonts!)", true);
    public ValueBoolean customChatFont = new ValueBoolean("CustomChatFont", "Custom Chat Font", "Use a custom font to render chat messages", false);

    public ModuleFont() {
        INSTANCE = this;
    }
}