package hack.echo.client.mixin.render;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.ViewModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void echo$applyViewModelTransform(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        ViewModel viewModel = Echo.featureManager.getFeatureByClass(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;

        float posX = hand == InteractionHand.MAIN_HAND ? viewModel.posXMain.getValue() : viewModel.posXOff.getValue();
        float posY = hand == InteractionHand.MAIN_HAND ? viewModel.posYMain.getValue() : viewModel.posYOff.getValue();
        float posZ = hand == InteractionHand.MAIN_HAND ? viewModel.posZMain.getValue() : viewModel.posZOff.getValue();

        float rotX = hand == InteractionHand.MAIN_HAND ? viewModel.rotXMain.getValue() : viewModel.rotXOff.getValue();
        float rotY = hand == InteractionHand.MAIN_HAND ? viewModel.rotYMain.getValue() : viewModel.rotYOff.getValue();
        float rotZ = hand == InteractionHand.MAIN_HAND ? viewModel.rotZMain.getValue() : viewModel.rotZOff.getValue();

        float s = hand == InteractionHand.MAIN_HAND ? viewModel.scaleMain.getValue() : viewModel.scaleOff.getValue();
        matrices.translate(posX, posY, posZ);

        matrices.mulPose(Axis.XP.rotationDegrees(rotX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
        matrices.scale(s, s, s);

    }

    @Inject(
        method = "swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void echo$applySwingMode(float swingProgress, PoseStack matrixStack, int i, HumanoidArm arm, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        ViewModel viewModel = Echo.featureManager.getFeatureByClass(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;

        ci.cancel();
        viewModel.getSelectedMode().apply(swingProgress, matrixStack, i, arm);
    }

    @Inject(
        method = "applyEatTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void echo$modifyEatingBob(PoseStack matrices, float tickProgress, HumanoidArm arm, ItemStack stack, Player player, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        ViewModel viewModel = Echo.featureManager.getFeatureByClass(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;

        if (!viewModel.eating.getValue()) {
            ci.cancel();
            return;
        }

        float intensity = Mth.clamp(viewModel.eatingBob.getValue(), 0.0F, 2.0F);
        if (Mth.equal(intensity, 1.0F)) {
            return;
        }

        ci.cancel();

        float f = player.getUseItemRemainingTicks() - tickProgress + 1.0F;
        float g = f / stack.getUseDuration(player);
        if (g < 0.8F) {
            float hBob = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F) * intensity;
            matrices.translate(0.0F, hBob, 0.0F);
        }

        float h = (1.0F - (float) Math.pow(g, 27.0)) * intensity;
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * i, h * -0.5F, 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(i * h * 90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(h * 10.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(i * h * 30.0F));
    }








}
