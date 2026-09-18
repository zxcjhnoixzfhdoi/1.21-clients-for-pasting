package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.StatusUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.inventory.PotionUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import net.minecraft.world.phys.Vec3;
import hack.echo.client.utils.strings.Concat;

public class SmartPot extends Feature {

	public SmartPot() {
		super(new FeatureInfo(
			Concat.of("Smart Pot"),
			Concat.of("Intelligent potion usage"),
			Category.UTILITY
		));
	}

    private static final BoolSetting angleCheck = new BoolSetting("Angle Check", true);
    private static final RangeSetting fov = new RangeSetting("FOV", 0, 20, 0, 90, 1, obj -> angleCheck.getValue());
    private static final RangeSetting minHealth = new RangeSetting("Min Health", 16, 16, 0, 20, 1);
    private static final BoolSetting throwPot = new BoolSetting("Throw Potion", false);
    private static final BoolSetting autoAim = new BoolSetting("Auto Aim", false, obj -> throwPot.getValue());
    private static final BoolSetting silentAim = new BoolSetting("Silent Aim", true, obj -> throwPot.getValue() && autoAim.getValue());
    private static final RangeSetting throwDelay = new RangeSetting("Throw Delay", 150, 250, 0, 450, 5, obj -> throwPot.getValue());
    private static final ModeSetting eventMode = new ModeSetting("Event Mode", "Tick", "Tick", "Real");
    private static final BoolSetting autoBuff = new BoolSetting("Auto Buff", true);
    private static final BoolSetting swapBack = new BoolSetting("Swap Back", false);
    private static final RangeSetting swapBackDelay = new RangeSetting("Swap Back Delay", 50, 100, 0, 500, 5, obj -> swapBack.getValue());

    private final TimerUtils throwTimer = new TimerUtils();
    private final TimerUtils swapBackTimer = new TimerUtils();
    private int lastSpeedThrowTick = -1;
    private int lastStrengthThrowTick = -1;
    private boolean isAiming = false;
    private boolean wasTracking = false;

    @EventSubscribe
    private void onSystemUpdate(EventSystemUpdate event) {
        if (!eventMode.is("Real")) return;
        if (!canUpdate()) return;
        handlePotions();
    }

    @EventSubscribe
    private void onAim(MouseUpdateEvent event) {
        if (!autoAim.getValue() || !throwPot.getValue() || !canUpdate()) {
            if (isAiming) {
                stopAiming();
            }
            return;
        }

        boolean hasPotion = hasAnyHealthPotion();
        boolean shouldAim = hasPotion && isLowHealth();

        if (shouldAim) {
            startAimingIfNeeded();
            aimDownForThrow();
        } else if (isAiming) {
            stopAiming();
        }
    }

    @EventSubscribe
    private void onSilentAim(EventMove.Pre event) {
        if (!autoAim.getValue() || !silentAim.getValue() || !throwPot.getValue()) return;
        if (!canUpdate() || !isAiming) return;
        if (!hasAnyHealthPotion()) return;
        if (!RotationUtils.hasSilentRotation()) return;

        event.setYaw(RotationUtils.getSilentYaw());
        event.setPitch(RotationUtils.getSilentPitch());
    }

    @EventSubscribe
    private void onTick(EventHandleInput.Early event) {
        if (!eventMode.is("Tick")) return;
        if (!canUpdate()) return;
        handlePotions();
    }

    private void handlePotions() {
        mainLogic();
        buffLogic();
        if (swapBack.getValue() && SwapStateManager.isOwnerActive(this) && swapBackTimer.hasReached(swapBackDelay.getRandom())) {
            resetSwapBack();
        }
    }

    private void mainLogic() {
        if (!shouldUseHealthPotion()) return;

        if (PotionUtils.hasInstantHealthPotionInOffhand()) {
            // No need for item swapping
            usePotionFromOffhand();
        } else {
            int potionSlot = PotionUtils.findInstantHealthPotionInHotbar();
            if (potionSlot != -1) {
                usePotionFromHotbar(potionSlot);
            }
        }
    }

