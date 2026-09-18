package com.instrumentalist.krs.hacks.features.player;



import com.instrumentalist.krs.events.features.*;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.utils.ChatUtil;
import com.instrumentalist.krs.utils.entity.EntityExtension;
import com.instrumentalist.krs.utils.packet.PacketUtil;
import com.instrumentalist.krs.utils.pathfinder.MainPathFinder;
import com.instrumentalist.krs.utils.render.RenderUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Stalker extends Module {

    public Stalker() {
        super("Stalker", ModuleCategory.Player, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    private int targetIndex = 0;
    private Player currentTarget = null;
    private final List<AbstractClientPlayer> players = new ArrayList<>();

    @Override
    public String description() {
        return "Switch target with left and right key";
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onWorld(WorldEvent event) {
        reset();
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (players == null || players.isEmpty()) {
            currentTarget = null;
            updatePlayerList();
            return;
        }

        if (currentTarget == null || currentTarget.isRemoved() || !players.contains((AbstractClientPlayer) currentTarget)) {
            updatePlayerList();
            selectClosestTarget();
        }

        if (currentTarget == null) return;

        if (EntityExtension.boundingDistanceTo(mc.player, currentTarget) >= 1.2f) {
            ArrayList<Vec3> paths = MainPathFinder.computePath(mc.player.position(), currentTarget.position());
            if (paths == null || paths.isEmpty()) return;

            for (Vec3 path : paths) {
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(path.x, path.y, path.z, true, mc.player.horizontalCollision));
            }
        }

        mc.player.setPos(currentTarget.position());
    }

    private void updatePlayerList() {
        players.clear();
        for (AbstractClientPlayer player : mc.level.players()) {
            if (!player.isRemoved() && !(player instanceof LocalPlayer))
                players.add(player);
        }
        players.sort(Comparator.comparingDouble(player -> mc.player.distanceToSqr(player)));
        if (targetIndex >= players.size())
            targetIndex = 0;
    }

    @Override
    public void onKey(KeyboardEvent event) {
        if (mc.player == null || mc.level == null || event.action != GLFW.GLFW_PRESS) return;

        if (event.key == GLFW.GLFW_KEY_LEFT) {
            selectPreviousTarget();
        } else if (event.key == GLFW.GLFW_KEY_RIGHT) {
            selectNextTarget();
        }
    }

    private void reset() {
        targetIndex = 0;
        currentTarget = null;
        players.clear();
    }

    private void selectClosestTarget() {
        if (!players.isEmpty()) {
            targetIndex = 0;
            currentTarget = players.get(targetIndex);
        }
    }

    private void selectNextTarget() {
        if (!players.isEmpty()) {
            targetIndex = (targetIndex + 1) % players.size();
            currentTarget = players.get(targetIndex);
        }
    }

    private void selectPreviousTarget() {
        if (!players.isEmpty()) {
            targetIndex = (targetIndex - 1 + players.size()) % players.size();
            currentTarget = players.get(targetIndex);
        }
    }
}
