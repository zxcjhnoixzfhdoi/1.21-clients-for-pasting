package hack.echo.client.mixin.render.liquidblockrenderer;

//? if <26.1 {
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LiquidBlockRenderer.class)
public class MixinLiquidBlockRenderer_1_21_11 {
}
//?} else {
/*public final class MixinLiquidBlockRenderer_1_21_11 {
}
*///?}
