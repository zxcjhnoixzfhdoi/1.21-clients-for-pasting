package we.devs.opium.client.gui.click.manage;

import me.x150.renderer.render.Renderer2d;
import net.minecraft.util.Identifier;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.IMinecraft;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.api.utilities.SnowflakeRenderer;
import we.devs.opium.client.gui.click.components.ColorComponentTest;
import we.devs.opium.client.gui.click.components.ModuleComponent;
import net.minecraft.client.gui.DrawContext;
import we.devs.opium.client.gui.click.components.StringComponent;
import we.devs.opium.client.modules.client.ModuleGUI;
import we.devs.opium.client.modules.client.ModuleOutline;

import java.util.ArrayList;

public class Frame implements IMinecraft {
    private final ArrayList<Component> components;
    private final String tab;
    private Identifier categoryIcon;
    private int x;
    private int y;
    private int height;
    private final int width;
    private boolean open = true;
    private boolean dragging;
    private int dragX;
    private int dragY;

    private final SnowflakeRenderer snowflakeRenderer = new SnowflakeRenderer();
    private boolean snowflakesInitialized = false;

    public Frame(Module.Category category, int x, int y) {
        this.tab = category.getName();
        this.x = x;
        this.y = y;
        this.width = 100;
        this.dragging = false;
        this.dragX = 0;
        this.dragY = 0;
        this.components = new ArrayList<>();
        int offset = 16;
        for (Module module : Opium.MODULE_MANAGER.getModules(category)) {
            this.components.add(new ModuleComponent(module, offset, this));
            offset += 16;
        }
        this.height = offset;
        this.refresh();
    }

