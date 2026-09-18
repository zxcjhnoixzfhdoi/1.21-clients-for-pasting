package hack.echo.client.mixin.render.indigo.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractTerrainRenderContext.class, remap = false)
public class MixinAbstractTerrainRenderContextLight_1_21_11 {

    @Shadow @Final protected BlockRenderInfo blockInfo;

    private boolean echo$isXraySelectedBlock() {
        if (Echo.isDestroyed) return false;
        if (Echo.featureManager == null || this.blockInfo == null) return false;
        BlockState state = this.blockInfo.blockState;
        if (state == null) return false;
        XrayModule xray = XrayModule.getActive();
        return xray != null && xray.shouldRenderBlock(state.getBlock());
    }

    @ModifyVariable(method = "shadeQuad", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean echo$disableAoForXray(boolean ao) {
        return ao && !this.echo$isXraySelectedBlock();
    }

    @Inject(method = "shadeQuad", at = @At("TAIL"))
    private void echo$forceFullbrightForXray(MutableQuadViewImpl quad, boolean ao, boolean emissive, boolean vanillaShade, CallbackInfo ci) {
        if (!this.echo$isXraySelectedBlock()) return;

        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, LightTexture.FULL_BRIGHT);
        }
    }
}
//?} else {
/*public final class MixinAbstractTerrainRenderContextLight_1_21_11 {
}
*///?}
