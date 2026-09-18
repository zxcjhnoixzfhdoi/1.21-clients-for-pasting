package we.devs.opium.client.modules.combat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.api.utilities.InventoryUtils;
import we.devs.opium.api.utilities.Renderer3d;
import we.devs.opium.api.utilities.RotationsUtil;
import we.devs.opium.api.utilities.TargetUtils;
import we.devs.opium.client.events.EventRender3D;

import java.awt.*;
import java.util.List;

@RegisterModule(name = "AutoCrystalGPT", description = "AutoCrystalGPT", tag = "AutoCrystalGPT", category = Module.Category.COMBAT)
public class GPT_CA extends Module {

    private PlayerEntity target;
    private BlockPos bestCrystalPos; // Best placement position for rendering and placement
    private double bestDamage;       // Highest damage value found

    @Override
    public void onTick() {
        super.onTick();
        if (mc.world == null || mc.player == null || mc.world.getPlayers() == null) {
            return;
        }

        target = TargetUtils.getTarget(6);
        if (target == null) {
            bestCrystalPos = null;
            return;
        }
        // Search for the valid block that gives the most explosion damage.
        bestCrystalPos = getBestDamageBlock(target, 3); // 3-block radius search

        // If a valid best position is found, attempt to place an end crystal there.
        if (bestCrystalPos != null) {
            placeCrystal(bestCrystalPos);
        }
    }

