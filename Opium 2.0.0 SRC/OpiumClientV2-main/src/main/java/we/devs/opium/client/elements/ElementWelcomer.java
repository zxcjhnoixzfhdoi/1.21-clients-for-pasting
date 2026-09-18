package we.devs.opium.client.elements;


import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueCategory;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueString;

@RegisterElement(name = "Welcomer", description = "A welcoming element.")
public class ElementWelcomer extends Element {
    private final ValueCategory welcomerCategory = new ValueCategory("Modes", "different modes for greeter.");
    private final ValueEnum mode = new ValueEnum("Mode", "Mode", "The mode for the welcomer.", this.welcomerCategory, Modes.Normal);
    private final ValueString customValue = new ValueString("WelcomerValue", "Value", "The value for the Custom Welcomer.", this.welcomerCategory, "Welcome");

    @Override
    public void onRender2D(EventRender2D event) {
        if (mc.textRenderer == null) return;

        super.onRender2D(event);

        String text = this.getText();
        this.frame.setWidth(mc.textRenderer.getWidth(text));
        this.frame.setHeight(mc.textRenderer.fontHeight);

        RenderUtils.drawString(event.getContext().getMatrices(), text, (int) this.frame.getX(), (int) this.frame.getY(), ModuleColor.getColor().getRGB());
    }

    private String getText() {
        String username = mc.player != null ? mc.player.getName().getString() : "Player";

        switch ((Modes) this.mode.getValue()) {
            case Custom:
                return this.customValue.getValue() + " " + username + " :^)";

            case Time:
                return getDynamicGreeting() + ", " + username + " :^)";

            case Heil:
                return "Sieg Heil " + username + " >:^";

            case Jew:
                return "Shalom " + username + "!!!";

            default:
                return "Hello " + username + " :^)";
        }
    }

    private String getDynamicGreeting() {
        int hour = java.time.LocalTime.now().getHour();

        if (hour >= 5 && hour < 12) {
            return "Good morning";
        } else if (hour >= 12 && hour < 18) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    public enum Modes {
        Normal,
        Time,
        Jew,
        Custom,
        Heil
    }
}
