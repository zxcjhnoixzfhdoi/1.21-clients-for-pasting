package we.devs.opium.api.manager.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.utilities.RotationUtils;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.events.EventPacketSend;
import we.devs.opium.client.modules.client.ModuleRotations;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class RotationManager implements EventListener {
    public RotationManager() {
        Opium.EVENT_MANAGER.register(this);
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ModuleRotations rotationModule = ModuleRotations.INSTANCE;
    private float yaw, pitch, currentPriority;
    private final List<Rotation> rotationRequests = new ArrayList<>();

    @Override
    public void onMotion(EventMotion event) {
        if (yaw != mc.player.getYaw()) {
            event.setRotationYaw(yaw);
        }

        if (pitch != mc.player.getPitch()) {
            event.setRotationPitch(pitch);
        }
    }

    @Override
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null || mc.world == null) return;

        if(event.getPacket() instanceof PlayerMoveC2SPacket packet && packet.changesLook()) {
            yaw = packet.getYaw(0f);
            pitch = packet.getPitch(0f);
            for (Rotation rotation : rotationRequests) {
                if (rotation.getPriority() > currentPriority) {
                    currentPriority = rotation.getPriority();
                    if (rotation.isBlock()) handleRotation(rotation, rotationModule.getBlockRotations(), rotationModule.getBlockSmooth());
                    else handleRotation(rotation, rotationModule.getEntityRotations(), rotationModule.getEntitySmooth());
                    rotationRequests.remove(rotation);
                }
            }
        }
    }

    private void handleRotation(Rotation rotation, ModuleRotations.Rotations type, int smooth) {
        switch (rotationModule.getBlockRotations()) {
            case Packet -> {
                PlayerMoveC2SPacket p = new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(), rotation.getPitch(), mc.player.isOnGround());
                mc.getNetworkHandler().sendPacket(p);
                yaw = rotation.getYaw();
                pitch = rotation.getPitch();
            }
            case NCP -> {
                float[] a = new float[]{rotation.getYaw(), rotation.getPitch()};
                float[] angles = RotationUtils.getSmoothRotations(a, smooth);
                PlayerMoveC2SPacket p = new PlayerMoveC2SPacket.LookAndOnGround(angles[0], angles[1], mc.player.isOnGround());
                mc.getNetworkHandler().sendPacket(p);
                yaw= angles[0];
                pitch= angles[1];
            }
            case Grim -> {
                PlayerMoveC2SPacket first = new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotation.getYaw(), rotation.getPitch(), mc.player.isOnGround());
                PlayerMoveC2SPacket second = new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotation.getYaw(), rotation.getPitch(), mc.player.isOnGround());
                mc.getNetworkHandler().sendPacket(first);
                yaw = rotation.getYaw();
                pitch = rotation.getPitch();
                mc.getNetworkHandler().sendPacket(second);
                yaw = mc.player.getYaw();
                pitch = mc.player.getPitch();
            }
        }
    }

    public void requestRotation(int priority, Vec3d pos, boolean block) {
        float[] angles = RotationUtils.getRotationsTo(pos);
        rotationRequests.add(new Rotation(priority, angles[0], angles[1], block));
    }
}
