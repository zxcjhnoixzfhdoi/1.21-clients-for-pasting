package we.devs.opium.api.utilities;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import we.devs.opium.Opium;
import we.devs.opium.client.events.EventMotion;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BlockUtils implements IMinecraft {

    public static void placeBlock(EventMotion event, BlockPos position,  Hand hand) {
        if (!mc.world.getBlockState(position).canReplace(new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getStackInHand(Hand.MAIN_HAND), new BlockHitResult(Vec3d.of(position), Direction.UP, position, false)))) {
            return;
        }
        if (getPlaceableSide(position) == null) {
            return;
        }
        if (Opium.MODULE_MANAGER.isModuleEnabled("Rotations")) {
          //  float[] rot = RotationUtils.getSmoothRotations(RotationUtils.getRotations(position.getX(), position.getY(), position.getZ()), ModuleRotations.INSTANCE.blockSmoothness.getValue().intValue());
          //  RotationUtils.rotate(event, rot);
            Opium.ROTATION_MANAGER.requestRotation(20, Vec3d.of(position),true);
        }
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(Vec3d.of(position.offset(Objects.requireNonNull(getPlaceableSide(position)))), Objects.requireNonNull(getPlaceableSide(position)).getOpposite(), position.offset(Objects.requireNonNull(getPlaceableSide(position))), false), 0));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
    }

    public static boolean isPositionPlaceable(BlockPos position, boolean entityCheck, boolean sideCheck) {
        if (!mc.world.getBlockState(position).getBlock().canReplace(mc.world.getBlockState(position), new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getStackInHand(Hand.MAIN_HAND), new BlockHitResult(Vec3d.of(position), Direction.UP, position, false)))) {
            return false;
        }
        if (entityCheck) {
            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(position), Entity::isAlive)) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) continue;
                return false;
            }
        }
        if (sideCheck) {
            return getPlaceableSide(position) != null;
        }
        return true;
    }

    public static boolean isPositionPlaceable(BlockPos position, boolean entityCheck, boolean sideCheck, boolean ignoreCrystals) {
        if (!mc.world.getBlockState(position).getBlock().canReplace(mc.world.getBlockState(position), new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getStackInHand(Hand.MAIN_HAND), new BlockHitResult(Vec3d.of(position), Direction.UP, position, false)))) {
            return false;
        }
        if (entityCheck) {
            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(position), Entity::isAlive)) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof EndCrystalEntity && ignoreCrystals) continue;
                return false;
            }
        }
        if (sideCheck) {
            return getPlaceableSide(position) != null;
        }
        return true;
    }

    public static boolean surroundPlaceableCheck(BlockPos position, boolean entityCheck, boolean sideCheck) {
        if (!mc.world.getBlockState(position).getBlock().canReplace(mc.world.getBlockState(position), new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getStackInHand(Hand.MAIN_HAND), new BlockHitResult(Vec3d.of(position), Direction.UP, position, false)))) {
            return false;
        }
        if (entityCheck) {
            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(position), Entity::isAlive)) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof EndCrystalEntity) continue;
                return false;
            }
        }
        if (sideCheck) {
            return getPlaceableSide(position) != null;
        }
        return true;
    }



    public static Direction getPlaceableSide(BlockPos position) {
        for (Direction side : Direction.values()) {
            if (!mc.world.getBlockState(position.offset(side)).blocksMovement() || mc.world.getBlockState(position.offset(side)).isLiquid()) continue;
            return side;
        }
        return null;
    }

    public static List<BlockPos> getNearbyBlocks(PlayerEntity player, double blockRange, boolean motion) {
        ArrayList<BlockPos> nearbyBlocks = new ArrayList<>();
        int range = (int)MathUtils.roundToPlaces(blockRange, 0);
        if (motion) {
            player.getPos().add(Vec3d.of(new Vec3i((int) player.getVelocity().x, (int) player.getVelocity().y, (int) player.getVelocity().z)));
        }
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range - range / 2; ++y) {
                for (int z = -range; z <= range; ++z) {
                    nearbyBlocks.add(BlockPos.ofFloored(player.getPos().add(x, y, z)));
                }
            }
        }
        return nearbyBlocks;
    }

    public static BlockResistance getBlockResistance(BlockPos block) {
        if (mc.world.isAir(block)) {
            return BlockResistance.Blank;
        }
        if (!(mc.world.getBlockState(block).getBlock().getHardness() == -1.0f || mc.world.getBlockState(block).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(block).getBlock().equals(Blocks.ANVIL) || mc.world.getBlockState(block).getBlock().equals(Blocks.ENCHANTING_TABLE) || mc.world.getBlockState(block).getBlock().equals(Blocks.ENDER_CHEST))) {
            return BlockResistance.Breakable;
        }
        if (mc.world.getBlockState(block).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(block).getBlock().equals(Blocks.ANVIL) || mc.world.getBlockState(block).getBlock().equals(Blocks.ENCHANTING_TABLE) || mc.world.getBlockState(block).getBlock().equals(Blocks.ENDER_CHEST)) {
            return BlockResistance.Resistant;
        }
        if (mc.world.getBlockState(block).getBlock().equals(Blocks.BEDROCK)) {
            return BlockResistance.Unbreakable;
        }
        return null;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || Objects.requireNonNull(mc.player).getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    /**
     * @see net.minecraft.entity.player.PlayerEntity#getBlockBreakingSpeed(BlockState)
     */
    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        assert mc.player != null;
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);

            int efficiency = getEnchantmentLevel(tool, Enchantments.EFFICIENCY);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            speed *= mc.player.getAttributeValue(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED);
        }

        if (!mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }

    static int getEnchantmentLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
        if (itemStack.isEmpty()) return 0;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        return getEnchantmentLevel(itemEnchantments, enchantment);
    }

    static int getEnchantmentLevel(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantment) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : Object2IntMaps.fastIterable(itemEnchantments)) {
            if (entry.getKey().matchesKey(enchantment)) return entry.getIntValue();
        }
        return 0;
    }

    static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
        enchantments.clear();

        if (!itemStack.isEmpty()) {
            Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
                    ? itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantmentEntries()
                    : itemStack.getEnchantments().getEnchantmentEntries();

            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantments) {
                enchantments.put(entry.getKey(), entry.getIntValue());
            }
        }
    }

    public enum BlockResistance {
        Blank,
        Breakable,
        Resistant,
        Unbreakable
    }
}