    public Frame(int x, int y) {
        this.tab = "HUD";
        this.x = x;
        this.y = y;
        this.width = 100;
        this.dragging = false;
        this.dragX = 0;
        this.dragY = 0;
        this.components = new ArrayList<>();
        int offset = 16;
        for (Element element : Opium.ELEMENT_MANAGER.getElements()) {
            this.components.add(new ModuleComponent(element, offset, this));
            offset += 16;
        }
        this.height = offset;
        this.refresh();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.refresh();
        if (this.isDragging()) {
            this.setX(mouseX - this.dragX);
            this.setY(mouseY - this.dragY);
        }

        int radius = (int) ModuleGUI.INSTANCE.cornerRadius.getValue();
        int samples = 20;
        float outlineW = 0.7f;

        if (ModuleGUI.INSTANCE.snow.getValue()) {
            int screenWidth = IMinecraft.mc.getWindow().getScaledWidth();
            int screenHeight = IMinecraft.mc.getWindow().getScaledHeight();

            if (!snowflakesInitialized) {
                snowflakeRenderer.initializeSnowflakes(screenWidth, screenHeight);
                snowflakesInitialized = true;
            }

            // Save the graphics context state
            context.getMatrices().push();

            // Render snowflakes
            snowflakeRenderer.renderSnowflakes(context, screenWidth, screenHeight);

            // Restore the graphics context state
            context.getMatrices().pop();
        }


        if (ModuleGUI.INSTANCE.roundedCorners.getValue()) {
            Renderer2d.renderRoundedQuad(
                    context.getMatrices(),
                    ModuleGUI.INSTANCE.categoryTitleColor.getValue(),
                    this.getX() - 2, this.getY() - 3,
                    this.getX() + this.getWidth() + 2, this.getY() + 13,
                    radius, radius, 0, 0,
                    samples
            );
            if (ModuleOutline.INSTANCE.categoryTitleOutline.getValue()) {
                //Renderer2d.renderRoundedOutline(context.getMatrices(), ModuleOutline.INSTANCE.categoryTitleOutlineColor.getValue(), this.getX() - 2 + outlineW, this.getY() - 3 + outlineW, this.getX() + this.getWidth() + 2 - outlineW, this.getY() + 13 - outlineW, radius, radius, 0, 0, outlineW, samples * 4);
                RenderUtils.drawOutline(context.getMatrices(),this.getX() - 2 + outlineW, this.getY() - 3 + outlineW, this.getX() + this.getWidth() + 2 - outlineW, this.getY() + 13 - outlineW,outlineW,radius, ModuleOutline.INSTANCE.categoryTitleOutlineColor.getValue());
            }
        } else {
            RenderUtils.drawRect(context.getMatrices(), this.getX() - 2, this.getY() - 3, this.getX() + this.getWidth() + 2, this.getY() + 13, ModuleGUI.INSTANCE.categoryTitleColor.getValue());
            if (ModuleOutline.INSTANCE.categoryTitleOutline.getValue()) {
                Renderer2d.renderRoundedOutline(context.getMatrices(), ModuleOutline.INSTANCE.categoryTitleOutlineColor.getValue(), this.getX() - 2 + outlineW, this.getY() - 3 + outlineW, this.getX() + this.getWidth() + 2 - outlineW, this.getY() + 13 - outlineW, 0, 0, 0, 0, outlineW, samples * 4);
            }
        }

        if (this.isOpen()) {
            if (ModuleGUI.INSTANCE.roundedCorners.getValue()) {
                Renderer2d.renderRoundedQuad(
                        context.getMatrices(),
                        Opium.CLICK_GUI.getCategoryColor(),
                        this.getX() - 2, this.getY() + 13,
                        this.getX() + this.getWidth() + 2, this.getY() + this.getHeight() + 0.5f,
                        0, 0, radius, radius,
                        samples
                );
                if (ModuleOutline.INSTANCE.categoryOutline.getValue()) {
                    Renderer2d.renderRoundedOutline(context.getMatrices(), ModuleOutline.INSTANCE.categoryOutlineColor.getValue(), this.getX() - 2 + outlineW, this.getY() + 13 + outlineW, this.getX() + this.getWidth() + 2 - outlineW, this.getY() + this.getHeight() + 1f - outlineW, 0, 0, radius, radius, outlineW, samples * 4);
                }
            } else {
                RenderUtils.drawRect(context.getMatrices(), this.getX() - 2, this.getY() + 13, this.getX() + this.getWidth() + 2, this.getY() + this.getHeight(), Opium.CLICK_GUI.getCategoryColor());
                if (ModuleOutline.INSTANCE.categoryOutline.getValue()) {
                    Renderer2d.renderRoundedOutline(context.getMatrices(), ModuleOutline.INSTANCE.categoryOutlineColor.getValue(), this.getX() - 2 + outlineW, this.getY() + 13 + outlineW, this.getX() + this.getWidth() + 2 - outlineW, this.getY() + this.getHeight() + 1f - outlineW, 0, 0, 0, 0, outlineW, samples * 4);
                }
            }
        }

        //ss

        RenderUtils.drawString(context.getMatrices(), this.tab, this.x + 3, this.y + 1, -1);

        if (this.isOpen()) {
            for (Component component : this.components) {
                if (!component.isVisible()) continue;
                component.render(context, mouseX, mouseY, partialTicks);
            }
        }
    }


public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseX >= this.getX() - 4 && mouseX <= this.getX() + this.getWidth() + 4 && mouseY >= this.getY() - 3 && mouseY <= this.getY() + 13) {
            if (mouseButton == 0) {
                this.setDragging(true);
                this.dragX = mouseX - this.getX();
                this.dragY = mouseY - this.getY();
            }
            if (mouseButton == 1) {
                boolean bl = this.open = !this.open;
            }
        }
        if (this.isOpen()) {
            for (Component component : this.components) {
                component.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.setDragging(false);
        for (Component component : this.components) {
            component.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void charTyped(char typedChar, int keyCode) {
        if (this.isOpen()) {
            for (Component component : this.components) {
                if (!component.isVisible()) continue;
                component.charTyped(typedChar, keyCode);
            }
        }
    }

    public void refresh() {
        int offset = 16;
        for (Component component : this.components) {
            ModuleComponent moduleComponent;
            if (!component.isVisible()) continue;
            component.setOffset(offset);
            offset += 16;
            if (!(component instanceof ModuleComponent) || (moduleComponent = (ModuleComponent)component).getModule().getValues().isEmpty() || !moduleComponent.isOpen()) continue;
            for (Component valueComponent : moduleComponent.getComponents()) {
                if (!valueComponent.isVisible()) continue;
                valueComponent.setOffset(offset);
                offset += valueComponent instanceof ColorComponentTest && ((ColorComponentTest)valueComponent).isOpen() ? 190 : 15;
            }
        }
        this.setHeight(offset);
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public boolean isOpen() {
        return this.open;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ArrayList<Component> getComponents() {
        return this.components;
    }
}
