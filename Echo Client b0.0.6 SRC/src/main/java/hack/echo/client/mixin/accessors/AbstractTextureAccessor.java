package hack.echo.client.mixin.accessors;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractTexture.class)
public interface AbstractTextureAccessor {
    
    @Accessor("sampler")
    @Mutable
    void setSampler(GpuSampler sampler);
    
    @Accessor("sampler")
    GpuSampler getSampler();
}

