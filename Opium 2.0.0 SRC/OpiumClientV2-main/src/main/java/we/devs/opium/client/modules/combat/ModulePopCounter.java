package we.devs.opium.client.modules.combat;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.events.EventPacketReceive;
import we.devs.opium.client.modules.client.ModuleCommands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.HashMap;

@RegisterModule(name="PopCounter", tag="PopCounter", description="Keeps count of how any totems a player pops.", category=Module.Category.COMBAT)
public class ModulePopCounter extends Module {
    public static final HashMap<String, Integer> popCount = new HashMap<>();

    public void onPacketReceive(EventPacketReceive event) {
        EntityStatusS2CPacket packet;
        if (nullCheck()) {
            return;
        }
        if (event.getPacket() instanceof EntityStatusS2CPacket && (packet = (EntityStatusS2CPacket) event.getPacket()).getStatus() == 35) {
            Entity entity = packet.getEntity(mc.world);
            if(!(entity instanceof PlayerEntity pl)) return;
            int count = 1;
            String user = pl.getGameProfile().getName();
            if (popCount.containsKey(user)) {
                count = popCount.get(user) + 1;
                popCount.put(user, count);
            } else {
                popCount.put(user, count);
            }
            String text = "" + ModuleCommands.getFirstColor();
            if (entity == mc.player) {
                text += "You";
            } else text += user;
            text += " popped " + ModuleCommands.getSecondColor() + count + ModuleCommands.getFirstColor() + " totems!";
            if (Opium.FRIEND_MANAGER.isFriend(user) && entity != mc.player) {
                text += " you should go help them";
            }
            ChatUtils.sendMessage(text);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (nullCheck()) {
            return;
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isDead() || player.getHealth() <= 0.0f) popCount.remove(player.getGameProfile().getName());
        }
    }
}