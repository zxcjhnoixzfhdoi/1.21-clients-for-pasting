package we.devs.opium.client.events;

import net.minecraft.entity.LivingEntity;
import we.devs.opium.api.manager.event.Event;
import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;

/**
 * Represents an event triggered when a living entity dies.
 */
public class DeathEvent extends EventArgument {
    private final LivingEntity entity;

    /**
     * Constructs a new DeathEvent.
     *
     * @param entity The entity that died.
     */
    public DeathEvent(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Gets the entity involved in the death event.
     *
     * @return The deceased entity.
     */
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public void call(EventListener listener) {
        listener.onDeath(this);
    }
}
