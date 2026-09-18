package hack.echo.client.api;

import net.minecraft.client.Minecraft;

//? if >=26.1 {
/*import java.lang.reflect.Method;
*///?}

public final class CrosshairPickCompat {

    //? if >=26.1 {
    /*private static final Method PICK_METHOD = findPickMethod();
    *///?}

    private CrosshairPickCompat() {
    }

    public static void pick(Minecraft minecraft, float partialTick) {
        if (minecraft == null) {
            return;
        }

        //? if >=26.1 {
        /*if (PICK_METHOD == null) {
            return;
        }

        try {
            PICK_METHOD.invoke(minecraft, partialTick);
        } catch (ReflectiveOperationException ignored) {
        }
        *///?} else {
        minecraft.gameRenderer.pick(partialTick);
        //?}
    }

    //? if >=26.1 {
    /*private static Method findPickMethod() {
        try {
            Method method = Minecraft.class.getDeclaredMethod("pick", float.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
    *///?}
}
