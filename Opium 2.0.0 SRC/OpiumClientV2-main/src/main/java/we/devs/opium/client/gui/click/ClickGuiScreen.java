package we.devs.opium.client.gui.click;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.components.ModuleComponent;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.modules.client.ModuleColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import we.devs.opium.client.modules.client.ModuleGUI;

import java.awt.*;
import java.util.ArrayList;

public class ClickGuiScreen extends Screen implements EventListener {
    private final ArrayList<Frame> frames = new ArrayList<>();

    public ClickGuiScreen() {
        super(Text.literal("Click GUI"));
        Opium.EVENT_MANAGER.register(this);
        int offset = 30;
        for (Module.Category category : Module.Category.values()) {
            this.frames.add(new Frame(category, offset, 20));
            offset += 110;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 0.1);
        RenderUtils.setDrawContext(context);

        for (Frame frame : this.frames) {
            frame.render(context, mouseX, mouseY, delta);
        }

        String moduleDescription = null;

        for (Frame frame : this.frames) {
            for (Component c : frame.getComponents()) {
                if (c instanceof ModuleComponent component) {
                    if (component.isHovering(mouseX, mouseY) && frame.isOpen() && !component.getModule().getDescription().isEmpty()) {
                        moduleDescription = component.getModule().getDescription();
                    }
                }
            }
        }

        if (moduleDescription != null) {
            int windowWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int windowHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(moduleDescription);

            // Set the description position to bottom-left
            int x = 5; // Margin from the left
            int y = windowHeight - 15; // Margin from bottom

            RenderUtils.drawRect(context.getMatrices(), x - 2, y - 2, x + textWidth + 5, y + 10, new Color(40, 40, 40));
            RenderUtils.drawOutline(context.getMatrices(), x - 2, y - 2, x + textWidth + 5, y + 10, 1.0f, ModuleColor.getColor());

            RenderUtils.drawString(context.getMatrices(), moduleDescription, x, y, -1);
        }

        context.getMatrices().translate(0, 0, -0.1);
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //HWIDValidator.isHWIDValid(Opium.devEnv, false);
        for (Frame frame : this.frames) {
            frame.mouseClicked((int) mouseX, (int) mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        for (Frame frame : this.frames) {
            frame.mouseReleased((int) mouseX, (int) mouseY, state);
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        for (Frame frame : this.frames) {
            frame.charTyped(typedChar, keyCode);
        }
        return super.charTyped(typedChar, keyCode);
    }

    public Color getColor() {
        return new Color(ModuleColor.getColor().getRed(), ModuleColor.getColor().getGreen(), ModuleColor.getColor().getBlue(), ModuleColor.getColor().getAlpha());
    }
    public Color getCategoryColor() {
        return new Color(ModuleGUI.INSTANCE.categoryColor.getValue().getRed(), ModuleGUI.INSTANCE.categoryColor.getValue().getGreen(),ModuleGUI.INSTANCE.categoryColor.getValue().getBlue(), ModuleGUI.INSTANCE.categoryColor.getValue().getAlpha());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
