package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockBruteforcer extends Feature {

    public BlockBruteforcer() {
        super(new FeatureInfo(
                Concat.of("Block Bruteforcer"),
                Concat.of("Reveals obfuscated blocks by bruteforcing each block"),
                Category.UTILITY
        ));
    }

    private final IntSetting radius = new IntSetting(Concat.of("Radius"), 5, 1, 8);
    private final IntSetting up = new IntSetting(Concat.of("Up"), 3, 0, 64);
    private final IntSetting down = new IntSetting(Concat.of("Down"), 3, 0, 64);
    private final IntSetting delay = new IntSetting(Concat.of("Delay"), 0, 0, 10);
    private final IntSetting blocksPerTick = new IntSetting(Concat.of("Blocks Per Tick"), 4, 1, 5);
    private final BoolSetting freezeMovement = new BoolSetting(Concat.of("Freeze Movement"), true);
    private final ModeSetting mode = new ModeSetting(Concat.of("Mode"), Concat.of("Abort"), Concat.of("Abort"), Concat.of("Start"));

    private final List<BlockPos> toCheck = new ArrayList<>();
    private final Set<BlockPos> checked = new HashSet<>();
    private final Set<BlockPos> revealed = new HashSet<>();

    private int tickCounter = 0;
    private boolean scanning = false;
    private BlockPos scanOrigin;

    @Override
    public void onDisable() {
        super.onDisable();
        toCheck.clear();
        checked.clear();
        revealed.clear();
        scanning = false;
        scanOrigin = null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        toCheck.clear();
        checked.clear();
        revealed.clear();
        startScan();
    }

    @EventSubscribe
    private void onPacketReceive(EventPacketReceive event) {
        if (isNull()) return;

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            setEnabled(false);
            return;
        }

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
            BlockPos pos = packet.getPos();
            if (checked.contains(pos)) {
                revealed.add(pos);
            }
        }
    }

    @EventSubscribe
    private void onTick(EventTick event) {
        if (isNull()) {
            setEnabled(false);
            return;
        }

        if (scanning) {
            processScan();
        }
    }

    @EventSubscribe
    private void on3dRender(EventRender3D event) {
        if (isNull() || scanOrigin == null) return;

        int r = radius.getValue();
        int u = up.getValue();
        int d = down.getValue();

        FramebufferTarget draw = event.getDraw3D().getMinecraftTarget();

        double minX = scanOrigin.getX() - r;
        double minY = scanOrigin.getY() - d;
        double minZ = scanOrigin.getZ() - r;
        double maxX = scanOrigin.getX() + r + 1;
        double maxY = scanOrigin.getY() + u + 1;
        double maxZ = scanOrigin.getZ() + r + 1;

        int outlineColor = ARGB.colorFromFloat(0.8f, 1.0f, 1.0f, 1.0f);

        // Bottom face edges
        draw.line(minX, minY, minZ, maxX, minY, minZ, outlineColor);
        draw.line(maxX, minY, minZ, maxX, minY, maxZ, outlineColor);
        draw.line(maxX, minY, maxZ, minX, minY, maxZ, outlineColor);
        draw.line(minX, minY, maxZ, minX, minY, minZ, outlineColor);

        // Top face edges
        draw.line(minX, maxY, minZ, maxX, maxY, minZ, outlineColor);
        draw.line(maxX, maxY, minZ, maxX, maxY, maxZ, outlineColor);
        draw.line(maxX, maxY, maxZ, minX, maxY, maxZ, outlineColor);
        draw.line(minX, maxY, maxZ, minX, maxY, minZ, outlineColor);

        // Vertical edges
        draw.line(minX, minY, minZ, minX, maxY, minZ, outlineColor);
        draw.line(maxX, minY, minZ, maxX, maxY, minZ, outlineColor);
        draw.line(maxX, minY, maxZ, maxX, maxY, maxZ, outlineColor);
        draw.line(minX, minY, maxZ, minX, maxY, maxZ, outlineColor);

    }

    @EventSubscribe
    private void onMove(EventMovementInput e) {
        if (isNull()) return;
        if (!scanning || !freezeMovement.getValue()) return;
        e.left = false;
        e.right = false;
        e.forward = false;
        e.back = false;
    }

    private void startScan() {
        scanning = true;
        toCheck.clear();
        checked.clear();

        BlockPos playerPos = mc.player.blockPosition();
        scanOrigin = playerPos;
        int r = radius.getValue();
        int u = up.getValue();
        int d = down.getValue();

        // layer by layer from bottom to top
        for (int y = -d; y <= u; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    if (!mc.level.isEmptyBlock(pos) && !checked.contains(pos)) {
                        toCheck.add(pos);
                    }
                }
            }
        }
    }

    private void processScan() {
        if (toCheck.isEmpty()) {
            scanning = false;
            setEnabled(false);
            return;
        }

        if (mc.getConnection() == null && mc.gameMode == null) {
            scanning = false;
            return;
        }

        tickCounter++;
        if (tickCounter < delay.getValue()) {
            return;
        }
        tickCounter = 0;

        int count = Math.min(blocksPerTick.getValue(), toCheck.size());
        for (int i = 0; i < count; i++) {
            BlockPos pos = toCheck.remove(0);
            checked.add(pos);
            if (mode.is(Concat.of("Start"))) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            } else if  (mode.is(Concat.of("Abort"))) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public Set<BlockPos> getRevealedBlocks() {
        return revealed;
    }

    public boolean isScanning() {
        return scanning;
    }

    public int getProgress() {
        int total = toCheck.size() + checked.size();
        return total == 0 ? 100 : (checked.size() * 100) / total;
    }
}
