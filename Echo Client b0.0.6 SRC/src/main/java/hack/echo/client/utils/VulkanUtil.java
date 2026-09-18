package hack.echo.client.utils;
//? if > 26.1.2 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import hack.echo.client.mixin.accessors.GpuDeviceAccessor;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.client.PreferredGraphicsApi;
*///?}
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public class VulkanUtil implements Imports {
    //? if > 26.1.2 {
    /*public static boolean isVulkanLoaded() {
        return ((GpuDeviceAccessor) RenderSystem.getDevice()).getBackend() instanceof VulkanDevice;
    }
    *///?} else {
    private static Boolean isVulkanLoaded = null;
    public static boolean isVulkanLoaded() {
        if (isVulkanLoaded == null) {
            isVulkanLoaded = FabricLoader.getInstance().isModLoaded("vulkanmod");
        }
        return isVulkanLoaded;
    }
    //?}
}
