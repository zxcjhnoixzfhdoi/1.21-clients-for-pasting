package hack.echo.client.handlers.impl;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.handlers.Handler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HurtTickHandler extends Handler {
    private static final int HURT_TICKS = 10;
    private static final Map<UUID, Long> attackedEntityTicks = new HashMap<>();

    public static long getAttackedEntityTick(LivingEntity entity) {
        if (entity == null) {
            return 0L;
        }
        return attackedEntityTicks.getOrDefault(entity.getUUID(), 0L);
    }

    public static boolean isCurrentlyHurt(LivingEntity entity) {
        if (entity == null || mc.player == null) {
            return false;
        }

        long customRemaining = 0;
        Long lastAttackTick = attackedEntityTicks.get(entity.getUUID());
        if (lastAttackTick != null) {
            long currentTick = mc.player.tickCount;
            long elapsed = currentTick - lastAttackTick;
            if (elapsed >= HURT_TICKS) {
                attackedEntityTicks.remove(entity.getUUID());
            } else {
                customRemaining = HURT_TICKS - elapsed;
            }
        }

        // Vanilla hurtTime adjusted by ping
        int pingTicks = getPingTicks();
        int vanillaRemaining = Math.max(entity.hurtTime - Math.max(pingTicks - 1, 0), 0);

        // If we have custom tracking, use the higher of both so external hits are still caught
        // If no custom tracking, fall back to vanilla hurtTime (external hits only)
        long remaining = customRemaining > 0 ? Math.max(customRemaining, vanillaRemaining) : vanillaRemaining;
        return remaining > 0;
    }

    private static int getPingTicks() {
        if (mc.player == null || mc.getConnection() == null) return 0;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) return 0;
        return info.getLatency() / 50;
    }

    @EventSubscribe
    private void onAttackPost(EventOnAttackEntity.Post event) {
        if (mc.player == null) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target) {
            attackedEntityTicks.put(target.getUUID(), (long) mc.player.tickCount);
        }
    }

    @EventSubscribe
    private void onPacketReceive(EventPacketReceive event) {
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket
                || event.getPacket() instanceof ClientboundRespawnPacket) {
            attackedEntityTicks.clear();
        }
    }
}
