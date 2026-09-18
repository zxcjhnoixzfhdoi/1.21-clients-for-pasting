package hack.echo.client.mixin;

import hack.echo.client.utils.VulkanUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class VulkanMixinPlugin implements IMixinConfigPlugin {

    private static final String VULKAN_MIXIN_PACKAGE = "hack.echo.client.mixin.render.vulkan.";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(VULKAN_MIXIN_PACKAGE)) {
            return true;
        }

        //? if > 26.1.2 {
        /*return true;
        *///?} else {
        return VulkanUtil.isVulkanLoaded();
        //?}
    }

    @Override public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
