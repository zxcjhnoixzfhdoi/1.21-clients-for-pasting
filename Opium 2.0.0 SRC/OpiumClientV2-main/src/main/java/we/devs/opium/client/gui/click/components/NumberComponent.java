package we.devs.opium.client.gui.click.components;

import we.devs.opium.Opium;
import we.devs.opium.api.utilities.MathUtils;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.gui.click.manage.Component;
import we.devs.opium.client.gui.click.manage.Frame;
import we.devs.opium.client.values.impl.ValueNumber;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;

public class NumberComponent extends Component {
    private final ValueNumber value;
    private float sliderWidth;
    private boolean dragging;
    private float targetSliderWidth; // For smooth slider animation

    public NumberComponent(ValueNumber value, int offset, Frame parent) {
        super(offset, parent);
        this.value = value;
        this.targetSliderWidth = calculateSliderWidth();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        // Smoothly animate the slider width
        if (sliderWidth != targetSliderWidth) {
            sliderWidth = MathUtils.lerp(sliderWidth, targetSliderWidth, 0.2f);
        }

        // Calculate the color based on the current value (fade effect)
        Color sliderColor = getSliderColor();

        // Draw the slider background with the fade effect
        RenderUtils.drawRect(
                context.getMatrices(),
                this.getX() + 1,
                this.getY(),
                this.getX() + 1 + sliderWidth,
                this.getY() + 14,
                sliderColor
        );

        // Draw the value text
        String valueText = this.value.getTag() + " " + Formatting.GRAY + this.value.getValue() + (this.value.getType() == 1 ? ".0" : "");
        RenderUtils.drawString(
                context.getMatrices(),
                valueText,
                this.getX() + 3,
                this.getY() + 3,
                -1
        );
    }

    @Override
    public void update(int mouseX, int mouseY, float partialTicks) {
        super.update(mouseX, mouseY, partialTicks);
        if (this.value.getParent() != null) {
            this.setVisible(this.value.getParent().isOpen());
        }

        // Update the slider width based on the current value
        targetSliderWidth = calculateSliderWidth();

        // Handle dragging to update the value
        if (dragging) {
            double difference = Math.min(98, Math.max(0, mouseX - this.getX()));
            updateValueFromDrag(difference);
        }
    }

    private float calculateSliderWidth() {
        switch (this.value.getType()) {
            case 1: // Integer
                return 98.0f * (this.value.getValue().intValue() - this.value.getMinimum().intValue()) / (this.value.getMaximum().intValue() - this.value.getMinimum().intValue());
            case 2: // Double
                return (float) (98.0 * (this.value.getValue().doubleValue() - this.value.getMinimum().doubleValue()) / (this.value.getMaximum().doubleValue() - this.value.getMinimum().doubleValue()));
            case 3: // Float
                return 98.0f * (this.value.getValue().floatValue() - this.value.getMinimum().floatValue()) / (this.value.getMaximum().floatValue() - this.value.getMinimum().floatValue());
            default:
                return 0f;
        }
    }

    private void updateValueFromDrag(double difference) {
        switch (this.value.getType()) {
            case 1: // Integer
                int intValue = (int) MathUtils.roundToPlaces(difference / 98.0 * (this.value.getMaximum().intValue() - this.value.getMinimum().intValue()) + this.value.getMinimum().intValue(), 0);
                this.value.setValue(intValue);
                break;
            case 2: // Double
                double doubleValue = MathUtils.roundToPlaces(difference / 98.0 * (this.value.getMaximum().doubleValue() - this.value.getMinimum().doubleValue()) + this.value.getMinimum().doubleValue(), 2);
                this.value.setValue(doubleValue);
                break;
            case 3: // Float
                float floatValue = (float) MathUtils.roundToPlaces(difference / 98.0 * (this.value.getMaximum().floatValue() - this.value.getMinimum().floatValue()) + this.value.getMinimum().floatValue(), 2);
                this.value.setValue(floatValue);
                break;
        }
    }

    /**
     * Calculates the slider color based on the current value.
     * The color becomes brighter as the value increases.
     *
     * @return The interpolated color.
     */
    private Color getSliderColor() {
        // Base color (darker)
        Color baseColor = Opium.CLICK_GUI.getColor();

        // Target color (brighter)
        Color targetColor = new Color(
                Math.min(255, baseColor.getRed() + 50),
                Math.min(255, baseColor.getGreen() + 50),
                Math.min(255, baseColor.getBlue() + 50),
                baseColor.getAlpha()
        );

        // Calculate the interpolation factor (0 = min value, 1 = max value)
        float progress = (float) ((value.getValue().doubleValue() - value.getMinimum().doubleValue()) / (value.getMaximum().doubleValue() - value.getMinimum().doubleValue()));

        // Interpolate between the base color and the target color
        int red = (int) MathUtils.lerp(baseColor.getRed(), targetColor.getRed(), progress);
        int green = (int) MathUtils.lerp(baseColor.getGreen(), targetColor.getGreen(), progress);
        int blue = (int) MathUtils.lerp(baseColor.getBlue(), targetColor.getBlue(), progress);

        return new Color(red, green, blue, baseColor.getAlpha());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovering(mouseX, mouseY) && mouseButton == 0) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = false;
    }

    public ValueNumber getValue() {
        return this.value;
    }
}