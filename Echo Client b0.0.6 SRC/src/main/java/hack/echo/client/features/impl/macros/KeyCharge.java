package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

// Use as a reference silent aim macro modules
public class KeyCharge extends Feature {

    public KeyCharge() {
        super(new FeatureInfo(
                Concat.of("Key Charge"),
                Concat.of("Charge attacks with key"),
                Category.MACROS
        ));
    }

    private final BoolSetting autoJump = new BoolSetting(Concat.of("Auto Jump"), true);
    private final BoolSetting autoAim = new BoolSetting(Concat.of("Auto Aim"), true);
    private final BoolSetting silentAim = new BoolSetting(Concat.of("Silent Aim"), true, sexy -> autoAim.getValue());
    private final BoolSetting switchback = new BoolSetting(Concat.of("Switchback"), true);
    private final BoolSetting silentSwap = new BoolSetting(Concat.of("Silent Swap"), true, sexy -> switchback.getValue());

    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("Activate Key"), -1);

    private boolean activated = false;
    private boolean keyWasDown = false;
    private final TimerUtils timer = new TimerUtils();
    private boolean wasTracking = false;

    @Override
    public void onDisable() {
        cleanupRotation();
        activated = false;
        keyWasDown = false;
        SwapStateManager.cancel(this, false);
        timer.reset();
        super.onDisable();
    }

    private boolean hasWindCharge() {
        return InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.WIND_CHARGE) != -1;
    }

    /**
     * Cleans up rotation tracking when we're no longer using wind charges.
     * Releases control and rotates back if no other modules are tracking.
     */
    private void cleanupRotation() {
        if (!wasTracking) return;
        if (RotationUtils.cleanup(this, silentAim.getValue())) return;
        wasTracking = false;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onPress(EventMovementInput e) {
        if (!hasWindCharge()) return;
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        e.jump = InputHandler.isBindDown(((KeyMappingAccessor) mc.options.keyJump).getKey().getValue());

        boolean keyDown = InputHandler.isBindDown(activateKey.getKey());
        if (keyDown && !keyWasDown) {
            // Only prevent activation if there is a target AND we cannot take control with HIGH priority
            if (RotationUtils.getCurrentTarget() != null && 
                !RotationUtils.canTakeControl(this, EventSubscribe.Priority.HIGHEST)) {
                return;
            }
            activated = true;
            if (autoAim.getValue() && silentAim.getValue() && !RotationUtils.isTracking()) {
                RotationUtils.initFromPlayer();
            }
        }
        keyWasDown = keyDown;

        if (!activated) return;
        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE))) return;

        if (autoAim.getValue()) {
            if (!RotationUtils.isTracking()) return;
            float pitch = silentAim.getValue() ? RotationUtils.getLastRotations()[1] : mc.player.getXRot();
            if (pitch < 88f) return;
        }

        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.WIND_CHARGE);
        if (windChargeSlot != -1) {
            if (!SwapStateManager.swapToIfNeeded(this, windChargeSlot, false, -1, false)) {
                return;
            }
            ((MinecraftAccessor) mc).invokeStartUseItem();
            if (silentSwap.getValue()) {
                SwapStateManager.cancel(this, switchback.getValue());
            } else if (!switchback.getValue()) {
                SwapStateManager.cancel(this, false);
            }
            timer.reset();

            if (autoJump.getValue() && mc.player.onGround()) {
                if (autoAim.getValue() || mc.player.getXRot() >= 80) {
                    e.jump = true;
                }
            }

            activated = false;
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onAim(MouseUpdateEvent e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        
        // Delay cleanup for 1 tick after the last use item to ensure rotation packets match and dont flag BadPacketsJ
        if (!hasWindCharge() && wasTracking) {
            if (!timer.hasReachedTicks(1)) {
                return;
            }
            cleanupRotation();
            return;
        }
        
        if (!hasWindCharge()) return;
        if (!autoAim.getValue()) return;

        if (activated) {
            float yaw;
            if (RotationUtils.isTracking() && RotationUtils.hasSilentRotation()) {
                yaw = RotationUtils.getSilentYaw();
            } else {
                yaw = silentAim.getValue() ? RotationHandler.getServerYaw() : mc.player.getYRot();
            }
            Vec3 target = mc.player.getEyePosition().add(Vec3.directionFromRotation(89.9f, yaw));
            RotationUtils.aim(this).silent(silentAim.getValue()).speed(450f).priority(EventSubscribe.Priority.HIGHEST).to(target);
            wasTracking = true;
        } else if (wasTracking) {
            cleanupRotation();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onSilentAim(EventMove.Pre e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!autoAim.getValue() || !silentAim.getValue()) return;

        if (!RotationUtils.hasSilentRotation()) return;

        // Allow silent rotation even if we ran out of wind charges
        // Otherwise it will get stuck at the target rotation
        if (!wasTracking && !activated) return;
        

        e.setYaw(RotationUtils.getSilentYaw());
        e.setPitch(RotationUtils.getSilentPitch());
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onTick(EventHandleInput.Early e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        // if (!wasTracking) return;

        if (!switchback.getValue()) return;
        if (silentSwap.getValue()) return;
        if (!SwapStateManager.isOwnerActive(this)) return;
        // for some reason 1 tick is technically 2 ticks?
        if (timer.hasReachedTicks(1)) {
            SwapStateManager.cancel(this);
        }
    }

    public static boolean isKeyChargeActive() {
        return SwapStateManager.isOwnerActive(KeyCharge.class);
    }

}
