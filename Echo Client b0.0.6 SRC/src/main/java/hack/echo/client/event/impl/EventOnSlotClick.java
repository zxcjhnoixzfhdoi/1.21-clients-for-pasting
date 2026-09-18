package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import hack.echo.client.api.InventoryClickType;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

@Getter
public class EventOnSlotClick extends Event {
    private final int slotIndex;
    private final int button;
    private final InventoryClickType actionType;
    private final Player player;

    public EventOnSlotClick(int slotIndex, int button, InventoryClickType actionType, Player player) {
        this.slotIndex = slotIndex;
        this.button = button;
        this.actionType = actionType;
        this.player = player;
    }
}
