package we.devs.opium.client.gui.click.components;

import me.x150.renderer.render.Renderer2d;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import we.devs.opium.Opium;
import we.devs.opium.api.utilities.ColorUtils;
import we.devs.opium.api.utilities.Keys;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.api.utilities.font.FontRenderers;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleFont;
import we.devs.opium.client.modules.client.ModuleGUI;
import we.devs.opium.client.modules.client.ModuleOutline;
import we.devs.opium.client.values.Value;
import we.devs.opium.client.values.impl.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModuleComponent extends Component {
    private final ArrayList<Component> components;
    private final Module module;
    private boolean open = false;
    private float hoverAnimation = 0.0f; // For smooth hover animations
    public Map<Integer, Color> colorMap = new HashMap<>();

    public ModuleComponent(Module module, int offset, Frame parent) {
        super(offset, parent);
        this.module = module;
        this.components = new ArrayList<>();
        int valueOffset = offset;
        if (!module.getValues().isEmpty()) {
            for (Value value : module.getValues()) {
                if (value instanceof ValueBoolean) {
                    this.components.add(new BooleanComponent((ValueBoolean)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (value instanceof ValueNumber) {
                    this.components.add(new NumberComponent((ValueNumber)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (value instanceof ValueEnum) {
                    this.components.add(new EnumComponent((ValueEnum)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (value instanceof ValueString) {
                    this.components.add(new StringComponent((ValueString)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (value instanceof ValueColor) {
                    this.components.add(new ColorComponentTest((ValueColor)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (value instanceof ValueBind) {
                    this.components.add(new BindComponent((ValueBind)value, valueOffset, parent));
                    valueOffset += 14;
                }
                if (!(value instanceof ValueCategory)) continue;
                this.components.add(new CategoryComponent((ValueCategory)value, valueOffset, parent));
                valueOffset += 14;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int height = mc.getWindow().getScaledHeight();
        for (int i = 0; i <= height; ++i) {
            this.colorMap.put(i, ColorUtils.wave(Color.WHITE, ModuleGUI.INSTANCE.fadeOffset.getValue().intValue(), i * 2 + 10));
        }
        int radius = (int) ModuleGUI.INSTANCE.moduleRadius.getValue();
        int samples = 20;

        // Smooth hover animation
        hoverAnimation += (isHovering(mouseX, mouseY) ? 0.1f : -0.1f) * delta;
        hoverAnimation = Math.max(0, Math.min(1, hoverAnimation));

        // Check if hovering over the module title area
        boolean isHoveringTitle = this.isHovering(mouseX, mouseY);

        if (isHoveringTitle) {
            Color highlightColor = new Color(255, 255, 255, (int) (ModuleGUI.INSTANCE.hoverAlpha.getValue().floatValue() * hoverAnimation));

            if (ModuleGUI.INSTANCE.roundedModules.getValue()) {
                Renderer2d.renderRoundedQuad(
                        context.getMatrices(),
                        highlightColor,
                        (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                        this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                        radius, radius, radius, radius,
                        samples
                );
            } else {
                RenderUtils.drawRect(context.getMatrices(), (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                        this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                        highlightColor);
            }
        }

        if (this.module.isToggled() && ModuleGUI.INSTANCE.rectEnabled.getValue()) {
            // Render background quad
            if (ModuleGUI.INSTANCE.roundedModules.getValue()) {
                Renderer2d.renderRoundedQuad(
                        context.getMatrices(),
                        Opium.CLICK_GUI.getColor(),
                        (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                        this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                        radius, radius, radius, radius,
                        samples
                );
            } else {
                RenderUtils.drawRect(context.getMatrices(), (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                        this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                        Opium.CLICK_GUI.getColor());
            }

            // Render outline
            if (ModuleOutline.INSTANCE.moduleOutline.getValue()) {
                Color outlineColor = ModuleOutline.INSTANCE.moduleOutlineColor.getValue();
                float outlineWidth = 0.5f;

                if (ModuleGUI.INSTANCE.roundedModules.getValue()) {
                    Renderer2d.renderRoundedOutline(context.getMatrices(), outlineColor,
                            (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                            this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                            radius, radius, radius, radius, outlineWidth, samples * 4);
                } else {
                    Renderer2d.renderRoundedOutline(context.getMatrices(), outlineColor,
                            (float) this.getX() - 0.25f, (float) this.getY() - 0.2f,
                            this.getX() + this.getWidth() + 0.25f, (float) this.getY() + 0.2f + 14.1f,
                            0, 0, 0, 0, outlineWidth, 20 * 4);
                }
            }
        }

        // Render module name
        RenderUtils.drawString(context.getMatrices(), (!this.module.isToggled() ? Formatting.GRAY : "") + this.module.getTag(), this.getX() + 3, this.getY() + 3, ModuleGUI.INSTANCE.fadeText.getValue() ? this.colorMap.get(MathHelper.clamp(this.getY() + 3, 0, height)).getRGB() : -1);

        // Render keybinding if it exists and if we should
        if (this.module.getBind() != 0 && ModuleGUI.INSTANCE.displayKeybinds.getValue()) {
            String keyName = GLFW.glfwGetKeyName(this.module.getBind(), 0);
            String bindKey;

            if (keyName != null) {
                bindKey = (!this.module.isToggled() ? Formatting.GRAY : "") + "[" + keyName.toUpperCase() + "]";
            } else {
                bindKey = (!this.module.isToggled() ? Formatting.GRAY : "") + "[" + Keys.getFallbackKeyName(this.module.getBind()) + "]";
            }

            if (RenderUtils.getFontRenderer() == null) return;
            int paddingRight = 4;
            int bindKeyWidth = ModuleFont.INSTANCE.customFonts.getValue()
                    ? (int) FontRenderers.fontRenderer.getStringWidth(bindKey)
                    : mc.textRenderer.getWidth(bindKey);

            int bindKeyX = this.getX() + this.getWidth() - bindKeyWidth - paddingRight;

            RenderUtils.drawString(
                    context.getMatrices(),
                    bindKey,
                    bindKeyX,
                    this.getY() + 3,
                    ModuleGUI.INSTANCE.fadeText.getValue()
                            ? this.colorMap.get(MathHelper.clamp(this.getY() + 3, 0, height)).getRGB()
                            : -1
            );
        }

        for (Component component : this.components) {
            component.update(mouseX, mouseY, delta);
        }
        if (this.isOpen()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 0.1); // this crashes for whatever reason, something in font renderer
            for (Component component : this.components) {
                if (!component.isVisible()) continue;
                component.render(context, mouseX, mouseY, delta);
                context.getMatrices().translate(0, 0, -0.1);
                switch (component) {
                    case BooleanComponent booleanComponent -> {
                        if (!component.isHovering(mouseX, mouseY) || ((BooleanComponent) component).getValue().getDescription().isEmpty())
                            continue;

                        x5(context, booleanComponent.getValue().getDescription());
                        continue;
                    }
                    case NumberComponent numberComponent -> {
                        if (!component.isHovering(mouseX, mouseY) || numberComponent.getValue().getDescription().isEmpty())
                            continue;

                        x5(context, numberComponent.getValue().getDescription());
                    }
                    case EnumComponent enumComponent -> {
                        if (!component.isHovering(mouseX, mouseY) || enumComponent.getValue().getDescription().isEmpty())
                            continue;

                        x5(context, enumComponent.getValue().getDescription());
                    }
                    default -> {
                    }
                }
                if (!(component instanceof StringComponent stringComponent)) {
                    context.getMatrices().translate(0, 0, 0.1);
                    continue;
                }
                if (!component.isHovering(mouseX, mouseY) || stringComponent.getValue().getDescription().isEmpty()) {
                    context.getMatrices().translate(0, 0, 0.1);
                    continue;
                }

                int x = 5;
                int y = mc.getWindow().getScaledHeight() - 15;

                RenderUtils.drawRect(context.getMatrices(), x - 2, y - 2, (float) x + mc.textRenderer.getWidth(stringComponent.getValue().getDescription()) + 7.0f, y + 10, new Color(40, 40, 40));
                RenderUtils.drawOutline(context.getMatrices(), x - 2, y - 2, (float) x + mc.textRenderer.getWidth(stringComponent.getValue().getDescription()) + 7.0f, y + 10, 1.0f, ModuleColor.getColor());
                RenderUtils.drawString(context.getMatrices(), stringComponent.getValue().getDescription(), x, y, -1);
                context.getMatrices().translate(0, 0, 0.1);
            }
            context.getMatrices().pop();
        }
    }

    private void x5(DrawContext context, String description) {
        int x = 5;
        int y = mc.getWindow().getScaledHeight() - 15;

        RenderUtils.drawRect(context.getMatrices(), x - 2, y - 2, (float) x + mc.textRenderer.getWidth(description) + 7.0f, y + 10, new Color(40, 40, 40));
        RenderUtils.drawOutline(context.getMatrices(), x - 2, y - 2, (float) x + mc.textRenderer.getWidth(description) + 7.0f, y + 10, 1.0f, ModuleColor.getColor());
        RenderUtils.drawString(context.getMatrices(), description, x, y, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY)) {
            if (mouseButton == 0) {
                this.module.toggle(true);
            }
            if (mouseButton == 1) {
                this.setOpen(!this.open);
                this.getParent().refresh();
            }
        }
        if (this.isOpen()) {
            for (Component component : this.components) {
                if (!component.isVisible()) continue;
                component.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        for (Component component : this.components) {
            component.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void charTyped(char typedChar, int keyCode) {
        super.charTyped(typedChar, keyCode);
        if (this.isOpen()) {
            for (Component component : this.components) {
                if (!component.isVisible()) continue;
                component.charTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.isOpen()) {
            for (Component component : this.components) {
                component.onClose();
            }
        }
    }

    public ArrayList<Component> getComponents() {
        return this.components;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}