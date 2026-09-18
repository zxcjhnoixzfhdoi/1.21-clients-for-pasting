package onl.luka.grizzly.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorInvoker {
    @Invoker("innerFill")
    void grizzly$innerFill(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        int x0,
        int y0,
        int x1,
        int y1,
        int color,
        Integer color2
    );
}
