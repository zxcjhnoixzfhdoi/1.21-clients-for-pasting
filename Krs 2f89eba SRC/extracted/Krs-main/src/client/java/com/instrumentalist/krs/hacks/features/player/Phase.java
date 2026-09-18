package com.instrumentalist.krs.hacks.features.player;

import com.instrumentalist.krs.events.features.BlockEvent;
import com.instrumentalist.krs.events.features.MotionEvent;
import com.instrumentalist.krs.events.features.UpdateEvent;
import com.instrumentalist.krs.events.features.WorldEvent;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.utils.math.TickTimer;
import com.instrumentalist.krs.utils.math.TimerUtil;
import com.instrumentalist.krs.utils.move.MovementUtil;
import com.instrumentalist.krs.utils.packet.PacketUtil;
import com.instrumentalist.krs.utils.value.ListValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class Phase extends Module {
    @Setting
    private final ListValue mode = new ListValue("Mode", new String[]{"NCP", "AAC 4", "Hypixel"}, "NCP");

    private boolean isClipping = false;
    private final TickTimer phaseTimer = new TickTimer();
    private Float cachedDirection = null;

    public Phase() {
        super("Phase", ModuleCategory.Player, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Override
    public String tag() {
        return mode.get();
    }

    @Override
    public void onDisable() {
        isClipping = false;
        phaseTimer.reset();
        cachedDirection = null;

        var player = mc.player;
        if (player == null) return;

        switch (mode.get().toLowerCase(Locale.ROOT)) {
            case "ncp" -> TimerUtil.reset();
            case "aac 4" -> {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                boolean horizontalCollision = player.horizontalCollision;
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(x, y - 0.00000001, z, false, horizontalCollision));
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(x, y - 1, z, false, horizontalCollision));
            }
        }
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        if (mode.get().equalsIgnoreCase("aac 4")) toggle();
    }

    @Override
    public void onWorld(WorldEvent event) {
        isClipping = false;
        phaseTimer.reset();
        cachedDirection = null;
    }

    @Override
    public void onBlock(BlockEvent event) {
        var player = mc.player;
        if (player == null) return;
        if (mode.get().equalsIgnoreCase("hypixel") && isClipping && event.blockPos.getY() != player.blockPosition().below().getY())
            event.cancel();
    }

    @Override
    public void onMotion(MotionEvent event) {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return;

        if (mode.get().equalsIgnoreCase("hypixel")) {
            if (!player.horizontalCollision && !isClipping) return;

            phaseTimer.update();

            if (phaseTimer.hasTimePassed(3)) {
                if (phaseTimer.hasTimePassed(20)) {
                    isClipping = false;
                    phaseTimer.reset();
                    cachedDirection = null;
                }
            } else if (phaseTimer.hasTimePassed(1)) {
                float direction = cachedDirection == null ? (float) Math.toRadians((MovementUtil.getPlayerDirection() % 360 + 360) % 360.0) : cachedDirection;
                double sin = Mth.sin(direction);
                double cos = Mth.cos(direction);

                double xPos = event.x - sin * -0.25;
                double zPos = event.z + cos * -0.25;
                Double closestSurfaceY = null;

                var box = player.getBoundingBox();
                double playerX = box.getCenter().x - sin * -0.25;
                double playerZ = box.getCenter().z + cos * -0.25;
                double playerFeetY = box.minY;

                for (int y = (int) playerFeetY; y >= Math.max((int) playerFeetY - 10, level.getMinY()); y--) {
                    BlockPos blockPos = new BlockPos((int) Math.floor(playerX), y, (int) Math.floor(playerZ));
                    var blockState = level.getBlockState(blockPos);

                    if (!blockState.isAir()) {
                        var shape = blockState.getCollisionShape(level, blockPos);
                        double blockMaxY = Double.NEGATIVE_INFINITY;
                        for (var collisionBox : shape.toAabbs()) {
                            if (collisionBox.maxY > blockMaxY)
                                blockMaxY = collisionBox.maxY;
                        }
                        if (blockMaxY == Double.NEGATIVE_INFINITY)
                            blockMaxY = 1.0;
                        double surfaceY = y + blockMaxY;

                        if (surfaceY <= playerFeetY) {
                            closestSurfaceY = surfaceY;
                            break;
                        }
                    }
                }

                if (phaseTimer.hasTimePassed(2)) {
                    if (closestSurfaceY != null) {
                        event.onGround = true;
                        event.x = xPos;
                        event.y = closestSurfaceY - 0.07;
                        event.z = zPos;
                    }
                    phaseTimer.update();
                } else if (closestSurfaceY != null) {
                    MovementUtil.stopMoving();
                    isClipping = true;
                    if (cachedDirection == null)
                        cachedDirection = direction;
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null) return;

        if (mode.get().equalsIgnoreCase("ncp")) {
            if (player.horizontalCollision) isClipping = true;
            if (!isClipping) return;

            phaseTimer.update();

            if (phaseTimer.hasTimePassed(3)) {
                MovementUtil.stopMoving();
                TimerUtil.reset();
                phaseTimer.reset();
                isClipping = false;
                cachedDirection = null;
            } else if (phaseTimer.hasTimePassed(1)) {
                double offset = phaseTimer.hasTimePassed(2) ? 1.7 : 0.06;
                float direction = cachedDirection == null ? (float) Math.toRadians((MovementUtil.getPlayerDirection() % 360 + 360) % 360.0) : cachedDirection;
                double sin = Mth.sin(direction);
                double cos = Mth.cos(direction);

                Vec3 newPos = new Vec3(
                        player.getX() + (-sin * offset),
                        player.getY(),
                        player.getZ() + (cos * offset)
                );

                TimerUtil.timerSpeed = 0.3f;
                MovementUtil.stopMoving();
                player.setPos(newPos.x, newPos.y, newPos.z);
                if (cachedDirection == null)
                    cachedDirection = direction;
            }
        }
    }
}
