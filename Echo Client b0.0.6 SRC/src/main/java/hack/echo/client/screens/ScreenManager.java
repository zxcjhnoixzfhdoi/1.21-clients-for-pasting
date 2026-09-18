package hack.echo.client.screens;

import hack.echo.client.features.settings.Setting;
import hack.echo.client.screens.clickgui.glassrewrite.GlassScreen;
import hack.echo.client.utils.Imports;

public class ScreenManager implements Imports {
    public static Setting focusedSetting = null;

    private GlassScreen glassClickGuiScreen;
    
    public void initialize() {
        glassClickGuiScreen = new GlassScreen();
    }
    
    public void displayClickGUI() {
        hack.echo.client.api.MinecraftCompat.setScreen(glassClickGuiScreen);
    }
    
    public boolean isClickGUIOpen() {
        return hack.echo.client.api.MinecraftCompat.getScreen() == glassClickGuiScreen;
    }
    
    public void closeAllScreens() {
        focusedSetting = null;
        hack.echo.client.api.MinecraftCompat.setScreen(null);
    }

    public void destroy() {
        closeAllScreens();
        glassClickGuiScreen = null;
    }
}
