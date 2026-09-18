package dev.ambience.features.gui.base;
public abstract class GuiBase {
    public boolean mouseClicked(double x, double y, int btn) { return false; }
    public boolean mouseReleased(double x, double y, int btn) { return false; }
    public boolean mouseDragged(double x, double y, int btn, double dx, double dy) { return false; }
    public boolean mouseScrolled(double x, double y, double hdx, double vdy) { return false; }
    public boolean keyPressed(int key, int scan, int mod) { return false; }
    public boolean charTyped(char c, int mod) { return false; }
    public void render(Object ctx, int mx, int my, float dt) {}
    public void tick() {}
    public void setFocused(Object e) {}
    public Object getFocused() { return null; }
    public boolean isDragging() { return false; }
    public java.util.List<?> children() { return java.util.Collections.emptyList(); }
}
