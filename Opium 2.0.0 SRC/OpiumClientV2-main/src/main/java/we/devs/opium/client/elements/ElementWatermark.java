package we.devs.opium.client.elements;

import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueCategory;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueString;

@RegisterElement(name="Watermark", description="The watermark for the client.")
public class ElementWatermark extends Element {
    private final ValueCategory watermarkCategory = new ValueCategory("Watermark", "The category for the watermark.");
    private final ValueEnum mode = new ValueEnum("Mode", "Mode", "The mode for the watermark.", this.watermarkCategory, Modes.Normal);
    private final ValueString customValue = new ValueString("WatermarkValue", "Value", "The value for the Custom Watermark.", this.watermarkCategory, "Opium");
    private final ValueCategory versionCategory = new ValueCategory("Version", "The category for the version.");
    private final ValueEnum version = new ValueEnum("Version", "Version", "Renders the Version on the watermark.", this.versionCategory, Versions.Normal);

    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);
        this.frame.setWidth(mc.textRenderer.getWidth(this.getText()));
        this.frame.setHeight(mc.textRenderer.fontHeight);
        RenderUtils.drawString(new MatrixStack(),this.getText(), (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    private String getText() {
        return (this.mode.getValue().equals(Modes.Custom) ? this.customValue.getValue() : Opium.NAME) + (!this.version.getValue().equals(Versions.None) ? " " + (this.version.getValue().equals(Versions.Normal) ? "v" : "") + Opium.VERSION : "");
    }

    public enum Versions {
        None,
        Simple,
        Normal
    }

    public enum Modes {
        Normal,
        Custom
    }
}
