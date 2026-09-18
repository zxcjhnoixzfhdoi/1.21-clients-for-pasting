package hack.echo.client.utils.player;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import static hack.echo.client.utils.Imports.mc;

public class PlayerIntersectionUtil {
    private static boolean isPlayerInBlock() {
        AABB playerBox = mc.player.getBoundingBox();
        BlockPos playerPos = BlockPos.containing(mc.player.position());
        for (int x = playerPos.getX() - 1; x <= playerPos.getX() + 1; x++) {
            for (int y = playerPos.getY(); y <= playerPos.getY() + 1; y++) {
                for (int z = playerPos.getZ() - 1; z <= playerPos.getZ() + 1; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!mc.level.getBlockState(pos).isAir() && playerBox.intersects(new AABB(pos))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isPlayerInWeb() {
        AABB playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = BlockPos.containing(mc.player.position());

        return getNearbyBlockPositions(playerPosition).stream()
                .anyMatch(pos -> isBlockCobweb(playerBox, pos));
    }

    public static boolean isPlayerInSweetBerryBush(){
        AABB playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = BlockPos.containing(mc.player.position());

        return getNearbyBlockPositions(playerPosition).stream()
                .anyMatch(pos -> isBlockSweetBerryBush(playerBox, pos));
    }


    private static List<BlockPos> getNearbyBlockPositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = center.getX() - 2; x <= center.getX() + 2; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 4; y++) {
                for (int z = center.getZ() - 2; z <= center.getZ() + 2; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    private static boolean isBlockCobweb(AABB playerBox, BlockPos blockPos) {
        return playerBox.intersects(new AABB(blockPos)) && mc.level.getBlockState(blockPos).getBlock() == Blocks.COBWEB;
    }
    private static boolean isBlockSweetBerryBush(AABB playerBox, BlockPos blockPos) {
        return playerBox.intersects(new AABB(blockPos)) && mc.level.getBlockState(blockPos).getBlock() == Blocks.SWEET_BERRY_BUSH;
    }
}
