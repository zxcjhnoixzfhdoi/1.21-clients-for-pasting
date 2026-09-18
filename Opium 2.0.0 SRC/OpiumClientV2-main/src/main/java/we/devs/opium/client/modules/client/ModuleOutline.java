package we.devs.opium.client.modules.client;

import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.api.manager.module.Module;

import java.awt.*;
@RegisterModule(name="Outline", description="Manages the client's outlines", category=Module.Category.CLIENT, persistent=true)
public class ModuleOutline extends Module {
    public static ModuleOutline INSTANCE;
    public ValueBoolean categoryOutline = new ValueBoolean("CategoryOutline", "Category Outline", "Render an outline on the categories", true);
    public ValueBoolean categoryTitleOutline = new ValueBoolean("CategoryTitleOutline", "Category Title Outline", "Render an outline on the category title box", false);
    public ValueBoolean moduleOutline = new ValueBoolean("ModuleOutline", "Module Outline", "Render an outline on the modules..", false);
    public final ValueColor categoryOutlineColor = new ValueColor("CategoryOutlineColor", "Category Outline Color", "Color of the category outline.", ModuleColor.getColor());
    public ValueColor categoryTitleOutlineColor = new ValueColor("CategoryTitleOutline", "Category Title Outline Color", "Color of the category title box outline.", ModuleColor.getColor());
    public final ValueColor moduleOutlineColor = new ValueColor("ModuleOutlineColor", "Module Outline Color", "Color of the module outline", new Color(255, 255, 255,255));
    public ModuleOutline() {
        INSTANCE = this;
    }
}