package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventHotbarChange;
import hack.echo.client.event.impl.EventInventorySlotChange;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Inventory.class)
public class MixinInventory {

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void onSetSelectedSlot(int slot, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        Inventory inv = (Inventory) (Object) this;
        int prev = inv.getSelectedSlot();
        EventHotbarChange e = new EventHotbarChange(prev, slot);
        e.call();
        if (e.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        Inventory inv = (Inventory) (Object) this;
        ItemStack currentStack = inv.getItem(slot);
        EventInventorySlotChange e = new EventInventorySlotChange(slot, currentStack, stack);
        e.call();
        if (e.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void onRemoveStack(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (Echo.isDestroyed) return;

        Inventory inv = (Inventory) (Object) this;
        ItemStack currentStack = inv.getItem(slot);
        EventInventorySlotChange e = new EventInventorySlotChange(slot, currentStack, ItemStack.EMPTY);
        e.call();
        if (e.cancelled) {
            cir.setReturnValue(ItemStack.EMPTY);
            cir.cancel();
        }
    }
}
