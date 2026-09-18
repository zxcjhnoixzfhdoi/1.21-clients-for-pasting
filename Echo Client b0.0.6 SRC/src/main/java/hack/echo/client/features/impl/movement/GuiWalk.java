package hack.echo.client.features.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.EventPacketSend;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.impl.SprintController;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GuiWalk extends Feature {

    private final Queue<Packet<?>> queuedPackets = new ConcurrentLinkedDeque<>();
    private boolean flushing = false;
    private boolean waitingToFlush = false;
    private int ticksStopped = 0;

    private KeyMapping[] movementKeys;

    private final ModeSetting mode = new ModeSetting(Concat.of("Mode"), Concat.of("Vanilla"),
            Concat.of("Vanilla"),
            Concat.of("Grim")
    );

    @Override
    public void onDisable() {
        super.onDisable();
        flushPackets();
        waitingToFlush = false;
        ticksStopped = 0;
        SprintController.setForceStopSprint(false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        queuedPackets.clear();
        waitingToFlush = false;
        ticksStopped = 0;
    }

    public GuiWalk() {
        super(new FeatureInfo(
                Concat.of("GUI Walk"),
                Concat.of("Lets you walk inside a menu."),
                Category.MOVEMENT
        ));
    }

    private KeyMapping[] getMovementKeys() {
        if (movementKeys == null) {
            movementKeys = new KeyMapping[]{
                    mc.options.keyUp,
                    mc.options.keyRight,
                    mc.options.keyLeft,
                    mc.options.keyJump,
                    mc.options.keyDown,
            };
        }
        return movementKeys;
    }

    @EventSubscribe
    private void onUpdate(EventRender3D event) {
        if (isNull()) return;
        for (KeyMapping key : getMovementKeys()) {
            key.setDown(isKeyPressed(key));
        }
    }

    public static boolean isKeyPressed(KeyMapping key) {
        return InputConstants.isKeyDown(mc.getWindow(), key.getDefaultKey().getValue());
    }

    @EventSubscribe
    private void onPacketSend(EventPacketSend event) {
        if (isNull()) return;
        if (flushing) return;
        if (mode.is("Vanilla")) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerClosePacket ||
        packet instanceof ServerboundChatPacket || packet instanceof ServerboundChatCommandPacket) {
            queuedPackets.add(packet);
            event.cancel();

            waitingToFlush = true;
            ticksStopped = 0;
            SprintController.setForceStopSprint(true);
            return;
        }

        if (waitingToFlush && packet instanceof ServerboundMovePlayerPacket) {
            ticksStopped++;
            // We have to wait 1 tick to ensure the movement event knows that it has stopped since changing a field takes until the next
            // event/tick for an event to recognize it
            if (ticksStopped >= 1) {
                flushPackets();
                waitingToFlush = false;
                // We set this to false which makes it so that the next tick will free the movement of the player
                ticksStopped = 0;
                SprintController.setForceStopSprint(false);
            }
        }
    }

    @EventSubscribe
    private void onTick(EventTick event) {
        if (isNull()) return;

        if (!queuedPackets.isEmpty() && !waitingToFlush && !(hack.echo.client.api.MinecraftCompat.getScreen() instanceof AbstractContainerScreen)) {
            waitingToFlush = true;
            ticksStopped = 0;
            SprintController.setForceStopSprint(true);
        }
    }

    private void flushPackets() {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            queuedPackets.clear();
            return;
        }

        flushing = true;
        while (!queuedPackets.isEmpty()) {
            Packet<?> packet = queuedPackets.poll();
            if (packet != null) {
                connection.send(packet);
            }
        }
        flushing = false;
    }

    @EventSubscribe
    private void onMovement(EventMovementInput event) {
        if (isNull()) return;

        if (waitingToFlush) {
            event.forward = false;
            event.back = false;
            event.left = false;
            event.right = false;
        }
    }
}
