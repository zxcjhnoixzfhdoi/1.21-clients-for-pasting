package hack.echo.client.features.impl.combat;

import hack.echo.client.api.EntityMotionPacketCompat;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class KnockbackDelayModule extends Feature {

    private static final CharSequence RANGE_SEPARATOR = Concat.of("-");
    private static final CharSequence MS_SUFFIX = Concat.of("ms");
    private static final CharSequence HOLDING = Concat.of("Holding");
    private static final CharSequence PCT_SUFFIX = Concat.of("%");

    private final IntSetting chance = new IntSetting(Concat.of("Chance"), 100, 0, 100, PCT_SUFFIX);
    private final RangeSetting airDelay = new RangeSetting(
            Concat.of("Air Delay"), 100f, 250f, 0f, 5000f, 1f, MS_SUFFIX);
    private final RangeSetting groundDelay = new RangeSetting(
            Concat.of("Ground Delay"), 50f, 150f, 0f, 5000f, 1f, MS_SUFFIX);

    public KnockbackDelayModule() {
        super(new FeatureInfo(
                Concat.of("Knockback Delay"),
                Concat.of("Delays incoming knockback packets"),
                Category.COMBAT
        ));
    }

    private static final class QueuedPacket {
        final Packet<?> packet;
        final long timeStamp;

        QueuedPacket(Packet<?> packet, long timeStamp) {
            this.packet = packet;
            this.timeStamp = timeStamp;
        }
    }

    private final Queue<QueuedPacket> delayedPackets = new ConcurrentLinkedDeque<>();
    private boolean active;
    private float activeDelayMs;
    private int lastTargetId = -1;

    @Override
    public CharSequence concat() {
        if (active) return HOLDING;

        boolean inAir = mc.player != null && !mc.player.onGround();
        RangeSetting range = inAir ? airDelay : groundDelay;

        return Concat.of(
                Concat.ofInt((int) range.getMinValue()),
                RANGE_SEPARATOR,
                Concat.ofInt((int) range.getMaxValue()),
                MS_SUFFIX
        );
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushAll();
        resetState();
        super.onDisable();
    }

    private void resetState() {
        delayedPackets.clear();
        active = false;
        activeDelayMs = 0f;
        lastTargetId = -1;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onAttackEntity(EventOnAttackEntity.Pre event) {
        if (isNull()) return;
        if (event.getTarget() == null) return;
        lastTargetId = event.getTarget().getId();
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onTick(EventTick event) {
        if (!active) return;

        if (isLastTargetUnavailable()) {
            flushAll();
            lastTargetId = -1;
            return;
        }

        QueuedPacket oldest = delayedPackets.peek();
        if (oldest == null) {
            active = false;
            activeDelayMs = 0f;
            return;
        }
        if (System.currentTimeMillis() - oldest.timeStamp >= activeDelayMs) {
            flushAll();
        }
    }

    private boolean isLastTargetUnavailable() {
        if (lastTargetId == -1) return false;
        if (mc.level == null) return true;

        Entity entity = mc.level.getEntity(lastTargetId);
        if (entity == null) return true;
        if (!entity.isAlive()) return true;
        if (entity.isRemoved()) return true;

        if (entity instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) return true;
            return !TargetUtils.isTargetAllowed(living);
        }

        return false;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onPacketReceive(EventPacketReceive event) {
        if (isNull()) return;
        if (mc.isSingleplayer()) return;

        Packet<?> packet = event.getPacket();
        if (packet == null) return;

        if (packet instanceof ServerboundChatPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ServerboundChatCommandPacket) {
            return;
        }

        if (packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundDisconnectPacket) {
            flushAll();
            return;
        }

        if (packet instanceof ClientboundSetHealthPacket healthPacket
                && healthPacket.getHealth() <= 0f) {
            flushAll();
            return;
        }

        boolean removingLastTarget = false;
        if (packet instanceof ClientboundRemoveEntitiesPacket removePacket
                && lastTargetId != -1
                && removePacket.getEntityIds().contains(lastTargetId)) {
            lastTargetId = -1;
            removingLastTarget = true;
        }

        if (!active && isSelfKnockback(packet)) {
            if (rollChance()) {
                active = true;
                activeDelayMs = pickDelay();
            }
        }

        if (!active) return;

        delayedPackets.add(new QueuedPacket(packet, System.currentTimeMillis()));
        event.cancel();

        if (removingLastTarget) {
            flushAll();
            return;
        }

        QueuedPacket oldest = delayedPackets.peek();
        if (oldest != null && System.currentTimeMillis() - oldest.timeStamp >= activeDelayMs) {
            flushAll();
        }
    }

    private boolean rollChance() {
        int value = chance.getValue();
        if (value >= 100) return true;
        if (value <= 0) return false;
        return Math.random() * 100.0 < value;
    }

    private float pickDelay() {
        boolean inAir = mc.player != null && !mc.player.onGround();
        return (inAir ? airDelay : groundDelay).getRandom();
    }

    private boolean isSelfKnockback(Packet<?> packet) {
        if (mc.player == null) return false;

        if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            return EntityMotionPacketCompat.id(motion) == mc.player.getId();
        }

        if (packet instanceof ClientboundExplodePacket explode) {
            return explode.playerKnockback().isPresent();
        }

        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void flushAll() {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            delayedPackets.clear();
            active = false;
            activeDelayMs = 0f;
            return;
        }

        while (true) {
            QueuedPacket queued = delayedPackets.poll();
            if (queued == null) break;
            try {
                Packet packet = queued.packet;
                packet.handle((PacketListener) connection);
            } catch (Throwable ignored) {
            }
        }

        active = false;
        activeDelayMs = 0f;
    }
}
