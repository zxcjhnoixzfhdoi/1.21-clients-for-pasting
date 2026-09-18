package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.exploits.AntiCactus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void medved$fullCactusCollision(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (AntiCactus.shouldFillCactus()) cir.setReturnValue(Shapes.block());
    }
}
