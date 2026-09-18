package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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

@Mixin(ClientPlayerInteractionManager.class)
public class MixinMultiPlayerGameModeInject extends ClientPlayerInteractionManager {
   private MixinMultiPlayerGameModeInject(MinecraftClient var1, ClientPlayNetworkHandler var2) {
      super(var1, var2);
   }

   @Inject(method = "interactBlock", at = "HEAD", cancellable = true, captureArgs = true)
   public static void blockUse(ClientPlayerInteractionManager var0, ClientPlayerEntity var1, Hand var2, BlockHitResult var3) {
      if (AnchorMacroHook.blocksInteraction() || AutoEscHook.blocksInteraction() || AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         Cancel.cancel(ActionResult.FAIL);
      }
   }

   @Inject(method = "interactBlock", at = "RETURN", captureArgs = true, captureReturn = true)
   public static void debugUseOn(ClientPlayerInteractionManager var0, ClientPlayerEntity var1, Hand var2, BlockHitResult var3, ActionResult var4) {
      DebugLogger.c(
         "useItemOn",
         var2
            + " pos="
            + var3.getBlockPos().toShortString()
            + " face="
            + var3.getSide()
            + String.format(" loc=%.10f,%.10f,%.10f", var3.getPos().x, var3.getPos().y, var3.getPos().z)
            + " result="
            + var4
      );
   }

   @Inject(method = "interactItem", at = "HEAD", cancellable = true, captureArgs = true)
   public static void blockUseItem(ClientPlayerInteractionManager var0, PlayerEntity var1, Hand var2) {
      if (AutoEscHook.blocksInteraction() || AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         Cancel.cancel(ActionResult.FAIL);
      }
   }

   @Inject(method = "interactItem", at = "RETURN", captureArgs = true, captureReturn = true)
   public static void debugUseItem(ClientPlayerInteractionManager var0, PlayerEntity var1, Hand var2, ActionResult var3) {
      if (var3 != null && var3.isAccepted()) {
         RotationManager.c();
      }

      DebugLogger.c("useItem", var2 + " result=" + var3 + " item=" + var1.getStackInHand(var2).getItem());
   }

   @Inject(at = "HEAD", cancellable = true)
   public void attackEntity(PlayerEntity var1, Entity var2) {
      if (!AutoTotemHook.blocksInteraction() && !FreecamHook.blocksInteraction()) {
         ShieldStunHook.beforeAttack(var1, var2);
         BreachSwapHook.beforeAttack(var1, var2);
         WTapHook.beforeAttack(var1, var2);
         STapHook.beforeAttack(var1, var2);
      } else {
         Cancel.cancel();
      }
   }

   @Inject(method = "attackEntity", at = "RETURN", captureArgs = true)
   public static void afterAttack(ClientPlayerInteractionManager var0, PlayerEntity var1, Entity var2) {
      CrystalOptimizerHook.afterAttack(var2);
      BreachSwapHook.afterAttack();
      ShieldStunHook.afterAttack();
      WTapHook.afterAttack();
      STapHook.afterAttack();
      DebugLogger.c("attack", var2.getName().getString() + "#" + var2.getId());
   }

   @Inject(method = "interactEntity", at = "HEAD", cancellable = true, captureArgs = true)
   public static void blockInteract(ClientPlayerInteractionManager var0, PlayerEntity var1, Entity var2, Hand var3) {
      if (AutoTotemHook.blocksInteraction() || FreecamHook.blocksInteraction()) {
         Cancel.cancel(ActionResult.FAIL);
      }
   }

   @Inject(method = "attackBlock", at = "HEAD", cancellable = true, captureArgs = true)
   public static void blockStartDestroy(ClientPlayerInteractionManager var0, BlockPos var1, Direction var2) {
      if (FreecamHook.blocksInteraction()) {
         Cancel.cancel(Boolean.FALSE);
      }
   }

   @Inject(method = "updateBlockBreakingProgress", at = "HEAD", cancellable = true, captureArgs = true)
   public static void blockContinueDestroy(ClientPlayerInteractionManager var0, BlockPos var1, Direction var2) {
      if (FreecamHook.blocksInteraction()) {
         Cancel.cancel(Boolean.FALSE);
      }
   }

   @Inject(at = "HEAD", cancellable = true)
   public void clickSlot(int var1, int var2, int var3, SlotActionType var4, PlayerEntity var5) {
      if (AutoTotemHook.blocksInventoryInput()) {
         Cancel.cancel();
      }
   }
}
