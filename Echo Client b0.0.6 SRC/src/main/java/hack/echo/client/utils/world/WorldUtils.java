package hack.echo.client.utils.world;


import hack.echo.client.api.SlimeCompat;
import hack.echo.client.utils.ChatUtils;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static hack.echo.client.utils.Imports.mc;

public class WorldUtils {

    /**
     * Checks if the target is using an item with BLOCK action (attempting to block).
     * This is faster than isBlocking() as it doesn't wait for the 5-tick delay.
     */
    public static boolean isAttemptingToBlock(Player target) {
        if (target == null || !target.isUsingItem()) {
            return false;
        }
        ItemStack activeStack = target.getUseItem();
        if (activeStack.isEmpty()) {
            return false;
        }
        Item item = activeStack.getItem();
        return item.getUseAnimation(activeStack) == ItemUseAnimation.BLOCK;
    }

    public static boolean isShieldFacingAway(Player target) {
        if (mc.player == null || target == null) return false;

        // Use the same facing check as LivingEntity#getDamageBlockedAmount.
        // product is negative the player is behind the target (shield facing away).
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = target.position();

        Vec3 toPlayer = playerPos.subtract(targetPos);
        Vec3 toPlayerHorizontal = new Vec3(toPlayer.x, 0.0, toPlayer.z);
        if (toPlayerHorizontal.lengthSqr() == 0.0) return false;
        toPlayerHorizontal = toPlayerHorizontal.normalize();

        Vec3 facing = target.calculateViewVector(0.0F, target.getYHeadRot());

        double dot = facing.dot(toPlayerHorizontal);
        return dot < 0.0;
    }

    /*
     * Get nearest enemy player
     */
    public static Player getNearestEnemyPlayer() {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : mc.level.players()) {
            if (player == mc.player || player.isDeadOrDying() || player.isSpectator()) {
                continue;
            }
            double distance = mc.player.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        return nearestPlayer;
    }

    public static LivingEntity getNearestLivingEntity(int range) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        LivingEntity nearestEntity = null;
        double nearestDistance = Double.MAX_VALUE;
        AABB searchBox = mc.player.getBoundingBox().inflate(range);

        java.util.List<LivingEntity> entities = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != mc.player && !e.isDeadOrDying()
        );

        for (LivingEntity entity : entities) {
            double distance = mc.player.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEntity = entity;
            }
        }
        return nearestEntity;
    }

    /*
     * Outputs the vertical distance between the player (user) and the other player
     */
    public static double getVerticalDistance(Player target) {
        if (mc.player == null || target == null) {
            return 0;
        }
        return mc.player.getY() - target.getY();
    }

    /*
     * Outputs the horizontal distance between the player (user) and the other player
     */
    public static double getHorizontalDistance(Player target) {
        if (mc.player == null || target == null) {
            return 0;
        }
        double dx = mc.player.getX() - target.getX();
        double dz = mc.player.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /*
     * Outputs distance between the player (user) and the other player
     */
    public static double getDistance(Player target) {
        if (mc.player == null || target == null) {
            return 0;
        }
        double dx = mc.player.getX() - target.getX();
        double dy = mc.player.getY() - target.getY();
        double dz = mc.player.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /*
     * Checks distance to ground
     */
    public static double distanceToGround() {
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        double y = mc.player.getY();
        int blockY = (int) Math.floor(y);
        for (int i = blockY; i >= 0; i--) {
            if (!mc.level.getBlockState(mc.player.blockPosition().atY(i)).isAir()) {
                return y - i - 1;
            }
        }
        return y;
    }

    /**
     * Checks if an entity is a player entity
     */
    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    /**
     * Checks if an entity is a hostile mob
     */
    public static boolean isHostile(Entity entity) {
        if (entity instanceof Monster) return !(entity instanceof ZombifiedPiglin zombiePiglinEntity)
                || zombiePiglinEntity.isAggressive();
        if (SlimeCompat.isTinySlime(entity)) return false;
        // Might add more conditions in the future
        return entity instanceof Enemy
                // Panda, Polar bear, Wolf etc
                || entity instanceof Mob mobEntity && mobEntity.isAggressive();
    }

    /**
     * Checks if an entity is a passive mob
     */
    public static boolean isPassive(Entity entity) {
        return entity instanceof AgeableMob;
    }

    /**
     * Checks if an entity is a neutral mob
     */
    public static boolean isNeutral(Entity entity) {
        return entity instanceof Mob
                && !isHostile(entity)
                && !isPassive(entity);
    }

    /**
     * Checks if a living entity is naked (has no armor equipped)
     * Ignores elytra
     */
    public static boolean isNaked(LivingEntity entity) {
        if (entity == null) return false;
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && (chest.isEmpty() || chest.getItem() == Items.ELYTRA)
                && entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }
}
