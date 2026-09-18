package hack.echo.client.mixin.accessors;

//? if >26.1.2 {
/*import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.systems.GpuDevice;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend getBackend();
}
*///?} else {
public interface GpuDeviceAccessor {
    
}
//?}
