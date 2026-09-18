package onl.luka.grizzly.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import onl.luka.grizzly.module.modules.combat.AutoBlock;
import onl.luka.grizzly.module.modules.movement.Step;
import onl.luka.grizzly.module.modules.render.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void medved$stepHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer player
                && player == Minecraft.getInstance().player) {
            cir.setReturnValue(Step.resolveStepHeight(player, cir.getReturnValue()));
        }
    }

    @Inject(method = "getUseItem", at = @At("RETURN"), cancellable = true)
    private void fakeUseItem(CallbackInfoReturnable<ItemStack> cir) {
        if (AutoBlock.isBlocking && cir.getReturnValue().isEmpty()) {
            LivingEntity self = (LivingEntity)(Object)this;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = self.getItemInHand(hand);

                if (stack.has(DataComponents.BLOCKS_ATTACKS)
                        || stack.get(DataComponents.CONSUMABLE) != null &&
                        stack.get(DataComponents.CONSUMABLE).animation() == ItemUseAnimation.BLOCK) {

                    cir.setReturnValue(stack);
                    return;
                }
            }
        }
    }

    @Inject(method = "isUsingItem", at = @At("RETURN"), cancellable = true)
    private void fakeUsingItem(CallbackInfoReturnable<Boolean> cir) {
        if (AutoBlock.isBlocking && !cir.getReturnValue()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "getCurrentSwingDuration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I"), require = 0)
    private int hookSwingSpeed(int duration) {
        var animations = Animations.INSTANCE;
        return animations.isEnabled() && Minecraft.getInstance().player == (Object) this ? animations.getSwingDuration().getValue() : duration;
    }

}

