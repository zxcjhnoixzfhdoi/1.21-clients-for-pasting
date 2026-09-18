package hack.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.mixin.accessors.EntityAccessor;
import hack.echo.client.mixin.accessors.LivingEntityAccessor;
import hack.echo.client.utils.Imports;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PlayerSimulation implements Imports {

    private RemotePlayer simulatedPlayer;
    private Player sourcePlayer;

    public PlayerSimulation(Player player) {
        if (mc.level == null || !(mc.level instanceof ClientLevel)) {
            this.sourcePlayer = null;
            this.simulatedPlayer = null;
            return;
        }

        if (player == null) {
            this.sourcePlayer = null;
            this.simulatedPlayer = null;
            return;
        }

        this.sourcePlayer = player;
        this.simulatedPlayer = createSimulatedPlayer((ClientLevel) mc.level, player);
        cloneState();
    }

    private RemotePlayer createSimulatedPlayer(ClientLevel level, Player player) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "echo");

        return new RemotePlayer(level, profile) {
            {
                this.noPhysics = player.noPhysics;
            }

            @Override
            public void push(Entity entity) {
            }
        };
    }

    private void cloneState() {
        if (!isValid()) return;

        Player p = this.sourcePlayer;
        RemotePlayer sim = this.simulatedPlayer;

        sim.setPos(p.getX(), p.getY(), p.getZ());
        sim.setDeltaMovement(p.getDeltaMovement());
        sim.setBoundingBox(p.getBoundingBox());
        sim.setOnGround(p.onGround());
        sim.fallDistance = p.fallDistance;

        float yaw = p.getYRot();
        float pitch = p.getXRot();

        if (p == mc.player && RotationHandler.isHasSilentRotation()) {
            yaw = RotationHandler.getServerYaw();
            pitch = RotationHandler.getServerPitch();
        }

        sim.setYRot(yaw);
        sim.setXRot(pitch);
        sim.setYHeadRot(yaw);
        sim.setYBodyRot(yaw);

        if (p instanceof LivingEntityAccessor src && sim instanceof LivingEntityAccessor dst) {
            dst.setXxa(src.getXxa());
            dst.setZza(src.getZza());
        }

        sim.setSprinting(p.isSprinting());
        sim.setShiftKeyDown(p.isShiftKeyDown());
        sim.setSwimming(p.isSwimming());
        sim.setGlowingTag(p.isCurrentlyGlowing());

        ((EntityAccessor) sim).echo$setSharedFlag(7, p.isFallFlying());

        for (MobEffectInstance effect : p.getActiveEffects()) {
            if (effect != null) {
                sim.addEffect(new MobEffectInstance(effect));
            }
        }
    }

    public boolean isValid() {
        return this.sourcePlayer != null && this.simulatedPlayer != null;
    }

    public void simulateTick() {
        simulateTicks(1);
    }

    public void simulateTicks(int ticks) {
        if (!isValid() || ticks <= 0) return;

        for (int i = 0; i < ticks; i++) {
            this.simulatedPlayer.tick();
        }
    }

    public void resetFromSource() {
        cloneState();
    }

    public RemotePlayer getSimulatedPlayer() {
        return simulatedPlayer;
    }

    public Player getSourcePlayer() {
        return sourcePlayer;
    }

    public Vec3 getSimulatedPosition() {
        return isValid() ? simulatedPlayer.position() : Vec3.ZERO;
    }
}

