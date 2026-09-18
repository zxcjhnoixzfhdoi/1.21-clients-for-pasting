package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.blocks.BlockUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AutoLava extends Feature {

    public AutoLava() {
        super(new FeatureInfo(
            Concat.of("Auto Lava"),
            Concat.of("Automatically places lava on blocks"),
            Category.UTILITY
        ));
    }


    private boolean hasLavaBucket() {
        return InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.LAVA_BUCKET) != -1;
    }

    @EventSubscribe
    public void onTick(EventHandleInput.Early event) {
        if (isNull()) return;
        if (mc.player.getMainHandItem().getItem() != Items.LAVA_BUCKET) return;
        
        BlockPos targetPos = BlockUtils.getTargetPlacePos();
        if (targetPos == null) return;
        
        if (!BlockUtils.isValidPlacement(targetPos)) return;
        if (!BlockUtils.isBlockPlacingCollideWithEntity(targetPos, Blocks.LAVA)) return;
        
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) return;
        
        BlockPos placeOnPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        Vec3 hitVec = blockHitResult.getLocation();
        
        BlockHitResult placementHitResult = new BlockHitResult(
            hitVec,
            direction,
            placeOnPos,
            blockHitResult.isInside()
        );
        
        HitResult originalHitResult = mc.hitResult;
        mc.hitResult = placementHitResult;
        ((MinecraftAccessor) mc).invokeStartUseItem();
        mc.hitResult = originalHitResult;
    }

}
