package onl.luka.grizzly.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import onl.luka.grizzly.module.modules.combat.HitSelect;
import onl.luka.grizzly.module.modules.combat.KnockbackDisplacement;
import onl.luka.grizzly.module.modules.player.FakeLag;
import onl.luka.grizzly.module.modules.render.TargetESP;
import onl.luka.grizzly.module.modules.combat.Criticals;
import onl.luka.grizzly.module.modules.exploits.ComboOneHit;
import onl.luka.grizzly.module.modules.player.FastMine;
import onl.luka.grizzly.module.modules.other.TargetFilter;
import onl.luka.grizzly.module.modules.utility.InventoryManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import onl.luka.grizzly.module.modules.combat.HitSwap;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow private int destroyDelay;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void medved$applyFastMineDelay(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        int override = FastMine.INSTANCE.getBreakDelayOverrideOrMinusOne();
        if (override >= 0 && this.destroyDelay > override) {
            this.destroyDelay = override;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void medved$decreaseBreakDelay(CallbackInfo ci) {
        this.destroyDelay = FastMine.INSTANCE.adjustPassiveBreakDelay(this.destroyDelay);
    }

    @ModifyExpressionValue(
        method = "continueDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
        )
    )
    private float medved$multiplyDestroyProgress(float original) {
        return original * FastMine.INSTANCE.getBreakSpeedMultiplier();
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void medved$onAttackSent(Player player, Entity target, CallbackInfo ci) {
        ComboOneHit.onAttack(target);
        TargetFilter.onAttack();
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void medved$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (!(player instanceof LocalPlayer localPlayer)) return;
        if (!(target instanceof LivingEntity living)) return;

        if (KnockbackDisplacement.skipIntercept) {
            try {
                FakeLag.notifyAttack();
                if (Criticals.INSTANCE.isEnabled()) {
                    Criticals.INSTANCE.onAttack(target);
                }
                if (HitSwap.INSTANCE.isEnabled() && !KnockbackDisplacement.isAutoKbAttack()) {
                    HitSwap.INSTANCE.onAttack(player, target);
                }
                InventoryManager.notifyCombat();
                TargetESP.notifyAttack(target);
            } catch (Throwable ignored) {}
            return;
        }

        if (!Criticals.INSTANCE.beforeAttack(localPlayer, living)) {
            ci.cancel();
            return;
        }

        if (HitSelect.shouldCancelAttack()) {
            ci.cancel();
            return;
        }

        if (KnockbackDisplacement.INSTANCE.isEnabled()) {
            if (KnockbackDisplacement.scheduleAttack(localPlayer, living, (MultiPlayerGameMode) (Object) this)) {
                ci.cancel();
                return;
            }
        }

        try {
            FakeLag.notifyAttack();
            if (Criticals.INSTANCE.isEnabled()) {
                Criticals.INSTANCE.onAttack(target);
            }
            if (HitSwap.INSTANCE.isEnabled()) {
                HitSwap.INSTANCE.onAttack(player, target);
            }
            InventoryManager.notifyCombat();
            TargetESP.notifyAttack(target);
        } catch (Throwable ignored) {}
    }
}
