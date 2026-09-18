package hack.echo.client.utils.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class StorageBlockUtils {
    public static boolean isStorageBlock(Block block) {
        return block instanceof AbstractChestBlock || block instanceof BarrelBlock || 
               block instanceof ShulkerBoxBlock || block instanceof HopperBlock || 
               block instanceof DispenserBlock || block instanceof CrafterBlock ||
               block instanceof AbstractFurnaceBlock;
    }

    public static boolean isStorageBlockEntity(BlockEntityType<?> type) {
        return type == BlockEntityType.CHEST
            || type == BlockEntityType.BARREL
            || type == BlockEntityType.SHULKER_BOX
            || type == BlockEntityType.ENDER_CHEST;
    }

    public static Block storageBlockForType(BlockEntityType<?> type) {
        if (type == BlockEntityType.CHEST) return Blocks.CHEST;
        if (type == BlockEntityType.BARREL) return Blocks.BARREL;
        if (type == BlockEntityType.SHULKER_BOX) return Blocks.SHULKER_BOX;
        if (type == BlockEntityType.ENDER_CHEST) return Blocks.ENDER_CHEST;
        return null;
    }
}
