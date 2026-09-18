package hack.echo.client.mixin.client.minecraft;

//? if >26.1.2 {

/*import hack.echo.client.Echo;
import hack.echo.client.event.EventManager;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.impl.player.FastPlace;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.screens.clickgui.glass.GlassUIConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static hack.echo.client.utils.Imports.mc;

@Mixin(Minecraft.class)
public class MixinMinecraft_26_2 {

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Final
    private DeltaTracker.Timer deltaTracker;

    @Shadow
    @Final
    public Options options;

    @Shadow
    private int rightClickDelay;

    private ClientLevel lastFiredLevel;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (!Echo.isDestroyed && this.level != null && this.player != null && this.level != this.lastFiredLevel) {
            this.lastFiredLevel = this.level;
            EventLevelChange event = new EventLevelChange();
            event.call();
        }

        EventTick event = new EventTick();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    public void run(CallbackInfo ci) {
        GlassUIConstants.syncTheme();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        if (mc.level == null) {
            return;
        }
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceGameTime(J)I",
                    shift = At.Shift.AFTER
            )
    )
    private void onSystemUpdate(boolean tick, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (mc.level == null) {
            return;
        }

        float dynamicDelta = this.deltaTracker.getGameTimeDeltaTicks();
        float tickProgress = this.deltaTracker.getGameTimeDeltaPartialTick(false);
        float fixedDelta = this.deltaTracker.getRealtimeDeltaTicks();

        EventSystemUpdate event = new EventSystemUpdate(tick, dynamicDelta, tickProgress, fixedDelta);
        event.call();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        EventStartAttack event;
        if (this.hitResult == null) {
            event = new EventStartAttack(this.player, null);
        } else if (this.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) this.hitResult).getEntity();
            event = new EventStartAttack(this.player, targetEntity);
        } else {
            event = new EventStartAttack(this.player, null);
        }
        event.call();

        if (!event.cancelled) {
            return;
        }

        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    public void onBlockBreaking(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventBlockBreaking event = new EventBlockBreaking.Pre();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("RETURN"))
    public void onBlockBreakingPost(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventBlockBreaking event = new EventBlockBreaking.Post();
        event.call();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onDoItemUsePre(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (player == null) {
            return;
        }

        EventStartUseItem.Pre event = new EventStartUseItem.Pre(
                player,
                InteractionHand.MAIN_HAND,
                player.getMainHandItem(),
                hitResult
        );
        event.call();

        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void onDoItemUsePost(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (player == null) {
            return;
        }

        EventStartUseItem.Post event = new EventStartUseItem.Post(
                player,
                InteractionHand.MAIN_HAND,
                player.getMainHandItem(),
                hitResult
        );
        event.call();
    }

    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I",
                    shift = At.Shift.AFTER
            )
    )
    private void modifyPlaceDelay(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (Echo.featureManager == null) {
            return;
        }

        FastPlace fastPlace = Echo.featureManager.getFeatureByClass(FastPlace.class);
        if (fastPlace == null || !fastPlace.isEnabled()) {
            return;
        }

        if (fastPlace.blockSetting.getSelectedCount() > 0 && this.player != null) {
            Block mainHand = Block.byItem(this.player.getMainHandItem().getItem());
            Block offHand = Block.byItem(this.player.getOffhandItem().getItem());
            if (!fastPlace.blockSetting.isSelected(mainHand) && !fastPlace.blockSetting.isSelected(offHand)) {
                return;
            }
        }

        this.rightClickDelay = fastPlace.fastPlaceDelay.getValue();
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    void onRenderFrame(boolean advanceGameTime, CallbackInfo ci) {
        EventManager.post(new EventEarlyBeginFrame());
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        if (Echo.featureConfig == null) {
            return;
        }

        Echo.featureConfig.saveProfile("_autosave");
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onHandleKeybindsEarly(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventHandleInput.Early event = new EventHandleInput.Early();
        event.call();

        if (this.player == null || this.options == null || this.options.keyAttack == null) {
            return;
        }

        KeyMappingAccessor attackKeyAccessor = (KeyMappingAccessor) this.options.keyAttack;
        if (attackKeyAccessor.getClickCount() <= 0) {
            return;
        }

        Entity targetEntity = null;
        if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.ENTITY) {
            targetEntity = ((EntityHitResult) this.hitResult).getEntity();
        }

        EventStartAttack.Pre preEvent = new EventStartAttack.Pre(this.player, targetEntity);
        preEvent.call();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void onHandleInputPre(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventHandleInput.Pre event = new EventHandleInput.Pre();
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void onHandleInputPost(CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventHandleInput.Post event = new EventHandleInput.Post();
        event.call();
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void redirectSendPacket(ClientPacketListener handler, Packet<?> packet) {
        if (Echo.isDestroyed) {
            handler.send(packet);
            return;
        }

        if (packet instanceof ServerboundPlayerActionPacket pac) {
            if (pac.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                EventSwapHands event = new EventSwapHands();
                event.call();
                if (event.cancelled) {
                    return;
                }
            }
        }

        handler.send(packet);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    public void onSetLevel(@Nullable ClientLevel level, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (level != null && this.player != null && level != this.lastFiredLevel) {
            this.lastFiredLevel = level;
            EventLevelChange event = new EventLevelChange();
            event.call();
            return;
        }

        if (level == null) {
            this.lastFiredLevel = null;
        }
    }
}
*///?} else {
public final class MixinMinecraft_26_2 {
}
//?}
