package we.devs.opium.api.manager.miscellaneous;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.BlockBreakingInfo;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.utilities.IMinecraft;
import we.devs.opium.client.events.EventPacketSend;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class PlayerManager implements IMinecraft, EventListener {
    private boolean switching;
    private boolean sneaking;
    private int slot;
    private int sentPackets;
    private int receivedPackets;
    private int startBlockBreaking;

    public PlayerManager() {
        Opium.EVENT_MANAGER.register(this);
    }

    @Override
    public void onPacketSend(EventPacketSend event) {
        if (event.getPacket() instanceof ClientCommandC2SPacket a) {
            if (a.getMode() == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
                this.sneaking = true;
            } else if (a.getMode() == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
                this.sneaking = false;
            }
        }
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket b) {
            this.slot = b.getSelectedSlot();
        }
    }

    @Subscribe
    public int onStartBlockBreaking(BlockBreakingInfo event) {
        if (event.getStage() != 0) {
        return this.startBlockBreaking = event.getStage();
        } else {
            return this.startBlockBreaking = 0;
        }
    }

    public boolean isSwitching() {
        return this.switching;
    }

    public void setSwitching(boolean switching) {
        this.switching = switching;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public int getSlot() {
        return this.slot;
    }

    public int getSentPackets() {
        return this.sentPackets;
    }

    public int getReceivedPackets() {
        return this.receivedPackets;
    }
}
