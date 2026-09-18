package hack.echo.client.mixin.render.sodium.versioned;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class MixinAbstractBlockRenderContext_26_1 {

    @Shadow
    protected BlockState state;

    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true)
    private void echo$alwaysRenderSelectedBlockFaces(Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        if (this.state == null) {
            return;
        }

        XrayModule xray = XrayModule.getActive();
        if (xray == null) {
            return;
        }

        if (!xray.shouldRenderBlock(this.state.getBlock())) {
            return;
        }

        cir.setReturnValue(false);
    }
}
*///?} else {
public final class MixinAbstractBlockRenderContext_26_1 {
}
//?}
