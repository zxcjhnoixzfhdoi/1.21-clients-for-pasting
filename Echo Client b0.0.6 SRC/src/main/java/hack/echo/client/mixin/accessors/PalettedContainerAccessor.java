package hack.echo.client.mixin.accessors;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PalettedContainer.class)
public interface PalettedContainerAccessor<T> {

    @Invoker("get")
    T getIndex(int i);

}
