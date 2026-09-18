package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.screens.clickgui.glass.GlassUIConstants;
import hack.echo.client.utils.DrawUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Backtrack extends Feature {
    private static final CharSequence RANGE_SEPARATOR = Concat.of("-");
    private static final CharSequence DELAY_SUFFIX = Concat.of("ms");

    public Backtrack() {
        super(new FeatureInfo(
                Concat.of("Backtrack"),
                Concat.of("Allows you to attack targets at previous positions"),
                Category.COMBAT
        ));
        delayMs.describedBy(Concat.of("How long to delay incoming movement packets for the attacked target."));
        entityRangeBlocks.describedBy(Concat.of("Maximum distance to keep delaying packets for the attacked target."));
        fillColor.describedBy(Concat.of("Fill color for the delayed target position box."));
        outlineColor.describedBy(Concat.of("Outline color for the delayed target position box."));
    }

    private final RangeSetting delayMs = new RangeSetting(Concat.of("Delay"),150f, 250f, 0f, 1000f, 1f, Concat.of(" ms"));
    private final IntSetting entityRangeBlocks = new IntSetting(Concat.of("Range"), 6, 0, 16, Concat.of(" blocks"));
    private final ColorSetting fillColor = new ColorSetting(
            Concat.of("Fill Color"),
            GlassUIConstants.ACCENT_FOR_RENDERING.getRGB(),
            true
    );
    private final ColorSetting outlineColor = new ColorSetting(
            Concat.of("Outline Color"),
            Color.WHITE.getRGB(),
            true
    );

    @Override
    public CharSequence concat() {
        return Concat.of(
            Concat.ofFloat(delayMs.getMinValue()),
            RANGE_SEPARATOR,
            Concat.ofFloat(delayMs.getMaxValue()),
            DELAY_SUFFIX
        );
    }

    private static final class QueuePacket {
        final Packet<?> packet;
        final long timeStamp;

        QueuePacket(Packet<?> packet, long timeStamp) {
            this.packet = packet;
            this.timeStamp = timeStamp;
        }
    }

    private final Queue<QueuePacket> delayedPackets = new ConcurrentLinkedDeque<>();
    private Entity target;
    private Vec3 trackedTargetPosition;
    private VecDeltaCodec trackedTargetPositionCodec;
    private float activeDelayMs;

    @Override
    public void onEnable() {
        delayedPackets.clear();
        clearTrackedTargetPosition();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushPackets(false);
        clearTrackedTargetPosition();
        super.onDisable();
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onAttackEntity(EventOnAttackEntity.Pre event) {
        if (isNull()) return;
        target = event.getTarget();
        clearTrackedTargetPosition();
    }

    private boolean hasValidTarget() {
        if (target == null) return false;
        if (!target.isAlive()) return false;

        double maxDistance = (entityRangeBlocks.getValue());
        return mc.player.distanceToSqr(target) <= (maxDistance * maxDistance);
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onPacketRecieve(EventPacketReceive event) {
        if (isNull()) return;
        if (mc.isSingleplayer()) return;

        Packet<?>  packet = event.getPacket();
        if (packet == null) return;

        if (!hasValidTarget()) {
            flushPackets(false);
            clearTrackedTargetPosition();
            return;
        }

        if (packet instanceof ServerboundChatPacket
        ||  packet instanceof ClientboundSystemChatPacket
        ||  packet instanceof ServerboundChatCommandPacket) {
            return;
        }

        //on tp or dc we need to flush
        if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundDisconnectPacket) {
            flushPackets(false);
            clearTrackedTargetPosition();
            return;
        }

        // Flush on own death
        if (packet instanceof ClientboundSetHealthPacket) {
            if (((ClientboundSetHealthPacket) packet).getHealth() <= 0.0) {
                flushPackets(false);
                clearTrackedTargetPosition();
                return;
            }
        }


        if (packet instanceof ClientboundSoundPacket) {
            if (((ClientboundSoundPacket) packet).getSound().value() == SoundEvents.PLAYER_HURT) {
                return;
            }
        }

        if (activeDelayMs <= 0.0f) {
            activeDelayMs = delayMs.getRandom();
        }

        float currentDelayMs = activeDelayMs;
        if (currentDelayMs <= 0) {
            flushPackets(false);
            return;
        }

        flushPackets(true);

        trackTargetPosition(packet);
        delayedPackets.add(new QueuePacket(packet, System.currentTimeMillis()));
        event.cancel();

        QueuePacket oldest = delayedPackets.peek();
        if (oldest != null && System.currentTimeMillis() - oldest.timeStamp > currentDelayMs) {
            flushPackets(false);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void flushPackets(boolean onlyExpiredPackets) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            delayedPackets.clear();
            clearTrackedTargetPosition();
            return;
        }

        long now = System.currentTimeMillis();

        while (true) {
            QueuePacket queued = delayedPackets.peek();
            if (queued == null) break;

            if (onlyExpiredPackets) {
                long age = now - queued.timeStamp;
                if (age < activeDelayMs) {
                    break;
                }
            }

            delayedPackets.poll();
            try {
                Packet packet = queued.packet;
                packet.handle((PacketListener) connection);
            } catch (Throwable ignored) {
            }
        }

        if (!onlyExpiredPackets) {
            delayedPackets.clear();
            clearTrackedTargetPosition();
            return;
        }

        if (delayedPackets.isEmpty()) {
            clearTrackedTargetPosition();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onRender3D(EventRender3D event) {
        if (isNull()) return;
        if (!hasValidTarget()) {
            clearTrackedTargetPosition();
            return;
        }

        boolean renderFill = fillColor.getAlpha() > 0;
        boolean renderOutline = outlineColor.getAlpha() > 0;
        if (!renderFill && !renderOutline) return;

        AABB actualBox = getTrackedTargetBox();
        if (actualBox == null) return;

        FramebufferTarget draw = event.getDraw3D().getMinecraftTarget();
        if (renderFill) {
            draw.box(
                    actualBox.minX,
                    actualBox.minY,
                    actualBox.minZ,
                    actualBox.getXsize(),
                    actualBox.getYsize(),
                    actualBox.getZsize(),
                    fillColor.getARGB()
            );
        }

        if (renderOutline) {
            DrawUtils.drawBoxOutline(draw, actualBox, outlineColor.getARGB());
        }
    }

    private void trackTargetPosition(Packet<?> packet) {
        if (!hasValidTarget()) return;
        if (mc.level == null) return;

        if (packet instanceof ClientboundMoveEntityPacket movePacket) {
            if (!isTargetMovePacket(movePacket)) return;
            trackMovePacket(movePacket);
            return;
        }

        if (packet instanceof ClientboundEntityPositionSyncPacket syncPacket) {
            if (syncPacket.id() != target.getId()) return;
            setTrackedTargetPosition(syncPacket.values().position());
            return;
        }

        if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            if (teleportPacket.id() != target.getId()) return;
            trackTeleportPacket(teleportPacket);
        }
    }

    private void trackMovePacket(ClientboundMoveEntityPacket packet) {
        ensureTrackedTargetPosition();
        if (trackedTargetPositionCodec == null) return;
        if (!packet.hasPosition()) return;

        Vec3 actualPosition = trackedTargetPositionCodec.decode(
                packet.getXa(),
                packet.getYa(),
                packet.getZa()
        );

        trackedTargetPosition = actualPosition;
        trackedTargetPositionCodec.setBase(actualPosition);
    }

    private void trackTeleportPacket(ClientboundTeleportEntityPacket packet) {
        ensureTrackedTargetPosition();
        if (trackedTargetPosition == null) return;

        Vec3 change = packet.change().position();
        double x = packet.relatives().contains(Relative.X) ? trackedTargetPosition.x + change.x : change.x;
        double y = packet.relatives().contains(Relative.Y) ? trackedTargetPosition.y + change.y : change.y;
        double z = packet.relatives().contains(Relative.Z) ? trackedTargetPosition.z + change.z : change.z;

        setTrackedTargetPosition(new Vec3(x, y, z));
    }

    private void ensureTrackedTargetPosition() {
        if (trackedTargetPosition != null && trackedTargetPositionCodec != null) return;
        if (target == null) return;

        setTrackedTargetPosition(target.getPositionCodec().getBase());
    }

    private void setTrackedTargetPosition(Vec3 position) {
        trackedTargetPosition = position;
        trackedTargetPositionCodec = new VecDeltaCodec();
        trackedTargetPositionCodec.setBase(position);
    }

    private AABB getTrackedTargetBox() {
        if (trackedTargetPosition == null) return null;

        Vec3 boxOffset = trackedTargetPosition.subtract(target.position());
        return target.getBoundingBox().move(boxOffset);
    }

    private boolean isTargetMovePacket(ClientboundMoveEntityPacket packet) {
        Entity movedEntity = packet.getEntity(mc.level);
        if (movedEntity == null) return false;

        return movedEntity.getId() == target.getId();
    }

    private void clearTrackedTargetPosition() {
        trackedTargetPosition = null;
        trackedTargetPositionCodec = null;
        activeDelayMs = 0.0f;
    }
}
