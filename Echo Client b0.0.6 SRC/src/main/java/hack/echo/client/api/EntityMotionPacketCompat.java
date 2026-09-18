package hack.echo.client.api;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public final class EntityMotionPacketCompat {

    private EntityMotionPacketCompat() {
    }

    public static int id(ClientboundSetEntityMotionPacket packet) {
        //? if >=26.1 {
        /*return packet.id();
        *///?} else {
        return packet.getId();
        //?}
    }

    public static Vec3 movement(ClientboundSetEntityMotionPacket packet) {
        //? if >=26.1 {
        /*return packet.movement();
        *///?} else {
        return packet.getMovement();
        //?}
    }
}
