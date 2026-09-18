package hack.echo.client.mixin.render.gui;

//? if >26.1.2 {
/*import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventSetScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui_26_2 {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventSetScreen event = new EventSetScreen(screen);
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

}
*///?} else {
public final class MixinGui_26_2 {
}
//?}
