package xyz.breadloaf.imguimc.theme;

import xyz.breadloaf.imguimc.interfaces.Theme;

public class ImGuiLightTheme implements Theme {
    @Override
    public void preRender() {
        KrsImGuiTheme.applyLight();
    }

    @Override
    public void postRender() {

    }
}