    /**
     * Scans for the best valid crystal placement block (within the given radius)
     * that would deal the highest explosion damage to the target.
     *
     * @param target The target player.
     * @param radius The horizontal radius (in blocks) to scan around the target's feet.
     * @return The block position that maximizes damage, or null if none is found.
     */
    private BlockPos getBestDamageBlock(PlayerEntity target, int radius) {
        // Get the target's feet position.
        BlockPos feetPos = target.getBlockPos();
        // We want to scan one block beneath the feet.
        BlockPos startingPos = feetPos.down();
        BlockPos bestPos = null;
        bestDamage = 0.0;

        // Loop through candidate positions within the given radius.
        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                BlockPos currentPos = startingPos.add(xOffset, 0, zOffset);
                if (isValidCrystalPlacement(currentPos)) {
                    double damage = calculateCrystalDamage(currentPos, target);
                    // Optionally, add a threshold to ensure the damage is worthwhile.
                    if (damage > bestDamage) {
                        bestDamage = damage;
                        bestPos = currentPos;
                    }
                }
            }
        }
        if (bestPos == null) {
            ChatUtils.sendMessage("No valid block found beneath the target!");
        } else {
            // Debug message showing the calculated damage.
            ChatUtils.sendMessage("Best crystal placement found with raw damage: " + bestDamage);
        }
        return bestPos;
    }

    /**
     * Calculates the raw explosion damage an end crystal would do to the target
     * if placed at the given block position.
     *
     * This version checks both the target's base position and its eye position,
     * and returns the higher value.
     *
     * @param crystalPos The block position where the crystal is placed.
     * @param target The entity (player) that would be damaged.
     * @return The calculated raw damage (before armor/resistance reductions).
     */
    private double calculateCrystalDamage(BlockPos crystalPos, Entity target) {
        // Determine the explosion center.
        // End crystal explosions are typically centered at crystalPos + (0.5, 1, 0.5).
        Vec3d explosionPos = new Vec3d(crystalPos.getX() + 0.5, crystalPos.getY() + 1, crystalPos.getZ() + 0.5);

        // The explosion power for an end crystal is 6.0.
        float explosionPower = 6.0F;
        // The explosion radius factor is explosionPower * 2.
        float explosionRadius = explosionPower * 2.0F; // Typically 12.0

        // Calculate damage based on two target positions:
        // 1. The target's base position (target.getPos())
        // 2. The target's eye position (target.getEyePos(1.0F))
        double damageBase = computeDamageForPos(explosionPos, target.getPos(), target.getBoundingBox(), explosionRadius);
        double damageEye = computeDamageForPos(explosionPos, Vec3d.of(target.getBlockPos().up(1)), target.getBoundingBox(), explosionRadius);

        // Return the maximum damage calculated.
        return Math.max(damageBase, damageEye);
    }

    /**
     * Helper method that computes explosion damage given an explosion center and a target position.
     *
     * @param explosionPos The center of the explosion.
     * @param targetPos The position on the target to use for distance calculations.
     * @param box The bounding box of the target.
     * @param explosionRadius The effective explosion radius.
     * @return The calculated raw damage.
     */
    private double computeDamageForPos(Vec3d explosionPos, Vec3d targetPos, Box box, float explosionRadius) {
        double distance = targetPos.distanceTo(explosionPos);
        double normalizedDistance = distance / explosionRadius;
        if (normalizedDistance > 1.0) {
            return 0.0;
        }
        // Use our custom block density method.
        double blockDensity = getBlockDensity(explosionPos, box);
        double impact = (1.0 - normalizedDistance) * blockDensity;
        // Vanilla explosion formula.
        double rawDamage = ((impact * impact + impact) / 2.0) * 7.0 * explosionRadius + 1.0;
        return rawDamage;
    }

    /**
     * Checks if a block position is a valid crystal placement spot.
     * The block must be Bedrock or Obsidian, must have two air blocks above,
     * and the placement space must not be obstructed by any entities.
     *
     * @param pos The candidate block position.
     * @return True if the placement is valid, false otherwise.
     */
    private boolean isValidCrystalPlacement(BlockPos pos) {
        assert mc.world != null;
        BlockState state = mc.world.getBlockState(pos);

        // Must be Obsidian or Bedrock.
        if (!(state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK))) {
            return false;
        }

        // Check that the two blocks above are air.
        if (!mc.world.getBlockState(pos.up()).isAir() || !mc.world.getBlockState(pos.up(2)).isAir()) {
            return false;
        }

        // Also check that the space where the crystal will be placed isn't obstructed by any entities.
        return !isCrystalPlacementObstructed(pos);
    }

    /**
     * Checks if the placement area (the two blocks above the given position) is obstructed by any entities.
     *
     * @param pos The candidate block position (crystal would be placed at pos.up()).
     * @return True if the area is obstructed, false otherwise.
     */
    private boolean isCrystalPlacementObstructed(BlockPos pos) {
        // Define a box covering the space above the block (1x2 area).
        Box placementBox = new Box(
                pos.up().getX(), pos.up().getY(), pos.up().getZ(),
                pos.up(2).getX() + 1, pos.up(2).getY() + 1, pos.up(2).getZ() + 1
        );

        // Get all entities in the area.
        List<Entity> entities = mc.world.getOtherEntities(null, placementBox, e -> true);
        return !entities.isEmpty();
    }

    @Override
    public void onRender3D(EventRender3D event) {
        if (target == null) return;

        // Render the target's position for reference.
        Renderer3d.renderEdged(
                event.getMatrices(),
                new Color(50, 135, 168, 20),
                new Color(50, 135, 168, 255),
                Vec3d.of(target.getBlockPos()),
                new Vec3d(1, 1, 1)
        );

        // Render the best crystal placement position.
        if (bestCrystalPos != null) {
            Renderer3d.renderEdged(
                    event.getMatrices(),
                    new Color(173, 49, 144, 20),
                    new Color(173, 49, 144, 255),
                    Vec3d.of(bestCrystalPos),
                    new Vec3d(1, 1, 1)
            );
        }
    }

    /**
     * Approximates the block density between an explosion position and an entity’s bounding box.
     *
     * @param explosionPos The position of the explosion.
     * @param box The bounding box of the target entity.
     * @return A value between 0.0 (fully obstructed) and 1.0 (completely unobstructed).
     */
    private float getBlockDensity(Vec3d explosionPos, Box box) {
        int samples = 0;
        int unobstructed = 0;

        // Use 3 samples in each dimension (total 27 sample points)
        int steps = 3;
        for (int x = 0; x < steps; x++) {
            for (int y = 0; y < steps; y++) {
                for (int z = 0; z < steps; z++) {
                    double sampleX = box.minX + (box.getLengthX() * (x + 0.5) / steps);
                    double sampleY = box.minY + (box.getLengthY() * (y + 0.5) / steps);
                    double sampleZ = box.minZ + (box.getLengthZ() * (z + 0.5) / steps);
                    Vec3d samplePos = new Vec3d(sampleX, sampleY, sampleZ);
                    samples++;

                    // Perform a raycast from explosionPos to the sample position.
                    if (isUnobstructed(explosionPos, samplePos)) {
                        unobstructed++;
                    }
                }
            }
        }
        return (float) unobstructed / samples;
    }

    /**
     * Checks if the path between two points is unobstructed by blocks.
     *
     * @param start The start point.
     * @param end The end point.
     * @return True if the ray does not hit any blocks, false otherwise.
     */
    private boolean isUnobstructed(Vec3d start, Vec3d end) {
        assert mc.player != null;
        var raycastResult = mc.world.raycast(new net.minecraft.world.RaycastContext(
                start,
                end,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return raycastResult.getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    // =====================================================
    // Methods for placing end crystals at the best block position
    // =====================================================

    /**
     * Checks your inventory for an end crystal and returns the slot index.
     * Returns 40 if an end crystal is in the off-hand, otherwise searches hotbar slots.
     *
     * @return The inventory slot index, or -1 if not found.
     */
    private int getCrystalSlot() {
        if (InventoryUtils.testInOffHand(Items.END_CRYSTAL)) {
            return 40;
        } else {
            return InventoryUtils.findItem(Items.END_CRYSTAL, 0, 9);
        }
    }

    /**
     * Places an end crystal at the given block position.
     *
     * Uses either the off-hand or main-hand based on the available slot.
     *
     * @param pos The block position where the crystal should be placed.
     */
    private void placeCrystal(BlockPos pos) {
        int crystalSlot = getCrystalSlot();
        if (crystalSlot == -1) {
            ChatUtils.sendMessage("Out of End Crystals!");
            this.disable(true);
            return;
        }
        int lastSlot = mc.player.getInventory().selectedSlot;
        // You can use a default direction. Here we use UP.
        Direction placeDirection = Direction.UP;

        // Optional: rotate the player toward the block (using your RotationsUtil).
        RotationsUtil.rotateToBlockPos(pos, false);  // Change second parameter if you want client-side rotation

        if (crystalSlot == 40) {
            // Off-hand placement
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(
                    new Vec3d(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1),
                    placeDirection,
                    pos,
                    true
            ));
        } else {
            // Main-hand placement with silent slot switch.
            InventoryUtils.switchSlot(crystalSlot, true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(
                    new Vec3d(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1),
                    placeDirection,
                    pos,
                    true
            ));
            InventoryUtils.switchSlot(lastSlot, true);
        }
    }
}
