package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class EventOnTridentStoppedUsing extends Event {
    private final ItemStack stack;
    private final LivingEntity user;
    private final int remainingUseTicks;

    public EventOnTridentStoppedUsing(LivingEntity user, ItemStack stack, int remainingUseTicks) {
        this.user = user;
        this.stack = stack;
        this.remainingUseTicks = remainingUseTicks;
    }

    public ItemStack getStack() {
        return stack;
    }

    public LivingEntity getUser() {
        return user;
    }

    public int getRemainingUseTicks() {
        return remainingUseTicks;
    }

}
