package com.instrumentalist.krs.hacks.features.combat;



import com.instrumentalist.krs.events.features.UpdateEvent;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.utils.packet.PacketUtil;
import com.instrumentalist.krs.utils.value.IntValue;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemUseAnimation;
import org.lwjgl.glfw.GLFW;

public class FastEat extends Module {

    public FastEat() {
        super("Fast Eat", ModuleCategory.Combat, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Setting
    private final IntValue packets = new IntValue("Packets", 20, 1, 20, "x");

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null) return;

        ItemUseAnimation animation = player.getMainHandItem().getUseAnimation();
        if (player.isUsingItem() && player.getUseItemRemainingTicks() >= 30 && (animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK)) {
            boolean horizontalCollision = player.horizontalCollision;
            int packetCount = packets.get();
            for (int i = 0; i < packetCount; i++) {
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(true, horizontalCollision));
            }
        }
    }
}
