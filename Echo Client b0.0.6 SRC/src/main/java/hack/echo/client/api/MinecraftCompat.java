package hack.echo.client.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

public final class MinecraftCompat {

    private MinecraftCompat() {
    }

    public static Screen getScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >26.1.2 {
        /*if (minecraft.gui == null) {
            return null;
        }

        return minecraft.gui.screen();
        *///?} else {
        return minecraft.screen;
        //?}
    }

    public static void setScreen(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >26.1.2 {
        /*if (minecraft.gui == null) {
            return;
        }

        minecraft.gui.setScreen(screen);
        *///?} else {
        minecraft.setScreen(screen);
        //?}
    }

    public static Overlay getOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >26.1.2 {
        /*if (minecraft.gui == null) {
            return null;
        }

        return minecraft.gui.overlay();
        *///?} else {
        return minecraft.getOverlay();
        //?}
    }

    public static boolean hasOverlay() {
        return getOverlay() != null;
    }
}
