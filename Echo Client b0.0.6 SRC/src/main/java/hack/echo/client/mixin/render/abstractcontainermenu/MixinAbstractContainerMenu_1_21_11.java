package hack.echo.client.mixin.render.abstractcontainermenu;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventOnSlotClick;
import hack.echo.client.api.InventoryClickCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu_1_21_11 {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
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
//?} else {
/*public final class MixinAbstractContainerMenu_1_21_11 {
}
*///?}
