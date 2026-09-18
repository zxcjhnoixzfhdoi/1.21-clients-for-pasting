package hack.echo.client.mixin.world;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventBlockUpdate;
import hack.echo.client.event.impl.EventChunkLoad;
import hack.echo.client.event.impl.EventChunkUnload;
import hack.echo.client.api.ChunkPosCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// wowie
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level {


    protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    void onChunkLoad(ChunkPos chunkPos, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        LevelChunk chunk = this.getChunk(ChunkPosCompat.x(chunkPos), ChunkPosCompat.z(chunkPos));
        EventChunkLoad event = new EventChunkLoad(chunk);
        event.call();
    }

    @Inject(method = "unload", at = @At("HEAD"))
    void onChunkUnload(LevelChunk levelChunk, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventChunkUnload event = new EventChunkUnload(levelChunk.getPos());
        event.call();
    }

    @Inject(
            method = "setBlock",
            at = @At("RETURN")
    )
    void onBlockUpdate(BlockPos blockPos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        if (cir.getReturnValue()) {
            EventBlockUpdate event = new EventBlockUpdate(blockPos, blockState);
            event.call();
        }
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    void onServerVerifiedBlockState(BlockPos blockPos, BlockState blockState, int i, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        new EventBlockUpdate(blockPos, blockState).call();
    }

}
