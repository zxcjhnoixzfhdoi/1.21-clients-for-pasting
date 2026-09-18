package dev.ambience.mixin.client;

import dev.ambience.hooks.AnchorMacroHook;
import dev.ambience.hooks.AutoEscHook;
import dev.ambience.hooks.AutoTotemHook;
import dev.ambience.hooks.BreachSwapHook;
import dev.ambience.hooks.CrystalOptimizerHook;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.STapHook;
import dev.ambience.hooks.ShieldStunHook;
import dev.ambience.hooks.WTapHook;
import dev.ambience.util.DebugLogger;
import dev.ambience.util.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinMultiPlayerGameMode {
   @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
   private void ambience$blockUse(ClientPlayerEntity var1, Hand var2, BlockHitResult var3, CallbackInfoReturnable<ActionResult> var4) {
      if (AnchorMacroHook.blocksInteraction() || AutoEscHook.blocksInteraction() || AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         var4.setReturnValue(ActionResult.FAIL);
      }
   }

   @Inject(method = "interactBlock", at = @At("RETURN"))
   private void ambience$debugUseOn(ClientPlayerEntity var1, Hand var2, BlockHitResult var3, CallbackInfoReturnable<ActionResult> var4) {
      DebugLogger.c(
         "useItemOn",
         var2
            + " pos="
            + var3.getBlockPos().toShortString()
            + " face="
            + var3.getSide()
            + String.format(" loc=%.10f,%.10f,%.10f", var3.getPos().x, var3.getPos().y, var3.getPos().z)
            + " result="
            + var4.getReturnValue()
      );
   }

   @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
   private void ambience$blockUseItem(PlayerEntity var1, Hand var2, CallbackInfoReturnable<ActionResult> var3) {
      if (AutoEscHook.blocksInteraction() || AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         var3.setReturnValue(ActionResult.FAIL);
      }
   }

   @Inject(method = "interactItem", at = @At("RETURN"))
   private void ambience$debugUseItem(PlayerEntity var1, Hand var2, CallbackInfoReturnable<ActionResult> var3) {
      if (var3.getReturnValue() != null && ((ActionResult)var3.getReturnValue()).isAccepted()) {
         RotationManager.c();
      }

      DebugLogger.c("useItem", var2 + " result=" + var3.getReturnValue() + " item=" + var1.getStackInHand(var2).getItem());
   }

   @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
   private void ambience$blockAttack(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      if (!AutoTotemHook.blocksInteraction() && !FreecamHook.blocksInteraction()) {
         ShieldStunHook.beforeAttack(var1, var2);
         BreachSwapHook.beforeAttack(var1, var2);
         WTapHook.beforeAttack(var1, var2);
         STapHook.beforeAttack(var1, var2);
      } else {
         var3.cancel();
      }
   }

   @Inject(method = "attackEntity", at = @At("TAIL"))
   private void ambience$crystalOptimizer(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      CrystalOptimizerHook.afterAttack(var2);
   }

   @Inject(method = "attackEntity", at = @At("RETURN"))
   private void ambience$debugAttack(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      BreachSwapHook.afterAttack();
      ShieldStunHook.afterAttack();
      WTapHook.afterAttack();
      STapHook.afterAttack();
      DebugLogger.c("attack", var2.getName().getString() + "#" + var2.getId());
   }

   @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
   private void ambience$blockInteract(PlayerEntity var1, Entity var2, Hand var3, CallbackInfoReturnable<ActionResult> var4) {
      if (AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         var4.setReturnValue(ActionResult.FAIL);
      }
   }

   @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
   private void ambience$blockStartDestroy(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      if (FreecamHook.blocksInteraction()) {
         var3.setReturnValue(false);
      }
   }

   @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
   private void ambience$blockContinueDestroy(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      if (FreecamHook.blocksInteraction()) {
         var3.setReturnValue(false);
      }
   }

   @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
   private void ambience$blockInventoryDuringTotem(int var1, int var2, int var3, SlotActionType var4, PlayerEntity var5, CallbackInfo var6) {
      if (AutoTotemHook.blocksInventoryInput()) {
         var6.cancel();
      }
   }
}