    private void buffLogic() {
        if (!autoBuff.getValue()) return;
        if (angleCheck.getValue() && mc.player.getXRot() < (90 - fov.getRandom())) return;
        if (StatusUtils.hasSpeed() && StatusUtils.hasStrength()) return;

        if (lastSpeedThrowTick != -1 && mc.player.tickCount <= lastSpeedThrowTick) lastSpeedThrowTick = -1;
        if (lastStrengthThrowTick != -1 && mc.player.tickCount <= lastStrengthThrowTick) lastStrengthThrowTick = -1;

        if (!StatusUtils.hasSpeed() && mc.player.tickCount > lastSpeedThrowTick) {
            if (tryBuffWithOffhand(PotionUtils.hasSpeedPotionInOffhand())) {
                lastSpeedThrowTick = mc.player.tickCount;
            } else {
                int potionSlot = PotionUtils.findSpeedPotionInHotbar();
                if (potionSlot != -1 && usePotionFromHotbar(potionSlot)) {
                    lastSpeedThrowTick = mc.player.tickCount;
                }
            }
        }

        if (!StatusUtils.hasStrength() && mc.player.tickCount > lastStrengthThrowTick) {
            if (tryBuffWithOffhand(PotionUtils.hasStrengthPotionInOffhand())) {
                lastStrengthThrowTick = mc.player.tickCount;
            } else {
                int potionSlot = PotionUtils.findStrengthPotionInHotbar();
                if (potionSlot != -1 && usePotionFromHotbar(potionSlot)) {
                    lastStrengthThrowTick = mc.player.tickCount;
                }
            }
        }
    }

    private boolean shouldUseHealthPotion() {
        float targetPitch = 90 - fov.getRandom();

        float effectivePitch;
        if (autoAim.getValue() && silentAim.getValue()) {
            effectivePitch = RotationUtils.getSilentPitch();
        } else {
            effectivePitch = mc.player.getXRot();
        }

        boolean pitchOk = !angleCheck.getValue() || effectivePitch >= targetPitch;

        return mc.player.onGround() && isLowHealth() && pitchOk;
    }

    private boolean usePotionFromHotbar(int potionSlot) {
        if (swapBack.getValue()) {
            if (!SwapStateManager.swapToIfNeeded(this, potionSlot, false, -1, false)) {
                return false;
            }
        } else {
            InventoryUtils.setInvSlot(potionSlot);
        }
        swapBackTimer.reset();
        if (throwPot.getValue()) {
            throwPotion();
            return true;
        }
        return false;
    }

    private void throwPotion() {
        if (throwTimer.hasReached(throwDelay.getRandom())) {
            throwTimer.reset();
            ((MinecraftAccessor) mc).invokeStartUseItem();
        }
    }

    private void usePotionFromOffhand() {
        if (throwPot.getValue()) {
            throwPotion();
        }
    }

    private boolean tryBuffWithOffhand(boolean hasOffhandPotion) {
        if (!hasOffhandPotion) return false;
        usePotionFromOffhand();
        return true;
    }

    private boolean canUpdate() {
        return !isNull() && hack.echo.client.api.MinecraftCompat.getScreen() == null;
    }

    private boolean hasAnyHealthPotion() {
        return PotionUtils.hasInstantHealthPotionInOffhand()
                || PotionUtils.findInstantHealthPotionInHotbar() != -1;
    }

    private boolean isLowHealth() {
        return mc.player.getHealth() <= minHealth.getRandom();
    }

    private void startAimingIfNeeded() {
        if (!isAiming) {
            RotationUtils.initFromPlayer();
            isAiming = true;
        }
    }

    private void aimDownForThrow() {
        float targetPitch = 90 - fov.getRandom();
        Vec3 target = mc.player.getEyePosition().add(
                Vec3.directionFromRotation(Math.max(targetPitch, 89.9f), mc.player.getYRot())
        );
        boolean success = RotationUtils.aim(this)
                .silent(silentAim.getValue())
                .priority(EventSubscribe.Priority.HIGHEST)
                .speed(450f)
                .to(target);
        if (success) {
            wasTracking = true;
        }
    }

    private void stopAiming() {
        if (!wasTracking) {
            isAiming = false;
            return;
        }

        if (RotationUtils.cleanup(this, silentAim.getValue())) {
            return;
        }

        wasTracking = false;
        isAiming = false;
    }

    private void resetSwapBack() {
        SwapStateManager.cancel(this);
        swapBackTimer.reset();
    }

    @Override
    public void onDisable() {
        SwapStateManager.cancel(this, false);
        lastSpeedThrowTick = -1;
        lastStrengthThrowTick = -1;
        isAiming = false;
        wasTracking = false;
        RotationUtils.stopTracking();
        throwTimer.reset();
        swapBackTimer.reset();
        super.onDisable();
    }
}
