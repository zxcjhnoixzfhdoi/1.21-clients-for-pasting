package hack.echo.client.render2.api;

import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.utils.VulkanUtil;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector4i;

import java.util.Stack;

public abstract class Draw2D {
    public static final int cameraUboBinding = 7;

    protected static float z = 0.001f;

    //? if > 26.1.2 {
    /*protected static float d = VulkanUtil.isVulkanLoaded() ? 0.001f : -0.001f;
    *///?} else {
    protected static float d = 0.001f;
    //?}

    public static float nextZ() {
        return z += d;
    }

    protected Stack<Vector4i> scissorStack = new Stack<>();

    @Getter
    protected final CrossTexture blurResult = new CrossTexture();


    public abstract void cleanup();

    public abstract void beginFrame(Matrix4f proj);

    public abstract void endFrame();

    public abstract void rect(Matrix4f model, float x, float y, float w, float h, float radius, int color);

    public abstract void rect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3,
            float r4, int color);

    public abstract void rect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3,
            float r4, int c1, int c2, int c3, int c4);

    public abstract void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size,
            int color);

    public abstract void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size,
            int color);

    public abstract void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size,
            int c1, int c2, int c3, int c4);

    public abstract void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size,
            int c1, int c2, int c3, int c4);

    public abstract void image(Matrix4f model, CrossTexture texture, float x, float y, float w, float h,
            float radius, float alpha);

    public abstract void screenImage(Matrix4f model, CrossTexture texture, float x, float y, float w, float h,
            float radius, float alpha);

    public abstract void liquidGlass(Matrix4f model, float x, float y, float w, float h, float radius, int tint, float refractionStrength);


    public abstract void sbPicker(Matrix4f model, float x, float y, float w, float h, float hue);

    public abstract void hueSlider(Matrix4f model, float x, float y, float w, float h);

    public abstract void alphaSlider(Matrix4f model, float x, float y, float w, float h, float r, float g, float b);

    public abstract void flush();

    public abstract void pushScissor(float x, float y, float width, float height);

    public abstract void popScissor();

    protected static Vector4i intersect(Vector4i a, Vector4i b) {
        int ax1 = a.x, ay1 = a.y, ax2 = a.x + a.z, ay2 = a.y + a.w;
        int bx1 = b.x, by1 = b.y, bx2 = b.x + b.z, by2 = b.y + b.w;
        int ix1 = Math.max(ax1, bx1);
        int iy1 = Math.max(ay1, by1);
        int ix2 = Math.min(ax2, bx2);
        int iy2 = Math.min(ay2, by2);
        int iw = Math.max(0, ix2 - ix1);
        int ih = Math.max(0, iy2 - iy1);
        return new Vector4i(ix1, iy1, iw, ih);
    }
}
