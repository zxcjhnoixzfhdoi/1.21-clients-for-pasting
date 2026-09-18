package we.devs.opium.client.elements;

import net.minecraft.util.Formatting;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.ColorUtils;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;
import we.devs.opium.client.values.impl.ValueEnum;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

@RegisterElement(name = "Arraylist", tag = "Arraylist", description = "Shows all enabled modules")
public class ElementArraylist extends Element {

    ArrayList<Module> modules = new ArrayList<>();
    ValueEnum ordering = new ValueEnum("Ordering", "Ordering", "", orderings.Length);
    ValueEnum rendering = new ValueEnum("Rendering", "Rendering", "", renderings.Up);
    ValueEnum modulesColor = new ValueEnum("ModulesColor", "Modules Color", "Color mode for array list.", modulesColors.Normal);

    Formatting reset = Formatting.RESET;
    Formatting gray = Formatting.GRAY;

    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        frame.color = new Color(0, 0, 0, 0);
        float sWidth = mc.getWindow().getScaledWidth();
        float sHeight = mc.getWindow().getScaledHeight();
        modules.clear();
        for (we.devs.opium.api.manager.module.Module module : Opium.MODULE_MANAGER.getModules()) {
            if (module.isToggled() && module.isDrawn()) {
                modules.add(module);
            }
        }
        if (!modules.isEmpty()) {
            int addY = 0;
            if (this.ordering.getValue().equals(orderings.Length)) {
                for (we.devs.opium.api.manager.module.Module m : modules.stream().sorted(Comparator.comparing(s -> mc.textRenderer.getWidth(s.getTag() + (s.getHudInfo().isEmpty() ? "" : this.gray + " [" + Formatting.WHITE + s.getHudInfo() + this.gray + "]")) * -1.0f)).toList()) {
                    String string = m.getTag() + (m.getHudInfo().isEmpty() ? "" : this.gray + " [" + Formatting.WHITE + m.getHudInfo() + this.gray + "]");
                    float x = frame.getX() + frame.getWidth() - 2 - getWidth(string);
                    float y = this.rendering.getValue().equals(renderings.Up) ? (this.frame.getY() + addY * 10) : (this.frame.getY() - addY * 10);
                    RenderUtils.drawString(event.getContext().getMatrices(), string, x, y, this.modulesColor.getValue().equals(modulesColors.Normal) ? ModuleColor.getColor().getRGB() : (this.modulesColor.getValue().equals(modulesColors.Random) ? m.getRandomColor().getRGB() : ColorUtils.rainbow(addY).getRGB()));
                    ++addY;
                }
            } else {
                for (we.devs.opium.api.manager.module.Module m : modules.stream().sorted(Comparator.comparing(Module::getName)).toList()) {
                    String string = m.getTag() + (m.getHudInfo().isEmpty() ? "" : this.gray + " [" + Formatting.WHITE + m.getHudInfo() + this.gray + "]");
                    float x = sWidth - 2.0f - getWidth(string);
                    float y = this.rendering.getValue().equals(renderings.Up) ? (float) (2 + addY * 10) : sHeight - 12.0f - (float) (addY * 10);
                    RenderUtils.drawString(event.getContext().getMatrices(), string, x, y, (int) (this.modulesColor.getValue().equals(modulesColors.Normal) ? ModuleColor.getColor().getRGB() : (this.modulesColor.getValue().equals(modulesColors.Random) ? m.getRandomColor() : ColorUtils.rainbow(addY)).getRGB()));
                    ++addY;
                }
            }
        }
    }

    float getWidth(String string) {
        if(ModuleFont.INSTANCE.customFonts.getValue()) return RenderUtils.getFontRenderer().getStringWidth(string);
        else return mc.textRenderer.getWidth(string);
    }

    public enum orderings {
        Length,
        ABC
    }

    public enum renderings {
        Up,
        Down
    }

    public enum modulesColors {
        Normal,
        Random,
        Rainbow
    }
}
