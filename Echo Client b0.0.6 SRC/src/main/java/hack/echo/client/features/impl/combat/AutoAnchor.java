package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.event.impl.EventStartUseItem;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.impl.combat.autoanchor.AnchorAimSearch;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.api.MinecraftCompat;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.screens.clickgui.glass.GlassUIConstants;
import hack.echo.client.utils.DrawUtils;
import hack.echo.client.utils.blocks.BlockUtils;
import hack.echo.client.utils.combat.ExplosionUtils;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.rotation.RotationConvergenceTracker;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks an anchor placement near the closest target, places it, and runs the
 * charge/explode sequence. Optional safety-block step drops a glowstone between
 * us and the anchor before charging so we eat less of our own explosion.
 *
 * <p>The interesting bits live in helper classes:
 * <ul>
 *   <li>{@link AnchorAimSearch} -- finds an aim point on the anchor that
 *       raycasts cleanly (works even when an adjacent block occludes the
 *       eye-side face).</li>
 *   <li>{@link RotationConvergenceTracker} -- per-tick rotation delta so we
 *       only fire interactions on a tick where Grim's {@code deltaX > 2}
 *       gate is closed.</li>
 * </ul>
 */
public class AutoAnchor extends Feature {

    private static final CharSequence MODE_HOLD = Concat.of("Hold");
    private static final CharSequence MODE_CLICK = Concat.of("Click");
    private static final int VANILLA_PLACE_COOLDOWN_TICKS = 4;
    private static final int MAX_ANCHOR_CHARGES = 4;
    private static final float ROTATION_SETTLE_THRESHOLD = 1.7f;

    private enum AnchorStep { PLACE_SAFETY, CHARGE, EXPLODE }

    public AutoAnchor() {
        super(new FeatureInfo(
                Concat.of("Auto Anchor"),
                Concat.of("Places anchors at the optimal position near your target"),
                Category.COMBAT
        ));
    }

    // -- Settings --

    private final ModeSetting mode = new ModeSetting(
            Concat.of("Mode"), MODE_CLICK, MODE_HOLD, MODE_CLICK);
    private final KeybindSetting activateKey = new KeybindSetting(
            Concat.of("Activate Key"), -1
    ).describedBy(Concat.of("If this matches your Use Key, start the sequence while holding a respawn anchor so vanilla use packets are not duplicated"));
    private final BoolSetting repeat = new BoolSetting(
            Concat.of("Repeat"), true, value -> mode.is(MODE_HOLD));
    private final BoolSetting autoAim = new BoolSetting(
            Concat.of("Auto Aim"), true);
    private final BoolSetting silentAim = new BoolSetting(
            Concat.of("Silent Aim"), false, value -> autoAim.getValue());
    private final IntSetting aimSpeed = new IntSetting(
            Concat.of("Aim Speed"), 120, 0, 600, value -> autoAim.getValue());
    private final BoolSetting renderPosition = new BoolSetting(
            Concat.of("Render Position"), true);
    private final ColorSetting fillColor = new ColorSetting(
            Concat.of("Fill Color"),
            GlassUIConstants.ACCENT_FOR_RENDERING.getRGB(),
            value -> renderPosition.getValue(),
            true);
    private final ColorSetting outlineColor = new ColorSetting(
            Concat.of("Outline Color"),
            Color.WHITE.getRGB(),
            value -> renderPosition.getValue(),
            true);
    private final BoolSetting canPlaceLegit = new BoolSetting(
            Concat.of("Can Place Legit"), true
    ).describedBy(Concat.of("Positions where you can only place legit"));
    private final BoolSetting safeAnchor = new BoolSetting(
            Concat.of("Safe Anchor"), false
    ).describedBy(Concat.of("Place a safety block at your feet between you and the anchor before charging"));
    private final IntSetting safetyTimeout = new IntSetting(
            Concat.of("Safety Timeout"), 200, 0, 2000, Concat.of(" ms"),
            value -> safeAnchor.getValue()
    ).describedBy(Concat.of("Give up on placing the safety block after this long and proceed to charging anyway"));
    private final FloatSetting minDamage = new FloatSetting(
            Concat.of("Min Damage"), 6.0f, 0.0f, 20.0f, 0.5f,
            value -> safeAnchor.getValue()
    ).describedBy(Concat.of("Skip the safety block if the anchor wouldn't deal at least this much damage to you"));
    private final RangeSetting charges = new RangeSetting(
            Concat.of("Charges"), 1, 1, 1, MAX_ANCHOR_CHARGES, 1, Concat.of(" charges"));
    private final RangeSetting chargeDelay = new RangeSetting(
            Concat.of("Charge Delay"), 1, 1, 0, 20, 1, Concat.of(" ticks"));
    private final IntSetting explodeSlot = new IntSetting(
            Concat.of("Explode Slot"), 1, 1, 9);
    private final RangeSetting explodeDelay = new RangeSetting(
            Concat.of("Explode Delay"), 1, 1, 0, 20, 1, Concat.of(" ticks"));

