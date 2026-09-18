package hack.echo.client.mixin.render.sodium.versioned;

//? if <26.1 {
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public class MixinDefaultFluidRenderer_1_21_11 {
}
//?} else {
/*public final class MixinDefaultFluidRenderer_1_21_11 {
}
*///?}
