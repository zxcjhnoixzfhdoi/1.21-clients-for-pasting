package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.Setting;
import hack.echo.client.render2.api.Draw2D;
import org.joml.Matrix4f;

public interface GlassSettingElement {


    void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta);

    float getHeight();

    boolean mouseClicked(double mouseX, double mouseY, int button);

    void mouseReleased(double mouseX, double mouseY, int button);

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default void charTyped(char chr, int modifiers) {
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    Setting getSetting();
}
