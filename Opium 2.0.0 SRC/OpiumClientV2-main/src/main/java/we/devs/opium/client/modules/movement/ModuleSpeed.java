package we.devs.opium.client.modules.movement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.FakePlayerEntity;
import we.devs.opium.api.utilities.MovementUtils;
import we.devs.opium.client.events.PlayerMoveEvent;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name = "Speed", description = "Speeds your movement up.", category = Module.Category.MOVEMENT)
public class ModuleSpeed extends Module {
    ValueEnum mode = new ValueEnum("Mode", "Mode", "", modes.Strafe);
    ValueNumber speed = new ValueNumber("Speed", "Speed", "Speed", 1.5f, 1.0f, 2.0f);

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player != null && mc.world != null) {
            if (this.mode.getValue().equals(modes.Strafe)) {

                //sets the default values for speed and slow effect
                double speedEffect = 1.0;
                double slowEffect = 1.0;

                //checks if the player has the speed effect and sets the speed effect, accordingly to the level, to the correct value
                if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                    double amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
                    speedEffect = 1 + (0.2 * (amplifier + 1));
                }
                //checks if the player has the slowness effect and sets the slow effect, accordingly to the level, to the correct value
                if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    double amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
                    slowEffect = 1 + (0.2 * (amplifier + 1));
                }

                //multiplies the base speed by the speed effect and divides it by the slow effect
                final double base = (0.2873f * speedEffect / slowEffect) * speed.getValue().floatValue();
                Vec2f motion = handleVanillaMotion((float) base);

                //sets the speed for the x and z axis' of the Player
                event.setX(motion.x);
                event.setZ(motion.y);

                //Checks if the Player is on the ground and moving
                if (!mc.player.isOnGround() || !MovementUtils.isMoving()) return;

                //Makes the Player Jump with the correct velocity (both are required, already tested it)
                mc.player.jump();
                event.setY(MovementUtils.getJumpBoost() + 0.40123128);

            } else if (this.mode.getValue().equals(modes.GrimStrafe)) {
                int collisions = 0;
                for (Entity entity : mc.world.getEntities()) {
                    if (checkIsCollidingEntity(entity) && MathHelper.sqrt((float) mc.player.squaredDistanceTo(entity)) <= 1.5) {
                        collisions++;
                    }
                }
                if (collisions > 0) {
                    Vec3d velocity = mc.player.getVelocity();
                    double factor = 0.08 * collisions;
                    Vec2f strafe = handleStrafeMotion((float) factor);
                    mc.player.setVelocity(velocity.x + strafe.x, velocity.y, velocity.z + strafe.y);
                }
            }
        }
    }

    public enum modes {
        GrimStrafe,
        Strafe
    }

    public boolean checkIsCollidingEntity(Entity entity) {
        return entity != null && entity != mc.player && entity instanceof LivingEntity
                && !(entity instanceof FakePlayerEntity) && !(entity instanceof ArmorStandEntity);
    }

    public Vec2f handleStrafeMotion(final float speed) {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);
        if (forward == 0.0f && strafe == 0.0f) {
            return Vec2f.ZERO;
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += forward > 0.0f ? -45 : 45;
                strafe = 0.0f;
            } else if (strafe <= -1.0f) {
                yaw += forward > 0.0f ? 45 : -45;
                strafe = 0.0f;
            }
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        float rx = (float) Math.cos(Math.toRadians(yaw));
        float rz = (float) -Math.sin(Math.toRadians(yaw));
        return new Vec2f((forward * speed * rz) + (strafe * speed * rx),
                (forward * speed * rx) - (strafe * speed * rz));
    }

    //handles the Velocity Calculations for Strafing
    public Vec2f handleVanillaMotion(final float speed) {
        assert mc.player != null;
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0.0f && strafe == 0.0f) {
            return Vec2f.ZERO;
        } else if (forward != 0.0f && strafe != 0.0f) {
            forward *= (float) Math.sin(0.7853981633974483);
            strafe *= (float) Math.cos(0.7853981633974483);
        }
        return new Vec2f((float) (forward * speed * -Math.sin(Math.toRadians(mc.player.getYaw())) + strafe * speed * Math.cos(Math.toRadians(mc.player.getYaw()))),
                (float) (forward * speed * Math.cos(Math.toRadians(mc.player.getYaw())) - strafe * speed * -Math.sin(Math.toRadians(mc.player.getYaw()))));
    }
}
