package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.impl.player.AutoTotem;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class DiveBomb extends Feature {

    public DiveBomb() {
        super(new FeatureInfo(
                Concat.of("Dive Bomb"),
                Concat.of("Tp to the sky with a wind charge and enderpearl"),
                Category.MACROS
        ));
    }

    private final BoolSetting autoJump = new BoolSetting(Concat.of("Auto Jump"), true);
    private final BoolSetting silentAim = new BoolSetting(Concat.of("Silent Aim"), true);
    private final BoolSetting switchback = new BoolSetting(Concat.of("Switchback"), true);
    private final IntSetting tickDelay = new IntSetting(Concat.of("Tick Delay"), 2, 0, 6, Concat.of(" ticks"));
    
    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("Dive Bomb"), -1);
    private final KeybindSetting instantActivateKey = new KeybindSetting(Concat.of("Instant Dive Bomb"), -1);

    private boolean activated = false;
    private boolean keyWasDown = false;
    private boolean instantKeyWasDown = false;
    private final TimerUtils timer = new TimerUtils();
    private boolean wasTracking = false;

    private int stage = 0; // 0 = Charge, 1 = Wait, 2 = Pearl
    private InstantRun instantRun = null;

    @Override
    public void onDisable() {
        AutoTotem.blocked = false;
        cleanupRotation();
        activated = false;
        keyWasDown = false;
        instantKeyWasDown = false;
        instantRun = null;
        SwapStateManager.cancel(this, false);
        timer.reset();
        stage = 0;
        super.onDisable();
    }

    private boolean hasItems() {
        return InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.WIND_CHARGE) != -1 &&
               InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.ENDER_PEARL) != -1;
    }

    private boolean isCoolingDown() {
        return mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL)) ||
               mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE));
    }

    private void cleanupRotation() {
        if (!wasTracking) return;
        if (RotationUtils.cleanup(this, silentAim.getValue())) return;
        wasTracking = false;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onPress(EventMovementInput e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        if (autoJump.getValue() && activated && instantRun == null && mc.player.onGround()) {
             e.jump = true;
        }

        // to make thrown items less affected by movement
        if (activated) {
            e.forward = false;
            e.left = false;
            e.right = false;
            e.back = false;
        }

        tickInstantKey();
        if (instantRun != null) {
            if (instantRun.tick()) stopInstant();
            return;
        }

        if (!activated && (!hasItems() || isCoolingDown())) return;

        boolean keyDown = InputHandler.isBindDown(activateKey.getKey());
        if (keyDown && !keyWasDown) {
            if (RotationUtils.getCurrentTarget() != null && 
                !RotationUtils.canTakeControl(this, EventSubscribe.Priority.HIGH)) {
                return;
            }
            activated = true;
            stage = 0;
            if (silentAim.getValue() && !RotationUtils.isTracking()) {
                RotationUtils.initFromPlayer();
            }
        }
        keyWasDown = keyDown;

        if (!activated) return;
        
        // Wait for rotation tracking to start
        if (!RotationUtils.isTracking()) return; 

        float currentPitch = silentAim.getValue() ? RotationUtils.getLastRotations()[1] : mc.player.getXRot();
        if (currentPitch >= -89.5f) return;
        


        if (stage == 0) {
             // START: Ender Pearl
             if (isCoolingDown()) return;
             
             int pearlSlot = InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.ENDER_PEARL);
             if (pearlSlot != -1) {
                 if (!SwapStateManager.swapToIfNeeded(this, pearlSlot, false, -1, false)) {
                     finish();
                     return;
                 }
                 // Verify low horizontal velocity to prevent trajectory indifference
                //  if (mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.00001d) return;
                 ((MinecraftAccessor) mc).invokeStartUseItem();
                 
                 timer.reset();
                 stage = 1;
             }
        } else if (stage == 1) {
             // WAIT
             if (timer.hasReachedTicks(tickDelay.getValue())) {
                 stage = 2;
             }
        } else if (stage == 2) {
             // Wind Charge
             int chargeSlot = InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.WIND_CHARGE);
             if (chargeSlot != -1) {
                  if (!SwapStateManager.swapToIfNeeded(this, chargeSlot, false, -1, false)) {
                      finish();
                      return;
                  }
                  ((MinecraftAccessor) mc).invokeStartUseItem();
                  timer.reset();
                  stage = 3;
              } else {
                 finish();
             }
        } else if (stage == 3) {
            // 1 gabillion millisecond delay to prevent badPacketJ flag ÃƒÂ°Ã…Â¸Ã¢â‚¬â„¢Ã¢â€šÂ¬
            // But becasue we want the pearl and charge to collide we do
            // 1 tick to ensure they collide
             if (timer.hasReachedTicks(1)) {
                 finish();
             }
        }
    }

    private void tickInstantKey() {
        boolean down = InputHandler.isBindDown(instantActivateKey.getKey());
        if (down && !instantKeyWasDown) startInstant();
        instantKeyWasDown = down;
    }

    private void startInstant() {
        if (activated || instantRun != null || isCoolingDown()) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean restoreOffhand = !mc.player.getOffhandItem().is(Items.WIND_CHARGE);
        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.WIND_CHARGE);
        int pearlSlot = InventoryUtils.findItemWithPredicateInHotbar(s -> s.getItem() == Items.ENDER_PEARL);

        if (restoreOffhand && windChargeSlot == -1) return;
        if (!mc.player.getMainHandItem().is(Items.ENDER_PEARL) && pearlSlot == -1) return;

        if (RotationUtils.getCurrentTarget() != null &&
            !RotationUtils.canTakeControl(this, EventSubscribe.Priority.HIGH)) {
            return;
        }

        activated = true;
        stage = 0;
        if (silentAim.getValue() && !RotationUtils.isTracking()) {
            RotationUtils.initFromPlayer();
        }
        AutoTotem.blocked = true;
        instantRun = new InstantRun(originalSlot, windChargeSlot, pearlSlot, restoreOffhand);
    }

    private void stopInstant() {
        AutoTotem.blocked = false;
        instantRun = null;
        activated = false;
        stage = 0;
        cleanupRotation();
    }

    private boolean isInstantReady() {
        return mc.player.getMainHandItem().is(Items.ENDER_PEARL)
            && mc.player.getOffhandItem().is(Items.WIND_CHARGE);
    }

    private static final int STEP_TIMEOUT_TICKS = 10;

    private record Step(Runnable enter, BooleanSupplier done) {}

    private enum Phase { PREP, AIM, THROW, RESTORE }

    private final class InstantRun {
        final int originalSelectedSlot;
        final int windChargeSlot;
        final boolean restoreOffhand;
        final Deque<Step> prep = new ArrayDeque<>();
        final Deque<Step> restore = new ArrayDeque<>();
        Phase phase = Phase.PREP;
        Step current = null;
        int stepTicks = 0;

        InstantRun(int originalSelectedSlot, int windChargeSlot, int pearlSlot, boolean restoreOffhand) {
            this.originalSelectedSlot = originalSelectedSlot;
            this.windChargeSlot = windChargeSlot;
            this.restoreOffhand = restoreOffhand;

            if (restoreOffhand) {
                prep.add(selectSlot(windChargeSlot));
                prep.add(swapOffhand(true));
            }
            if (!mc.player.getMainHandItem().is(Items.ENDER_PEARL)) {
                prep.add(new Step(
                    () -> InventoryUtils.setInvSlot(pearlSlot, true),
                    () -> mc.player.getMainHandItem().is(Items.ENDER_PEARL)));
            }

            if (restoreOffhand) {
                restore.add(selectSlot(windChargeSlot));
                restore.add(swapOffhand(false));
                restore.add(new Step(
                    () -> InventoryUtils.setInvSlot(originalSelectedSlot, true),
                    () -> true));
            }
        }

        private Step selectSlot(int slot) {
            return new Step(
                () -> InventoryUtils.setInvSlot(slot, true),
                () -> mc.player.getInventory().getSelectedSlot() == slot);
        }

        private Step swapOffhand(boolean expectWindChargeInOffhand) {
            return new Step(
                () -> InputHandler.simulateClick(mc.options.keySwapOffhand, false),
                () -> mc.player.getOffhandItem().is(Items.WIND_CHARGE) == expectWindChargeInOffhand);
        }

        boolean tick() {
            return switch (phase) {
                case PREP -> tickQueue(prep, Phase.AIM);
                case AIM -> tickAim();
                case THROW -> {
                    InputHandler.simulateClick(mc.options.keyUse, false);
                    InputHandler.simulateClick(mc.options.keyUse, false);
                    if (restoreOffhand) {
                        phase = Phase.RESTORE;
                        yield false;
                    }
                    yield true;
                }
                case RESTORE -> tickQueue(restore, null);
            };
        }

        private boolean tickQueue(Deque<Step> q, Phase next) {
            if (current == null) {
                if (q.isEmpty()) {
                    if (next == null) return true;
                    phase = next;
                    return false;
                }
                current = q.poll();
                current.enter.run();
                stepTicks = 0;
            }
            if (current.done.getAsBoolean()) {
                current = null;
                return false;
            }
            if (++stepTicks > STEP_TIMEOUT_TICKS) return abort();
            return false;
        }

        private boolean tickAim() {
            if (!isInstantReady() || isCoolingDown()) return abort();
            float pitch = silentAim.getValue() ? RotationUtils.getLastRotations()[1] : mc.player.getXRot();
            if (pitch < -89.5f) phase = Phase.THROW;
            return false;
        }

        private boolean abort() {
            if (restoreOffhand && phase != Phase.RESTORE
                && mc.player.getOffhandItem().is(Items.WIND_CHARGE)) {
                phase = Phase.RESTORE;
                current = null;
                return false;
            }
            return true;
        }
    }
    
    private void finish() {
        activated = false;
        stage = 0;
        if (!switchback.getValue()) {
            SwapStateManager.cancel(this, false);
        }
        cleanupRotation();
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onAim(MouseUpdateEvent e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        
        if (!activated && wasTracking) {
             if (!timer.hasReachedTicks(1)) return;
             cleanupRotation();
             return;
        }
        
        if (!activated) return;

        float yaw;
        if (RotationUtils.isTracking() && RotationUtils.hasSilentRotation()) {
            yaw = RotationUtils.getSilentYaw();
        } else {
            yaw = silentAim.getValue() ? RotationHandler.getServerYaw() : mc.player.getYRot();
        }
        
        // straight up while preserving yaw
        Vec3 target = mc.player.getEyePosition().add(Vec3.directionFromRotation(-89.9f, yaw).scale(10)); 
        
        RotationUtils.aim(this).silent(silentAim.getValue()).speed(450f).priority(EventSubscribe.Priority.HIGH).to(target);
        wasTracking = true;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onSilentAim(EventMove.Pre e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!silentAim.getValue()) return;

        if (!RotationUtils.hasSilentRotation()) return;
        if (!wasTracking && !activated) return;
        
        e.setYaw(RotationUtils.getSilentYaw());
        e.setPitch(RotationUtils.getSilentPitch());
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onTick(EventHandleInput.Early e) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        if (!switchback.getValue()) return;
        if (!activated && SwapStateManager.isOwnerActive(this) && timer.hasReachedTicks(1)) {
            SwapStateManager.cancel(this);
        }
    }
}
