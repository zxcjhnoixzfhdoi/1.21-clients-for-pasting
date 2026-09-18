package we.devs.opium.client.modules.client;

import org.lwjgl.glfw.GLFW;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueNumber;
import we.devs.opium.api.manager.module.Module;

import java.awt.*;

@RegisterModule(name="GUI", description="The client's GUI interface for interacting with modules and settings.", category=Module.Category.CLIENT, bind= GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ModuleGUI extends Module {
    public static ModuleGUI INSTANCE;
    public ValueBoolean snow = new ValueBoolean("Snow", "Snow", "Let It Snow (wierdly broken)", false);
    public final ValueColor categoryColor = new ValueColor("CategoryColor", "Category Color", "Color of the category panes.", new Color(29, 29, 29,255));
    public final ValueColor categoryTitleColor = new ValueColor("CategoryTitleColor", "Category Title Color", "Color of the category title.", ModuleColor.getColor());
    public ValueBoolean roundedCorners = new ValueBoolean("RoundedCorners", "Rounded Categories", "Make the category panes rounded.", true);
    public ValueBoolean roundedModules = new ValueBoolean("RoundedModules", "Rounded Modules", "Make the modules rounded.", true);
    public ValueNumber cornerRadius = new ValueNumber("cornerRadius", "Category Radius", "The radius of the rounded category corners", 4, 1, 20);
    public ValueNumber moduleRadius = new ValueNumber("moduleRadius", "Module Radius", "The radius of the rounded module corners", 2, 1, 20);
    public ValueNumber hoverAlpha = new ValueNumber("hoverAlpha", "Hover Alpha", "The alpha of the module hover.", 25, 0, 255);

    public ValueBoolean displayKeybinds = new ValueBoolean("DisplayKeybinds", "Display Keybinds", "Display keybinds on the module", true);
    public ValueBoolean rectEnabled = new ValueBoolean("RectEnabled", "Rect Enabled", "Render a rectangle behind enabled modules.", true);
    public ValueNumber scrollSpeed = new ValueNumber("ScrollSpeed", "Scroll Speed", "The speed for scrolling through the GUI.", 10, 1, 50);
    public ValueBoolean fadeText = new ValueBoolean("FadeText", "Fade Text", "Add cool animation to the text of the GUI.", false);
    public ValueNumber fadeOffset = new ValueNumber("FadeOffset", "Fade Offset", "Offset for the text animation of the GUI.", 100, 0, 255);

    public ModuleGUI() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || Opium.CLICK_GUI == null) {
            this.disable(false);
            return;
        }
        if (mc.currentScreen != Opium.CLICK_GUI) {
            mc.setScreen(Opium.CLICK_GUI);
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen == Opium.CLICK_GUI) {
            mc.setScreen(null);
        }
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != Opium.CLICK_GUI) {
            this.disable(true);
        }
    }
}
