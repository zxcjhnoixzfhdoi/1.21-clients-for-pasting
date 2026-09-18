package hack.echo.client.utils;

import hack.echo.client.utils.player.ITimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;

public interface Imports {
    Minecraft mc = Minecraft.getInstance();

    static void setTimer(float timer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        DeltaTracker counter = minecraft.getDeltaTracker();

        if (counter instanceof ITimer accessor) {
            accessor.setTimer(timer);
        }
    }
}
