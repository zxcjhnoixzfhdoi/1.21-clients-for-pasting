package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


// Aura monster
public class FastStairsModule extends Feature {

    private static final double JUMP_OFFSET = 1.01;

    public FastStairsModule() {
        super(new FeatureInfo(
                Concat.of("Fast Stairs"),
                Concat.of("Perfectly jump while walking up on stairs"),
                Category.MOVEMENT,
                false)
        );
    }

    @EventSubscribe
    private void onMovement(EventMovementInput e) {
        if (isNull() || !isEnabled()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (mc.player.isDeadOrDying()) return;
        if (!mc.player.onGround()) return;
        if (mc.player.isInWater()) return;
        if (mc.player.isInLava()) return;
        if (!mc.player.isSprinting()) return;
        if (!e.forward) return;
        
        // Calculate forward direction from player yaw
        float yawRad = mc.player.getYRot() * ((float) Math.PI / 180.0F);
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        
        CollisionContext collisionContext = CollisionContext.of(mc.player);
        
        // Check clearance above player first (blocks at Y+1 and Y+2 must be clear for jump)
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getBoundingBox().maxY, mc.player.getZ());
        BlockState aboveBlock1 = mc.level.getBlockState(playerPos.above());
        if (!aboveBlock1.getCollisionShape(mc.level, playerPos.above(), collisionContext).isEmpty()) {
            return;
        }
        
        BlockState aboveBlock2 = mc.level.getBlockState(playerPos.above(2));
        if (!aboveBlock2.getCollisionShape(mc.level, playerPos.above(2), collisionContext).isEmpty()) {
            return;
        }
        
        // This checks the block 1 block ahead at current y/floor/feet level
        double checkX = mc.player.getX() + forwardX;
        double checkZ = mc.player.getZ() + forwardZ;
        BlockPos checkPos = BlockPos.containing(checkX, mc.player.getBlockY(), checkZ);
        
        BlockState blockState = mc.level.getBlockState(checkPos);
        Block block = blockState.getBlock();
        
        if (block instanceof StairBlock) {
            VoxelShape stairsShape = blockState.getCollisionShape(mc.level, checkPos, collisionContext);
            if (!stairsShape.isEmpty()) {
                double maxY = stairsShape.max(Direction.Axis.Y);
                if (maxY >= 0.5) {
                    double blockCenterX = checkPos.getX() + 0.5;
                    double blockCenterZ = checkPos.getZ() + 0.5;
                    double playerX = mc.player.getX();
                    double playerZ = mc.player.getZ();
                    
                    // Horizontal distance
                    double dx = blockCenterX - playerX;
                    double dz = blockCenterZ - playerZ;
                    double distance = Math.sqrt(dx * dx + dz * dz);
          
                    if (distance <= JUMP_OFFSET && mc.player.onGround()) {
                        e.jump = true;
                    }
                }
            }
        }
    }
}
