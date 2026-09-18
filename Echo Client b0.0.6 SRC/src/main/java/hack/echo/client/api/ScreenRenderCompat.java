package hack.echo.client.api;

import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.Screen;

public abstract class ScreenRenderCompat extends Screen {

    protected ScreenRenderCompat(Component title) {
        super(title);

    }


    //? if >= 26.1 {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {

    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor guiGraphics) {

    }
    *///?} else {

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {

    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {

    }
    //?}

    public float getMouseX(float mx) {
        return mx * RenderUtil.getScaledWidth() / Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public float getMouseY(float my) {
        return my * RenderUtil.getScaledHeight() / Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