    // -- State --

    private boolean clickQueued;
    private boolean keyWasDown;
    private boolean wasAiming;
    private boolean restoringSwap;
    private boolean holdCycleCompleted;
    private int placeCooldown;
    private AnchorStep anchorStep;
    private BlockPos activeAnchorPos;
    private BlockPos pendingUseKeyAnchorPos;
    private int remainingCharges;
    private int nextAnchorActionTick;

    /** Aim point picked once per sequence; revalidated each call so a freshly
     *  placed safety block can force a re-pick onto an unobstructed face. */
    private Vec3 cachedAnchorAim;

    /** Wall-clock at PLACE_SAFETY entry; used by {@link #safetyTimeout}. */
    private long safetyStepStartMillis;

    private final RotationConvergenceTracker rotation = new RotationConvergenceTracker();

    // -- Lifecycle --

    @Override
    public void onEnable() {
        super.onEnable();
        clickQueued = false;
        keyWasDown = false;
        wasAiming = false;
        restoringSwap = false;
        holdCycleCompleted = false;
        placeCooldown = 0;
        rotation.reset();
        resetAnchorSequence();
    }

    @Override
    public void onDisable() {
        clickQueued = false;
        keyWasDown = false;
        wasAiming = false;
        restoringSwap = false;
        holdCycleCompleted = false;
        placeCooldown = 0;
        resetAnchorSequence();
        SwapStateManager.cancel(this, true);
        if (RotationUtils.isControlledBy(this)) {
            RotationUtils.stopTracking();
        }
        super.onDisable();
    }

