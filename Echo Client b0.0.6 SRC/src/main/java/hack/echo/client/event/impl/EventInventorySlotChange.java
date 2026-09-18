package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;

@Getter
public class EventInventorySlotChange extends Event {

    private final int slot;
    private final ItemStack currentStack;
    private final ItemStack newStack;

    public EventInventorySlotChange(int slot, ItemStack currentStack, ItemStack newStack) {
        this.slot = slot;
        this.currentStack = currentStack;
        this.newStack = newStack;
    }
}
