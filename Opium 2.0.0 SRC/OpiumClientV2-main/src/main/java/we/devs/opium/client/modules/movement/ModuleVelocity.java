package we.devs.opium.client.modules.movement;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Formatting;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.asm.mixins.IClientPlayerEntity;
import we.devs.opium.asm.mixins.IEntityVelocityUpdateS2CPacket;
import we.devs.opium.asm.mixins.IExplosionS2CPacket;
import we.devs.opium.client.events.EventPacketReceive;
import we.devs.opium.client.events.EventPush;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name="Velocity", description="Remove the knockback of the player.", category = Module.Category.MOVEMENT )
public class ModuleVelocity extends Module {
    public static ValueBoolean noPush = new ValueBoolean("NoPush", "NoPush", "Players will no longer be able to Push you.", false);
    public static ValueBoolean wallsOnly = new ValueBoolean("Walls", "Walls", "Only activates the velocity while you are inside of a wall.", false);
    public static ValueBoolean grim = new ValueBoolean("Grim", "Grim", "Bypass for GrimAC v2", false);
    public static ValueNumber horizontal = new ValueNumber("Horizontal", "Horizontal", "", 0.0f, 0.0f, 100.0f);
    public static ValueNumber vertical = new ValueNumber("Vertical", "Vertical", "", 0.0f, 0.0f, 100.0f);

    private Boolean doingVelocity = true;
    private Formatting hudStringColor = Formatting.GREEN;
    private String getVelocityState() {
        if (doingVelocity) {
            return "ON";
        } else return "OFF";
    }
    public void onTick() { // handle states
        if (wallsOnly.getValue() && mc.world.getBlockState(mc.player.getBlockPos()).isAir()) {
            doingVelocity = false;
            hudStringColor = Formatting.RED;
        }
        if (wallsOnly.getValue() && !mc.world.getBlockState(mc.player.getBlockPos()).isAir()) {
            doingVelocity = true;
            hudStringColor = Formatting.GREEN;
        }
        if (!wallsOnly.getValue()) {
            doingVelocity = true;
            hudStringColor = Formatting.GREEN;
        }
    }

    public void onPacketReceive(EventPacketReceive event) {
        EntityVelocityUpdateS2CPacket sPacketEntityVelocity;

        if (mc.player == null || mc.world == null) {
            return;
        }

        if (wallsOnly.getValue() && mc.world.getBlockState(mc.player.getBlockPos()).isAir()) {
            return;
        }

        if (grim.getValue() && event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
            if (mc.player != null && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()))
                return;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ((IClientPlayerEntity) mc.player).getLastYaw(), ((IClientPlayerEntity) mc.player).getLastPitch(), mc.player.isOnGround()));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(mc.player.getPos()), Direction.DOWN));
            return;
        }

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket && (sPacketEntityVelocity = (EntityVelocityUpdateS2CPacket) event.getPacket()).getEntityId() == mc.player.getId()) {
            if (horizontal.getValue().floatValue() == 0.0f && vertical.getValue().floatValue() == 0.0f) {
                event.cancel();
            }
            ((IEntityVelocityUpdateS2CPacket) sPacketEntityVelocity).setX((int) (sPacketEntityVelocity.getVelocityX() * horizontal.getValue().floatValue() / 100.0f));
            ((IEntityVelocityUpdateS2CPacket) sPacketEntityVelocity).setY((int) (sPacketEntityVelocity.getVelocityY() * vertical.getValue().floatValue() / 100.0f));
            ((IEntityVelocityUpdateS2CPacket) sPacketEntityVelocity).setZ((int) (sPacketEntityVelocity.getVelocityZ() * horizontal.getValue().floatValue() / 100.0f));

            doingVelocity = true;
            hudStringColor = Formatting.GREEN;

        }
        if (event.getPacket() instanceof ExplosionS2CPacket sPacketExplosion) {
            ((IExplosionS2CPacket) sPacketExplosion).setX((sPacketExplosion.getPlayerVelocityX() * horizontal.getValue().floatValue() / 100.0f));
            ((IExplosionS2CPacket) sPacketExplosion).setY((sPacketExplosion.getPlayerVelocityY() * vertical.getValue().floatValue() / 100.0f));
            ((IExplosionS2CPacket) sPacketExplosion).setZ((sPacketExplosion.getPlayerVelocityZ() * horizontal.getValue().floatValue() / 100.0f));
        }
        doingVelocity = true;
        hudStringColor = Formatting.GREEN;
    }

    public void onPush(EventPush event) {
        if (noPush.getValue()) {
            event.cancel();
        }
    }

    public String getHudInfo() {
        return "H" + horizontal.getValue().floatValue() + "%" + Formatting.GRAY + "," + Formatting.WHITE + "V" + vertical.getValue().floatValue() + "% " + hudStringColor + getVelocityState();
    }
}