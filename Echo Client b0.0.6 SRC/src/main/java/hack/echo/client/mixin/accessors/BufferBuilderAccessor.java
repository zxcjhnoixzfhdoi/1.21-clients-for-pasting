package hack.echo.client.mixin.accessors;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("format")
    VertexFormat getFormat();
    
    @Accessor("vertexPointer")
    long getVertexPointer();
    
    @Invoker("beginVertex")
    long invokeBeginVertex();
}

