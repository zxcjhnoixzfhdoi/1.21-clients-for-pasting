package we.devs.opium.client.events;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;

public class DamageBlockEvent extends EventArgument {
    private final BlockPos pos;
    private final Direction direction;
    private boolean cancelled = false; // Add a cancel flag

    public DamageBlockEvent(BlockPos pos, Direction direction) {
        this.pos = pos;
        this.direction = direction;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isCancelled() {
        return cancelled; // Getter for the cancel flag
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled; // Setter for the cancel flag
    }

    @Override
    public void call(EventListener listener) {
        listener.onDamageBlock(this);
    }
}
