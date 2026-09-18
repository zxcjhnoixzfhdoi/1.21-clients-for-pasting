package hack.echo.client.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public final class GuiGraphicsCompat {

    private final
    //? if >=26.1 {
    /*GuiGraphicsExtractor
    *///?} else {
    GuiGraphics
    //?}
    context;

    private GuiGraphicsCompat(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context
            *///?} else {
            GuiGraphics context
            //?}
    ) {
        this.context = context;
    }

    public static GuiGraphicsCompat of(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context
            *///?} else {
            GuiGraphics context
            //?}
    ) {
        if (context == null) {
            return null;
        }

        return new GuiGraphicsCompat(context);
    }

    public int guiWidth() {
        return context.guiWidth();
    }

    public int guiHeight() {
        return context.guiHeight();
    }

    public void pushMatrix() {
        context.pose().pushMatrix();
    }

    public void popMatrix() {
        context.pose().popMatrix();
    }

    public void translate(float x, float y) {
        //? if >=26.1 {
        /*context.pose().translate(x, y);
        *///?} else {
        context.pose().translate(x, y);
        //?}
    }

    public void renderItem(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        /*context.item(stack, x, y);
        *///?} else {
        context.renderItem(stack, x, y);
        //?}
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        /*context.itemDecorations(font, stack, x, y);
        *///?} else {
        context.renderItemDecorations(font, stack, x, y);
        //?}
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*context.text(font, text, x, y, color, shadow);
        *///?} else {
        context.drawString(font, text, x, y, color, shadow);
        //?}
    }

    public void submitEntityRenderState(
            EntityRenderState entityRenderState,
            float scale,
            Vector3f offset,
            Quaternionf baseRotation,
            Quaternionf cameraRotation,
            int left,
            int top,
            int right,
            int bottom
    ) {
        //? if >=26.1 {
        /*context.entity(entityRenderState, scale, offset, baseRotation, cameraRotation, left, top, right, bottom);
        *///?} else {
        context.submitEntityRenderState(entityRenderState, scale, offset, baseRotation, cameraRotation, left, top, right, bottom);
        //?}
    }
}
