package hack.echo.client.mixin.render.abstractcontainermenu;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventOnSlotClick;
import hack.echo.client.api.InventoryClickCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu_26_1 {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventOnSlotClick event = new EventOnSlotClick(
                slotIndex,
                button,
                InventoryClickCompat.fromVanilla(actionType),
                player
        );
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
*///?} else {
public final class MixinAbstractContainerMenu_26_1 {
}
//?}