    // -- Event handlers --

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onStartUseItem(EventStartUseItem.Pre event) {
        if (!shouldHandleSameUseKey()) return;
        if (event.getPlayer() != mc.player) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (MinecraftCompat.getScreen() != null || activateKey.isListening()) return;
        if (mode.is(MODE_HOLD) && !InputHandler.isBindDown(activateKey.getKey()) && !isAnchorSequenceActive()) return;

        boolean startFromAnchor = !isAnchorSequenceActive() && event.getStack().is(Items.RESPAWN_ANCHOR);
        if (startFromAnchor) {
            pendingUseKeyAnchorPos = anchorPosOf(event.getHitResult());
        }

        if (!startFromAnchor && !isAnchorSequenceActive() && !SwapStateManager.isOwnerActive(this) && !restoringSwap) {
            return;
        }

        if (!mode.is(MODE_HOLD) && startFromAnchor) {
            clickQueued = true;
            if (!isAnchorSequenceActive()) {
                restoringSwap = false;
                placeCooldown = 0;
            }
        }

        event.cancel();
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onRender3D(EventRender3D event) {
        if (isNull() || !renderPosition.getValue()) return;

        boolean renderFill = fillColor.getAlpha() > 0;
        boolean renderOutline = outlineColor.getAlpha() > 0;
        if (!renderFill && !renderOutline) return;

        BlockUtils.PlacementHit bestHit = findBestPlacementHit(event.getTickDelta());
        if (bestHit == null) return;

        BlockPos bestPos = bestHit.placementPos();
        FramebufferTarget draw = event.getDraw3D().getMinecraftTarget();
        if (renderFill) {
            draw.box(bestPos.getX(), bestPos.getY(), bestPos.getZ(), 1.0, 1.0, 1.0, fillColor.getARGB());
        }
        if (renderOutline) {
            DrawUtils.drawBoxOutline(draw, new AABB(bestPos), outlineColor.getARGB());
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onTick(EventTick event) {
        rotation.update();

        if (placeCooldown > 0) placeCooldown--;
        if (!SwapStateManager.isOwnerActive(this)) restoringSwap = false;

        if (isNull() || MinecraftCompat.getScreen() != null || activateKey.isListening()) {
            clickQueued = false;
            resetAnchorSequence();
            handleInactive();
            keyWasDown = false;
            return;
        }

        boolean sameUseKey = shouldHandleSameUseKey();
        boolean keyDown = InputHandler.isBindDown(activateKey.getKey());
        boolean holdMode = mode.is(MODE_HOLD);
        if (holdMode && !keyDown) holdCycleCompleted = false;

        if (!holdMode && keyDown && !keyWasDown && (!sameUseKey || isHoldingRespawnAnchor())) {
            clickQueued = true;
            if (!isAnchorSequenceActive()) {
                restoringSwap = false;
                placeCooldown = 0;
            }
        }

        boolean active = holdMode ? keyDown : clickQueued || isAnchorSequenceActive();
        if (!active) {
            resetAnchorSequence();
            handleInactive();
            keyWasDown = keyDown;
            return;
        }

        if (holdMode && !repeat.getValue() && holdCycleCompleted && !isAnchorSequenceActive()) {
            handleInactive();
            keyWasDown = keyDown;
            return;
        }

        if (sameUseKey && !isAnchorSequenceActive() && !isHoldingRespawnAnchor()) {
            if (!holdMode) clickQueued = false;
            if (!SwapStateManager.isOwnerActive(this)) handleInactive();
            keyWasDown = keyDown;
            return;
        }

        if (isAnchorSequenceActive()) {
            continueAnchorSequence();
            keyWasDown = keyDown;
            return;
        }

        BlockPos existingAnchor = consumePendingUseKeyAnchorPos();
        if (existingAnchor == null) {
            BlockHitResult hit = currentBlockHit();
            if (hit != null && isRespawnAnchor(hit.getBlockPos())) existingAnchor = hit.getBlockPos();
        }

        if (existingAnchor != null) {
            if (!isAnchorWithinInteractionRange(existingAnchor)) {
                if (!holdMode) clickQueued = false;
                handleInactive();
                keyWasDown = keyDown;
                return;
            }
            beginAnchorSequence(existingAnchor, rollChargeCount());
            keyWasDown = keyDown;
            return;
        }

        keyWasDown = keyDown;
        runFreshAnchorPlacement(holdMode, sameUseKey, keyDown);
    }

    /** Handles the "no active sequence yet, no anchor under cursor" branch -- find a spot, swap, place. */
    private void runFreshAnchorPlacement(boolean holdMode, boolean useHeldAnchorSlot, boolean keyDown) {
        // Same-use-key flow: the user drives placement by looking at a block.
        // Auto-aim is reserved for charging/exploding, so we force the manual
        // path here even when Auto Aim is enabled.
        boolean useAutoAim = autoAim.getValue() && !useHeldAnchorSlot;
        BlockUtils.PlacementHit autoAimHit = null;
        BlockHitResult manualHit = null;
        boolean useAutoAimPlacement = false;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        if (useAutoAim) {
            autoAimHit = findBestPlacementHit(partialTick);
            if (autoAimHit != null) {
                useAutoAimPlacement = true;
            } else {
                manualHit = currentBlockHit();
            }
            if (manualHit == null && !useAutoAimPlacement) {
                if (!holdMode) clickQueued = false;
                handleInactive();
                return;
            }
        } else {
            manualHit = currentBlockHit();
            if (manualHit == null) {
                if (!holdMode) clickQueued = false;
                handleInactive();
                return;
            }
        }

        int anchorSlot = useHeldAnchorSlot
                ? mc.player.getInventory().getSelectedSlot()
                : InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.is(Items.RESPAWN_ANCHOR));
        if (anchorSlot == -1) {
            clickQueued = false;
            handleInactive();
            return;
        }

        restoringSwap = false;
        if (!useHeldAnchorSlot && !SwapStateManager.swapToIfNeeded(this, anchorSlot, false, -1, false)) return;
        if (placeCooldown > 0) return;
        if (!mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) return;

        BlockHitResult hitResult;
        BlockPos placedAnchorPos;
        if (useAutoAimPlacement) {
            if (autoAimHit == null || !isReadyToPlace(autoAimHit, partialTick)) return;
            hitResult = autoAimHit.hitResult();
            placedAnchorPos = autoAimHit.placementPos();
        } else {
            hitResult = manualHit;
            placedAnchorPos = placedAnchorPosOf(hitResult);
            if (!isManualAnchorPlacementValid(placedAnchorPos)) {
                if (!holdMode) clickQueued = false;
                handleInactive();
                return;
            }
        }

        InteractionResult result = BlockUtils.interactWithBlock(hitResult, true);
        placeCooldown = VANILLA_PLACE_COOLDOWN_TICKS;
        if (result.consumesAction()) rotation.markInteraction();

        if (result.consumesAction() && placedAnchorPos != null) {
            beginAnchorSequence(placedAnchorPos, rollChargeCount());
        } else if (!holdMode) {
            finishAnchorSequence();
        }

        if (holdMode && !result.consumesAction()) {
            placeCooldown = Math.max(placeCooldown, 1);
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onMouseUpdate(MouseUpdateEvent event) {
        if (!autoAim.getValue() || isNull() || MinecraftCompat.getScreen() != null || !isActionActive()
                || (shouldHandleSameUseKey() && !isAnchorSequenceActive())) {
            disengageRotations();
            return;
        }

        Vec3 aimPoint = findActiveAimPoint();
        if (aimPoint == null) {
            disengageRotations();
            return;
        }

        boolean rotated = RotationUtils.aim(this)
                .priority(EventSubscribe.Priority.HIGH)
                .silent(silentAim.getValue())
                .speed(aimSpeed.getValue())
                .aimType(RotationUtils.AimType.REGULAR)
                .to(aimPoint);

        if (!rotated) {
            disengageRotations();
            return;
        }
        wasAiming = true;
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onMove(EventMove.Pre event) {
        if (!autoAim.getValue() || !wasAiming || !silentAim.getValue()) return;
        if (!RotationUtils.isControlledBy(this) || !RotationUtils.hasSilentRotation()) return;

        event.setYaw(RotationUtils.getSilentYaw());
        event.setPitch(RotationUtils.getSilentPitch());
    }

    // -- Sequence state machine --

    private void beginAnchorSequence(BlockPos anchorPos, int chargeCount) {
        activeAnchorPos = anchorPos;
        remainingCharges = Math.max(0, chargeCount - getAnchorCharge(anchorPos));
        anchorStep = safeAnchor.getValue() ? AnchorStep.PLACE_SAFETY : AnchorStep.CHARGE;
        nextAnchorActionTick = mc.player.tickCount + (anchorStep == AnchorStep.CHARGE ? rollTickDelay(chargeDelay) : 0);
        restoringSwap = false;
        cachedAnchorAim = AnchorAimSearch.findClearAimPoint(anchorPos, partialTick());
        safetyStepStartMillis = System.currentTimeMillis();
    }

    private void continueAnchorSequence() {
        if (!isRespawnAnchor(activeAnchorPos) || !isAnchorWithinInteractionRange(activeAnchorPos)) {
            finishAnchorSequence();
            return;
        }
        if (mc.player.tickCount < nextAnchorActionTick) return;

        switch (anchorStep) {
            case PLACE_SAFETY -> placeSafetyBlock();
            case CHARGE -> chargeActiveAnchor();
            case EXPLODE -> explodeActiveAnchor();
        }
    }

    private void finishAnchorSequence() {
        resetAnchorSequence();
        if (mode.is(MODE_HOLD) && InputHandler.isBindDown(activateKey.getKey())) {
            if (!repeat.getValue()) {
                holdCycleCompleted = true;
                scheduleSwapBack(1);
                disengageRotations();
            } else if (shouldHandleSameUseKey()) {
                scheduleSwapBack(1);
            }
            return;
        }
        holdCycleCompleted = false;
        clickQueued = false;
        scheduleSwapBack(1);
    }

    private void resetAnchorSequence() {
        anchorStep = null;
        activeAnchorPos = null;
        pendingUseKeyAnchorPos = null;
        remainingCharges = 0;
        nextAnchorActionTick = 0;
        cachedAnchorAim = null;
    }

    private boolean isAnchorSequenceActive() {
        return anchorStep != null && activeAnchorPos != null;
    }

    private void advanceToChargeStep() {
        anchorStep = AnchorStep.CHARGE;
        nextAnchorActionTick = mc.player.tickCount + rollTickDelay(chargeDelay);
    }

    private void beginExplodeStepOrFinish() {
        if (getAnchorCharge(activeAnchorPos) <= 0) {
            finishAnchorSequence();
            return;
        }
        anchorStep = AnchorStep.EXPLODE;
        nextAnchorActionTick = mc.player.tickCount + rollTickDelay(explodeDelay);
    }

    // -- Step: PLACE_SAFETY --

    private void placeSafetyBlock() {
        // Build the safety candidate list ONCE per tick instead of three times.
        List<BlockPos> ranked = rankedSafetyCandidates();

        if (!shouldPlaceSafety(ranked)) {
            advanceToChargeStep();
            return;
        }

        int timeoutMs = safetyTimeout.getValue();
        if (timeoutMs > 0 && System.currentTimeMillis() - safetyStepStartMillis >= timeoutMs) {
            advanceToChargeStep();
            return;
        }

        boolean useAutoAim = autoAim.getValue();
        BlockHitResult hitResult;
        float partialTick = partialTick();

        if (useAutoAim) {
            BlockUtils.PlacementHit placementHit = pickSafetyPlacement(ranked);
            if (placementHit == null) return;
            if (!isReadyToPlace(placementHit, partialTick)) return;
            hitResult = placementHit.hitResult();
        } else {
            BlockHitResult playerHit = currentBlockHit();
            if (playerHit == null) return;
            BlockPos lookedAt = playerHit.getBlockPos();
            if (lookedAt.equals(activeAnchorPos)) return;

            BlockPos placement = lookedAt.relative(playerHit.getDirection());
            if (placement.equals(activeAnchorPos)) return;
            if (!BlockUtils.isValidPlacement(placement)) return;
            if (BlockUtils.collidesWithPlayer(placement)) return;
            if (BlockUtils.collidesWithBlockingEntity(placement)) return;
            hitResult = playerHit;
        }

        int safetySlot = findSafetyBlockSlot();
        if (safetySlot == -1) {
            advanceToChargeStep();
            return;
        }

        if (!SwapStateManager.swapToIfNeeded(this, safetySlot, false, -1, false)) return;
        if (placeCooldown > 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;

        InteractionResult result = BlockUtils.interactWithBlock(hitResult, true);
        placeCooldown = VANILLA_PLACE_COOLDOWN_TICKS;
        if (result.consumesAction()) {
            rotation.markInteraction();
            advanceToChargeStep();
        }
    }

    private boolean shouldPlaceSafety(List<BlockPos> ranked) {
        if (!safeAnchor.getValue()) return false;
        if (mc.player == null || mc.level == null || activeAnchorPos == null) return false;

        // Below the damage threshold the explosion isn't worth a glowstone.
        float threshold = minDamage.getValue();
        if (threshold > 0.0f && ExplosionUtils.getAnchorDamageTo(mc.player, activeAnchorPos) < threshold) {
            return false;
        }

        // If the closest-to-anchor neighbor is already a solid block, we're already covered.
        BlockPos ideal = ranked.isEmpty() ? null : ranked.get(0);
        return ideal == null || !hasExistingSafetyBlock(ideal);
    }

    /**
     * 8 horizontal neighbors of the player's feet, restricted to the half-plane
     * toward the anchor (so the safety can never end up "to the right" or
     * behind us), sorted by distance to the anchor center -- the spot that
     * sits most directly between us and the anchor comes first.
     */
    private List<BlockPos> rankedSafetyCandidates() {
        List<BlockPos> ordered = new ArrayList<>(8);
        if (mc.player == null || activeAnchorPos == null) return ordered;

        float partialTick = partialTick();
        BlockPos feet = BlockPos.containing(mc.player.getPosition(partialTick));
        Vec3 feetCenter = Vec3.atCenterOf(feet);
        Vec3 anchorCenter = Vec3.atCenterOf(activeAnchorPos);
        Vec3 toAnchor = anchorCenter.subtract(feetCenter);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = feet.offset(dx, 0, dz);
                Vec3 toCand = Vec3.atCenterOf(pos).subtract(feetCenter);
                if (toCand.x * toAnchor.x + toCand.z * toAnchor.z <= 0) continue; // behind / perpendicular
                ordered.add(pos);
            }
        }
        ordered.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(anchorCenter)));
        return ordered;
    }

    private BlockUtils.PlacementHit pickSafetyPlacement(List<BlockPos> ranked) {
        for (BlockPos pos : ranked) {
            BlockUtils.PlacementHit hit = trySafetyPlacementAt(pos);
            if (hit != null) return hit;
        }
        return null;
    }

    private BlockUtils.PlacementHit trySafetyPlacementAt(BlockPos pos) {
        if (pos == null || mc.level == null || mc.player == null) return null;
        if (pos.equals(activeAnchorPos)) return null;
        if (!BlockUtils.isValidPlacement(pos)) return null;
        if (!BlockUtils.hasSupportingFace(pos)) return null;
        if (BlockUtils.collidesWithPlayer(pos)) return null;
        if (BlockUtils.collidesWithBlockingEntity(pos)) return null;

        BlockUtils.PlacementHit hit = BlockUtils.findPlacementHit(pos, canPlaceLegit.getValue());
        if (hit == null) return null;

        Vec3 eye = mc.player.getEyePosition(partialTick());
        double range = mc.player.blockInteractionRange();
        if (eye.distanceToSqr(hit.hitResult().getLocation()) > range * range) return null;
        return hit;
    }

    private boolean hasExistingSafetyBlock(BlockPos pos) {
        if (mc.level == null || pos == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private int findSafetyBlockSlot() {
        int slot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.is(Items.GLOWSTONE));
        if (slot != -1) return slot;
        return InventoryUtils.findItemWithPredicateInHotbar(stack ->
                stack.getItem() instanceof BlockItem && !stack.is(Items.RESPAWN_ANCHOR));
    }

    // -- Step: CHARGE --

    private void chargeActiveAnchor() {
        int currentCharge = getAnchorCharge(activeAnchorPos);
        if (remainingCharges <= 0 || currentCharge >= MAX_ANCHOR_CHARGES) {
            beginExplodeStepOrFinish();
            return;
        }

        int glowstoneSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.is(Items.GLOWSTONE));
        if (glowstoneSlot == -1) {
            remainingCharges = 0;
            beginExplodeStepOrFinish();
            return;
        }

        if (!SwapStateManager.swapToIfNeeded(this, glowstoneSlot, false, -1, false)) return;
        if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) return;

        BlockHitResult hitResult = getReadyAnchorHit(activeAnchorPos);
        if (hitResult == null) return;

        InteractionResult result = BlockUtils.interactWithBlock(hitResult, true);
        if (!result.consumesAction()) return;

        rotation.markInteraction();
        remainingCharges--;
        if (remainingCharges <= 0) {
            anchorStep = AnchorStep.EXPLODE;
            nextAnchorActionTick = mc.player.tickCount + rollTickDelay(explodeDelay);
            return;
        }
        nextAnchorActionTick = mc.player.tickCount + rollTickDelay(chargeDelay);
    }

    // -- Step: EXPLODE --

    private void explodeActiveAnchor() {
        int currentCharge = getAnchorCharge(activeAnchorPos);
        if (currentCharge <= 0) {
            finishAnchorSequence();
            return;
        }

        int targetSlot = explodeSlot.getValue() - 1;
        if (!SwapStateManager.swapToIfNeeded(this, targetSlot, false, -1, false)) return;

        // Glowstone in the explode slot would just charge again; bail unless the anchor is already maxed.
        ItemStack explodeStack = mc.player.getInventory().getItem(targetSlot);
        if (explodeStack.is(Items.GLOWSTONE) && currentCharge < MAX_ANCHOR_CHARGES) {
            finishAnchorSequence();
            return;
        }

        BlockHitResult hitResult = getReadyAnchorHit(activeAnchorPos);
        if (hitResult == null) return;

        InteractionResult result = BlockUtils.interactWithBlock(hitResult, true);
        if (!result.consumesAction()) return;

        rotation.markInteraction();
        placeCooldown = VANILLA_PLACE_COOLDOWN_TICKS;
        finishAnchorSequence();
    }

    // -- Aim picking --

    private Vec3 findActiveAimPoint() {
        if (isAnchorSequenceActive()) {
            if (anchorStep == AnchorStep.PLACE_SAFETY) {
                List<BlockPos> ranked = rankedSafetyCandidates();
                if (shouldPlaceSafety(ranked)) {
                    BlockUtils.PlacementHit safetyHit = pickSafetyPlacement(ranked);
                    if (safetyHit != null) return safetyHit.hitResult().getLocation();
                }
            }
            return getAnchorAimPoint(activeAnchorPos);
        }

        BlockHitResult anchorHit = currentBlockHit();
        if (anchorHit != null && isRespawnAnchor(anchorHit.getBlockPos())) {
            return anchorHit.getLocation();
        }

        float partialTick = partialTick();
        BlockUtils.PlacementHit placementHit = findBestPlacementHit(partialTick);
        if (placementHit != null) return placementHit.hitResult().getLocation();

        BlockHitResult manualHit = currentBlockHit();
        return manualHit != null ? manualHit.getLocation() : null;
    }

    private Vec3 getAnchorAimPoint(BlockPos anchorPos) {
        if (anchorPos == null) return null;
        if (mc.player == null) return Vec3.atCenterOf(anchorPos);

        float partialTick = partialTick();
        // Reuse the cached aim if it still raycasts cleanly. A block placed
        // since the sequence started (e.g. our safety glowstone) can occlude
        // the original corner -- in that case re-pick.
        if (cachedAnchorAim != null && anchorPos.equals(activeAnchorPos)) {
            Vec3 eye = mc.player.getEyePosition(partialTick);
            if (AnchorAimSearch.canHitAnchor(eye, cachedAnchorAim, anchorPos)) return cachedAnchorAim;
            cachedAnchorAim = AnchorAimSearch.findClearAimPoint(anchorPos, partialTick);
            return cachedAnchorAim != null ? cachedAnchorAim : Vec3.atCenterOf(anchorPos);
        }
        Vec3 fresh = AnchorAimSearch.findClearAimPoint(anchorPos, partialTick);
        return fresh != null ? fresh : Vec3.atCenterOf(anchorPos);
    }

    private boolean isReadyToPlace(BlockUtils.PlacementHit placementHit, float partialTick) {
        float yaw = effectiveYaw();
        float pitch = effectivePitch();

        // Grim's DuplicateRotPlace compares pitch delta against the previous
        // place packet. Falling changes the needed pitch every tick, so allow
        // those non-duplicate deltas instead of waiting until we land.
        if (!rotation.isDuplicateRotPlaceSafe(ROTATION_SETTLE_THRESHOLD)) return false;

        if (!canPlaceLegit.getValue()) {
            return RotationUtils.rayIntersectsBlock(yaw, pitch, placementHit.hitResult().getBlockPos(), partialTick);
        }

        BlockHitResult raycast = RotationUtils.raycastFromRotation(yaw, pitch, partialTick);
        if (raycast == null || raycast.getType() != HitResult.Type.BLOCK) return false;
        if (!raycast.getBlockPos().equals(placementHit.hitResult().getBlockPos())) return false;
        return raycast.getDirection() == placementHit.hitResult().getDirection();
    }

    private BlockHitResult getReadyAnchorHit(BlockPos anchorPos) {
        if (!isAnchorWithinInteractionRange(anchorPos)) return null;

        // Grim's DuplicateRotPlace compares pitch delta against the previous
        // place packet. Falling changes the needed pitch every tick, so allow
        // those non-duplicate deltas instead of waiting until we land.
        if (!rotation.isDuplicateRotPlaceSafe(ROTATION_SETTLE_THRESHOLD)) return null;

        float yaw = effectiveYaw();
        float pitch = effectivePitch();
        float partialTick = partialTick();

        if (!canPlaceLegit.getValue()) {
            if (!RotationUtils.rayIntersectsBlock(yaw, pitch, anchorPos, partialTick)) return null;
            return new BlockHitResult(getAnchorAimPoint(anchorPos), Direction.UP, anchorPos, false);
        }

        BlockHitResult currentHit = currentBlockHit();
        if (currentHit != null && currentHit.getBlockPos().equals(anchorPos)) return currentHit;

        BlockHitResult raycast = RotationUtils.raycastFromRotation(yaw, pitch, partialTick);
        if (raycast == null || raycast.getType() != HitResult.Type.BLOCK) return null;
        if (!raycast.getBlockPos().equals(anchorPos)) return null;
        return raycast;
    }

    // -- Best-placement search (anchor placement candidate) --

    private BlockUtils.PlacementHit findBestPlacementHit(float partialTick) {
        Vec3 playerEye = mc.player.getEyePosition(partialTick);
        LivingEntity target = TargetUtils.findClosestResolvedTarget(playerEye, partialTick);
        if (target == null) return null;

        double blockRange = mc.player.blockInteractionRange();
        double blockRangeSq = blockRange * blockRange;

        Vec3 targetPos = target.getPosition(partialTick);
        Vec3 offset = targetPos.subtract(target.position());
        AABB targetBox = target.getBoundingBox().move(offset);
        Vec3 targetFeet = new Vec3(targetPos.x, targetBox.minY, targetPos.z);

        int floorX = Mth.floor(targetFeet.x);
        int floorY = Mth.floor(targetFeet.y);
        int floorZ = Mth.floor(targetFeet.z);

        BlockUtils.PlacementHit bestHit = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos pos = new BlockPos(floorX + dx, floorY + dy, floorZ + dz);

                    if (!BlockUtils.isValidPlacement(pos)) continue;
                    if (!BlockUtils.hasSupportingFace(pos)) continue;

                    BlockUtils.PlacementHit hit = BlockUtils.findPlacementHit(pos, canPlaceLegit.getValue());
                    if (hit == null) continue;
                    if (BlockUtils.collidesWithPlayer(pos)) continue;
                    if (BlockUtils.hasBlockingEntity(new AABB(pos))) continue;
                    if (playerEye.distanceToSqr(hit.hitResult().getLocation()) > blockRangeSq) continue;

                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    float exposure = ExplosionUtils.getSeenPercent(blockCenter, targetBox, target);
                    double feetDistSq = targetFeet.distanceToSqr(blockCenter);
                    // Prefer high-exposure hits; break ties by proximity to feet.
                    double score = (1.0 - exposure) * 100.0 + feetDistSq * 0.01;

                    if (score < bestScore) {
                        bestScore = score;
                        bestHit = hit;
                    }
                }
            }
        }
        return bestHit;
    }

    // -- Misc helpers --

    private float partialTick() {
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private float effectiveYaw() {
        return RotationHandler.isHasSilentRotation() ? RotationHandler.getServerYaw() : mc.player.getYRot();
    }

    private float effectivePitch() {
        return RotationHandler.isHasSilentRotation() ? RotationHandler.getServerPitch() : mc.player.getXRot();
    }

    private boolean isActionActive() {
        if (mode.is(MODE_HOLD)) {
            if (!InputHandler.isBindDown(activateKey.getKey())) return false;
            return repeat.getValue() || !holdCycleCompleted || isAnchorSequenceActive();
        }
        return clickQueued || isAnchorSequenceActive();
    }

    private boolean shouldHandleSameUseKey() {
        if (mc == null || mc.options == null || mc.options.keyUse == null) return false;
        return activateKey.getKey() != -1 && activateKey.getKey() == bindCodeOf(mc.options.keyUse);
    }

    private int bindCodeOf(KeyMapping keyMapping) {
        if (keyMapping == null) return -1;
        InputConstants.Key key = ((KeyMappingAccessor) keyMapping).getKey();
        if (key == null) return -1;
        if (key.getType() == InputConstants.Type.MOUSE) return 0x80000000 | key.getValue();
        return key.getValue();
    }

    private boolean isHoldingRespawnAnchor() {
        return mc.player != null && mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR);
    }

    private BlockHitResult currentBlockHit() {
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return null;
        if (blockHit.getType() != HitResult.Type.BLOCK) return null;
        return blockHit;
    }

    private BlockPos consumePendingUseKeyAnchorPos() {
        BlockPos pos = pendingUseKeyAnchorPos;
        pendingUseKeyAnchorPos = null;
        if (pos == null) return null;
        return isRespawnAnchor(pos) ? pos : null;
    }

    private BlockPos anchorPosOf(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHit)) return null;
        if (blockHit.getType() != HitResult.Type.BLOCK) return null;
        return isRespawnAnchor(blockHit.getBlockPos()) ? blockHit.getBlockPos() : null;
    }

    private BlockPos placedAnchorPosOf(BlockHitResult hitResult) {
        return hitResult == null ? null : hitResult.getBlockPos().relative(hitResult.getDirection());
    }

    private boolean isManualAnchorPlacementValid(BlockPos pos) {
        if (pos == null) return false;
        if (!BlockUtils.isValidPlacement(pos)) return false;
        if (BlockUtils.collidesWithPlayer(pos)) return false;
        return !BlockUtils.collidesWithBlockingEntity(pos);
    }

    private boolean isRespawnAnchor(BlockPos pos) {
        if (pos == null || mc.level == null) return false;
        return BlockUtils.isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR);
    }

    private int getAnchorCharge(BlockPos pos) {
        if (!isRespawnAnchor(pos)) return 0;
        return mc.level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE);
    }

    private boolean isAnchorWithinInteractionRange(BlockPos anchorPos) {
        return mc.player != null && anchorPos != null && mc.player.isWithinBlockInteractionRange(anchorPos, 1.0);
    }

    private void handleInactive() {
        resetAnchorSequence();
        if (SwapStateManager.isOwnerActive(this) && !restoringSwap) scheduleSwapBack(1);
        disengageRotations();
    }

    private void scheduleSwapBack(int delayTicks) {
        if (!SwapStateManager.isOwnerActive(this)) return;
        int activeSlot = SwapStateManager.getActiveTargetSlot(this);
        if (activeSlot == -1) {
            SwapStateManager.cancel(this, true);
            restoringSwap = false;
            return;
        }
        SwapStateManager.swapToIfNeeded(this, activeSlot, false, delayTicks, true);
        restoringSwap = true;
    }

    private void disengageRotations() {
        if (!RotationUtils.isControlledBy(this)) {
            wasAiming = false;
            return;
        }
        if (RotationHandler.isHasSilentRotation() || silentAim.getValue()) {
            if (RotationUtils.cleanup(this, true, aimSpeed.getValue(), aimSpeed.getValue(), RotationUtils.AimType.REGULAR, false)) return;
        } else {
            RotationUtils.stopTracking();
        }
        wasAiming = false;
    }

    // Random rolls -- delegate to RangeSetting.getRandom() and clamp to int.
    private int rollChargeCount() {
        return Mth.clamp(Math.round(charges.getRandom()), 1, MAX_ANCHOR_CHARGES);
    }

    private int rollTickDelay(RangeSetting setting) {
        return Math.max(0, Math.round(setting.getRandom()));
    }
}
