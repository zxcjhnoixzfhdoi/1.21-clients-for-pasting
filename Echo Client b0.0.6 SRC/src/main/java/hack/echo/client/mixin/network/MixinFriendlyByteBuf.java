package hack.echo.client.mixin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf {

    /**
     * Clamps cursor position values in the range [0.0, 1.0]. To prevent FabricatedPlace
     */
    @Inject(method = "writeBlockHitResult", at = @At("HEAD"), cancellable = true)
    private void clampBlockHitResult(BlockHitResult blockHitResult, CallbackInfo ci) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        Vec3 location = blockHitResult.getLocation();

        double relX = location.x - blockPos.getX();
        double relY = location.y - blockPos.getY();
        double relZ = location.z - blockPos.getZ();

        float clampedX = (float) Math.clamp(relX, 0.0, 1.0);
        float clampedY = (float) Math.clamp(relY, 0.0, 1.0);
        float clampedZ = (float) Math.clamp(relZ, 0.0, 1.0);

        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;

        self.writeBlockPos(blockPos);
        self.writeEnum(direction);
        self.writeFloat(clampedX);
        self.writeFloat(clampedY);
        self.writeFloat(clampedZ);
        self.writeBoolean(blockHitResult.isInside());
        self.writeBoolean(blockHitResult.isWorldBorderHit());

        ci.cancel();
    }
}
