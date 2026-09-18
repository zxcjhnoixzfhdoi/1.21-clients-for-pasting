package we.devs.opium.client.gui.click.components;

import we.devs.opium.Opium;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.values.impl.ValueColor;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ColorComponentTest extends Component {
    private final ValueColor value;
    private boolean open = false;
    private boolean hueDragging = false;
    private boolean hsDragging = false;
    private boolean alphaDragging = false;
    private float hueWidth = 0;
    private float saturationWidth = 0;
    private float brightnessHeight = 0;
    private float alphaWidth = 0;

    public ColorComponentTest(ValueColor value, int offset, Frame parent) {
        super(offset, parent);
        this.value = value;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        // Draw the color preview box
        drawColorPreview(context);

        // Draw the expanded color picker if open
        if (this.isOpen()) {
            drawHueBar(context);
            drawColorPanel(context);
            drawAlphaBar(context);
            drawButtons(context);
            drawRainbowAndSyncOptions(context);
        }

        // Draw the title
        RenderUtils.drawString(context.getMatrices(), this.value.getTag(), this.getX() + 3, this.getY() + 3, -1);
    }

    private void drawColorPreview(DrawContext context) {
        RenderUtils.drawRect(context.getMatrices(), this.getX() + this.getWidth() - 12, this.getY() + 2, this.getX() + this.getWidth() - 2, this.getY() + 12, this.value.getValue());
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + this.getWidth() - 12, this.getY() + 2, this.getX() + this.getWidth() - 2, this.getY() + 12, 0.5f, new Color(20, 20, 20));
    }

    private void drawHueBar(DrawContext context) {
        float i = 0.0f;
        while (i + 1.0f < 96.0f) {
            RenderUtils.drawRect(context.getMatrices(), this.getX() + 2 + i, this.getY() + 16, this.getX() + 2 + i + 1.0f, this.getY() + 27, Color.getHSBColor(i / 96.0f, 1.0f, 1.0f));
            i += 0.45f;
        }
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2, this.getY() + 16, this.getX() + this.getWidth() - 2, this.getY() + 27, 0.5f, Color.BLACK);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2 + this.hueWidth, this.getY() + 16, this.getX() + 2 + this.hueWidth + 2.0f, this.getY() + 27, 0.5f, Color.WHITE);
    }

    private void drawColorPanel(DrawContext context) {
        float[] hsb = Color.RGBtoHSB(this.value.getValue().getRed(), this.value.getValue().getGreen(), this.value.getValue().getBlue(), null);
        Color color = Color.getHSBColor(hsb[0], 1.0f, 1.0f);
        RenderUtils.drawSidewaysGradient(context.getMatrices(), this.getX() + 2, this.getY() + 29, this.getWidth() - 4, 96, new Color(255, 255, 255), color);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2, this.getY() + 29, this.getX() + this.getWidth() - 2, this.getY() + 29 + 96, 0.5f, Color.BLACK);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2 + this.saturationWidth, this.getY() + 29 - this.brightnessHeight + 96, this.getX() + 2 + this.saturationWidth + 2.0f, this.getY() + 29 - this.brightnessHeight + 2.0f + 96, 0.8f, Color.BLACK);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2 + this.saturationWidth, this.getY() + 29 - this.brightnessHeight + 96, this.getX() + 2 + this.saturationWidth + 2.0f, this.getY() + 29 - this.brightnessHeight + 2.0f + 96, 0.5f, Color.WHITE);
    }

    private void drawAlphaBar(DrawContext context) {
        Color color = this.value.getValue();
        RenderUtils.drawSidewaysGradient(context.getMatrices(), this.getX() + 2, this.getY() + 28 + 96, this.getWidth() - 4, 11, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0), color);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2, this.getY() + 28 + 96, this.getX() + this.getWidth() - 2, this.getY() + 39 + 96, 0.5f, Color.BLACK);
        RenderUtils.drawOutline(context.getMatrices(), this.getX() + 2 + this.alphaWidth, this.getY() + 28 + 96, this.getX() + 2 + this.alphaWidth + 2.0f, this.getY() + 39 + 96, 0.5f, Color.WHITE);
    }

    private void drawButtons(DrawContext context) {
        RenderUtils.drawRect(context.getMatrices(), this.getX() + 2, this.getY() + 42 + 96, this.getX() + 49, this.getY() + 53 + 96, Opium.CLICK_GUI.getColor());
        RenderUtils.drawString(context.getMatrices(), "Copy", this.getX() + 25 - mc.textRenderer.getWidth("Copy") / 2, this.getY() + 47 + 96 - mc.textRenderer.fontHeight / 2, -1);
        RenderUtils.drawRect(context.getMatrices(), this.getX() + 51, this.getY() + 42 + 96, this.getX() + 98, this.getY() + 53 + 96, Opium.CLICK_GUI.getColor());
        RenderUtils.drawString(context.getMatrices(), "Paste", this.getX() + 75 - mc.textRenderer.getWidth("Paste") / 2, this.getY() + 47 + 96 - mc.textRenderer.fontHeight / 2, -1);
    }

    private void drawRainbowAndSyncOptions(DrawContext context) {
        if (this.value.isRainbow()) {
            RenderUtils.drawRect(context.getMatrices(), this.getX() + 2, this.getY() + 56 + 96, this.getX() + this.getWidth() - 2, this.getY() + 70 + 96, Opium.CLICK_GUI.getColor());
        }
        RenderUtils.drawString(context.getMatrices(), "Rainbow", this.getX() + 48 - mc.textRenderer.getWidth("Rainbow") / 2, this.getY() + 59 + 96, -1);

        if (this.value.isSync()) {
            RenderUtils.drawRect(context.getMatrices(), this.getX() + 2, this.getY() + 73 + 96, this.getX() + this.getWidth() - 2, this.getY() + 87 + 96, Opium.CLICK_GUI.getColor());
        }
        RenderUtils.drawString(context.getMatrices(), "Sync", this.getX() + 48 - mc.textRenderer.getWidth("Sync") / 2, this.getY() + 76 + 96, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (this.isOpen()) {
                handleColorPickerClick(mouseX, mouseY);
            }
        } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
        }
    }

    private void handleColorPickerClick(int mouseX, int mouseY) {
        if (this.isHoveringHue(mouseX, mouseY)) {
            this.hueDragging = true;
        } else if (this.isHoveringHS(mouseX, mouseY)) {
            this.hsDragging = true;
        } else if (this.isHoveringAlpha(mouseX, mouseY)) {
            this.alphaDragging = true;
        } else if (this.isHoveringCopy(mouseX, mouseY)) {
            Opium.COLOR_CLIPBOARD = this.value.getActualValue();
        } else if (this.isHoveringPaste(mouseX, mouseY) && Opium.COLOR_CLIPBOARD != null) {
            this.value.setValue(Opium.COLOR_CLIPBOARD);
        } else if (this.isHoveringRainbow(mouseX, mouseY)) {
            this.value.setRainbow(!this.value.isRainbow());
        } else if (this.isHoveringSync(mouseX, mouseY)) {
            this.value.setSync(!this.value.isSync());
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.hueDragging = false;
        this.hsDragging = false;
        this.alphaDragging = false;
    }

    @Override
    public void update(int mouseX, int mouseY, float partialTicks) {
        super.update(mouseX, mouseY, partialTicks);
        if (this.value.getParent() != null) {
            this.setVisible(this.value.getParent().isOpen());
        }
        updateColorValues(mouseX, mouseY);
    }

    private void updateColorValues(int mouseX, int mouseY) {
        float[] hsb = Color.RGBtoHSB(this.value.getValue().getRed(), this.value.getValue().getGreen(), this.value.getValue().getBlue(), null);
        double difference = Math.min(95, Math.max(0, mouseX - this.getX()));
        double differenceY = Math.min(95, Math.max(0, mouseY - this.getY() - 29));
        this.hueWidth = 95.5f * (hsb[0] * 360.0f / 360.0f);
        this.saturationWidth = 94.5f * (hsb[1] * 360.0f / 360.0f);
        this.brightnessHeight = 94.5f * (hsb[2] * 360.0f / 360.0f);
        this.alphaWidth = 94.5f * ((float) this.value.getValue().getAlpha() / 255.0f);
        this.changeColor(difference, new Color(Color.HSBtoRGB((float) (difference / 95.0 * 360.0 / 360.0), hsb[1], hsb[2])), new Color(Color.HSBtoRGB(0.0f, hsb[1], hsb[2])), this.hueDragging);
        this.changeHS(difference, differenceY, new Color(Color.HSBtoRGB(hsb[0], (float) (difference / 95.0 * 360.0 / 360.0), 1.0f - (float) (differenceY / 95.0 * 360.0 / 360.0))), new Color(Color.HSBtoRGB(hsb[0], 0.0f, 0.0f)), this.hsDragging);
        this.changeAlpha(difference, (float) (difference / 95.0 * 255.0 / 255.0), this.alphaDragging);
    }

    private void changeHS(double difference, double difference2, Color color, Color zeroColor, boolean dragging) {
        if (dragging) {
            if (difference == 0.0 && difference2 == 0.0) {
                this.value.setValue(new Color(zeroColor.getRed(), zeroColor.getGreen(), zeroColor.getBlue(), this.value.getValue().getAlpha()));
            } else {
                this.value.setValue(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.value.getValue().getAlpha()));
            }
        }
    }

    private void changeColor(double difference, Color color, Color zeroColor, boolean dragging) {
        if (dragging) {
            if (difference == 0.0) {
                this.value.setValue(new Color(zeroColor.getRed(), zeroColor.getGreen(), zeroColor.getBlue(), this.value.getValue().getAlpha()));
            } else {
                this.value.setValue(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.value.getValue().getAlpha()));
            }
        }
    }

    private void changeAlpha(double difference, float alpha, boolean dragging) {
        if (dragging) {
            if (difference == 0.0) {
                this.value.setValue(new Color(this.value.getValue().getRed(), this.value.getValue().getGreen(), this.value.getValue().getBlue(), 0));
            } else {
                this.value.setValue(new Color(this.value.getValue().getRed(), this.value.getValue().getGreen(), this.value.getValue().getBlue(), (int) (alpha * 255.0f)));
            }
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isHoveringHue(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + this.getWidth() - 2 && mouseY > this.getY() + 16 && mouseY < this.getY() + 27;
    }

    public boolean isHoveringHS(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + this.getWidth() - 2 && mouseY > this.getY() + 29 && mouseY < this.getY() + 25 + this.getWidth();
    }

    public boolean isHoveringAlpha(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + this.getWidth() - 2 && mouseY > this.getY() + 28 + this.getWidth() && mouseY < this.getY() + 39 + this.getWidth();
    }

    public boolean isHoveringCopy(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + 49 && mouseY > this.getY() + 42 + this.getWidth() && mouseY < this.getY() + 53 + this.getWidth();
    }

    public boolean isHoveringPaste(int mouseX, int mouseY) {
        return mouseX > this.getX() + 51 && mouseX < this.getX() + 98 && mouseY > this.getY() + 42 + this.getWidth() && mouseY < this.getY() + 53 + this.getWidth();
    }

    public boolean isHoveringRainbow(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + 98 && mouseY > this.getY() + 56 + this.getWidth() && mouseY < this.getY() + 70 + this.getWidth();
    }

    public boolean isHoveringSync(int mouseX, int mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + 98 && mouseY > this.getY() + 73 + this.getWidth() && mouseY < this.getY() + 87 + this.getWidth();
    }
}