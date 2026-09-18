package hack.echo.client.mixin.render;

import com.mojang.blaze3d.platform.NativeImage;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.misc.CapeModule;
import hack.echo.client.utils.ResourceHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static hack.echo.client.utils.Imports.mc;

@Mixin(CapeLayer.class)
public abstract class MixinCapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Set<Identifier> manuallyLoaded = new HashSet<>();

    public MixinCapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Final
    @Shadow
    private HumanoidModel<AvatarRenderState> model;

    @Shadow protected abstract boolean hasLayer(ItemStack stack, EquipmentClientInfo.LayerType layerType);

    // I think tihs will work
    private static void ensureCapeTexture(Identifier id, String capePath) {
        if (manuallyLoaded.contains(id)) return;
        // In standalone mode the resource manager handles it; only load manually for EchoLoader
        // if (mc.getResourceManager().getResource(id).isPresent()) return;

        try (InputStream is = ResourceHelper.getStream("textures/capes/" + capePath + ".png")) {
            if (is == null) return;
            NativeImage image = NativeImage.read(is);
            mc.getTextureManager().register(id, new DynamicTexture(() -> "echo:capes/" + capePath, image));
            manuallyLoaded.add(id);
        } catch (Exception ignored) {}
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void echoware$renderCape(PoseStack matrices, SubmitNodeCollector commands, int light, AvatarRenderState state, float f, float g, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        // Only override if module enabled; otherwise let vanilla proceed
        if (Echo.featureManager == null) return;
        CapeModule module = Echo.featureManager.getFeatureByClass(CapeModule.class);
        if (module == null || !module.isEnabled()) return;
        if (!state.isInvisible) {
            assert mc.player != null;
            if (state.id == mc.player.getId()) {
                if (!this.hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
                    matrices.pushPose();
                    if (this.hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
                        matrices.translate(0.0F, -0.063125F, 0.06875F);
                    }
                    Identifier capeTexture = Identifier.fromNamespaceAndPath(Echo.MOD_ID, "textures/capes/" + module.getCapePath() + ".png");
                    ensureCapeTexture(capeTexture, module.getCapePath());
                    commands.submitModel(
                            this.model,
                            state,
                            matrices,
                            RenderTypes.entitySolid(capeTexture),
                            light,
                            OverlayTexture.NO_OVERLAY,
                            state.outlineColor,
                            null
                    );
                    matrices.popPose();
                }
            }
        }
        ci.cancel();
    }
}
