package we.devs.opium.api.utilities;

import net.minecraft.util.Hand;

public record FindItemResult(int slot, int count) implements IMinecraft {

    public boolean found() {
        return slot != -1;
    }

    public Hand getHand() {
        return slot == mc.player.getInventory().selectedSlot ? Hand.MAIN_HAND : null;
    }

    public boolean isMainHand() {
        return getHand() == Hand.MAIN_HAND;
    }

    public boolean isOffhand() {
        return getHand() == Hand.OFF_HAND;
    }
}
