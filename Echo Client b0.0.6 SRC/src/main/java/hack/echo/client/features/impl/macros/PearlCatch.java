package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.features.impl.player.AutoTotem;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.simulation.PearlSimulation;
import hack.echo.client.utils.simulation.WindChargeSimulation;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.trajectory.TrajectoryLaunchUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PearlCatch extends Feature {

    private static final float PEARL_SPEED = 1.5F;
    private static final float WIND_CHARGE_SPEED = 1.5F;
    private static final int PRE_THROW_AIM_TICKS = 1;
    // Accounts for the pearl already being in flight before the wind charge can spawn and move.
    private static final int PEARL_WIND_ALIGNMENT_OFFSET = 2;
    private static final int TRACKING_TIMEOUT_TICKS = 60;
    private static final int TRACKING_SEARCH_AHEAD_TICKS = 50;
    private static final double COARSE_ACCEPT_DISTANCE = 1.5;
    private static final double REFINED_ACCEPT_DISTANCE = 1.2;
    private static final double AIM_OFFSET_RADIANS = 0.035;
    private static final float MIN_AIM_SPEED = 400.0F;
    private static final float MAX_AIM_SPEED = 1800.0F;
    private static final float AIM_SPEED_BUFFER = 1.35F;

    private final BoolSetting autoAim = new BoolSetting(Concat.of("Auto Aim"), true);
    private final ModeSetting aimMath = new ModeSetting(Concat.of("Aim Math"), Concat.of("Blatant"), Concat.of("Regular"), Concat.of("Blatant"), Concat.of("WindMouse"));
    private final BoolSetting silentAim = new BoolSetting(Concat.of("Silent Aim"), true, s -> autoAim.getValue());
    private final RangeSetting highDelay = new RangeSetting(Concat.of("High Delay"), 2.0F, 2.0F, 0.0F, 10.0F, 1.0F, Concat.of(" ticks"));
    private final IntSetting predictionTicks = new IntSetting(Concat.of("Prediction Range"), 60, 20, 100);
    private final BoolSetting switchBack = new BoolSetting(Concat.of("Switch Back"), true);
    private final KeybindSetting highActivateKey = new KeybindSetting(Concat.of("High Pearl Catch"), -1);
    private final KeybindSetting lowActivateKey = new KeybindSetting(Concat.of("Low Pearl Catch"), -1);
    private final KeybindSetting instantActivateKey = new KeybindSetting(Concat.of("Instant Pearl Catch"), -1);

    private enum State { IDLE, THROW_PEARL, WAIT, TRACKING, PRE_THROW, THROW_WIND_CHARGE, SWITCH_BACK }
    private enum NormalMode { HIGH, LOW }
    private enum InstantState {
        IDLE,
        SELECT_WIND_CHARGE,
        WAIT_WIND_CHARGE_SELECTED,
        SWAP_WIND_CHARGE_TO_OFFHAND,
        WAIT_WIND_CHARGE_OFFHAND,
        SELECT_PEARL,
        WAIT_PEARL_SELECTED,
        THROW_BOTH,
        RESTORE_SELECT_WIND_CHARGE,
        RESTORE_WAIT_WIND_CHARGE_SELECTED,
        RESTORE_SWAP_OFFHAND,
        RESTORE_WAIT_OFFHAND,
        RESTORE_SELECTED_SLOT
    }

    private State state = State.IDLE;
    private InstantState instantState = InstantState.IDLE;
    private boolean highKeyWasDown = false;
    private boolean lowKeyWasDown = false;
    private boolean instantKeyWasDown = false;
    private boolean wasTracking = false;
    private int tickCounter = 0;
    private int pearlThrowTick = -1;
    private int noSolutionTicks = 0;
    private int sampledDelayTicks = 2;
    private int originalSelectedSlot = -1;
    private int instantWindChargeSlot = -1;
    private int instantPearlSlot = -1;
    private boolean instantMovedWindChargeToOffhand = false;
    private int instantTicksInState = 0;

    private Vec3 targetAimPoint;
    private InterceptPlan targetPlan;
    private final List<Vec3> predictedPearlPath = new ArrayList<>();

    public PearlCatch() {
        super(new FeatureInfo(
            Concat.of("Pearl Catch"),
            Concat.of("Automatically catches ender pearls thrown by the player."),
            Category.MACROS
        ));
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onTick(EventTick event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        handleActivationKeys();

        if (instantState != InstantState.IDLE) {
            handleInstantState();
            return;
        }

        switch (state) {
            case THROW_PEARL -> handlePearlThrow();
            case WAIT -> handleWait();
            case TRACKING -> handleTracking();
            case PRE_THROW -> handlePreThrow();
            case THROW_WIND_CHARGE -> handleThrowWindCharge();
            case SWITCH_BACK -> handleSwitchBack();
            default -> {}
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onAim(MouseUpdateEvent event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!autoAim.getValue()) return;

        if ((state == State.TRACKING || state == State.PRE_THROW) && targetAimPoint != null) {
            boolean rotated = RotationUtils.aim(this)
                .silent(silentAim.getValue())
                .aimType(getAimType())
                .speed(getDynamicAimSpeed(targetAimPoint))
                .to(targetAimPoint);
            wasTracking = true;

            if (state == State.TRACKING && rotated && hasReachedAimPoint(targetAimPoint)) {
                state = State.PRE_THROW;
                tickCounter = 0;
            }
        } else if (state == State.IDLE && wasTracking) {
            if (!RotationUtils.cleanup(this, silentAim.getValue())) {
                wasTracking = false;
            }
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onSilentAim(EventMove.Pre event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!autoAim.getValue() || !silentAim.getValue()) return;
        if (state != State.TRACKING && state != State.PRE_THROW && state != State.THROW_WIND_CHARGE && !wasTracking) return;
        if (!RotationUtils.hasSilentRotation()) return;

        event.setYaw(RotationUtils.getSilentYaw());
        event.setPitch(RotationUtils.getSilentPitch());
    }

    @Override
    public void onDisable() {
        AutoTotem.blocked = false;
        if (RotationUtils.isControlledBy(this)) {
            RotationUtils.stopTracking();
        }
        wasTracking = false;
        resetState(false);
        resetInstantState();
        super.onDisable();
    }

    private void handleActivationKeys() {
        boolean highKeyDown = InputHandler.isBindDown(highActivateKey.getKey());
        boolean lowKeyDown = InputHandler.isBindDown(lowActivateKey.getKey());
        boolean instantKeyDown = InputHandler.isBindDown(instantActivateKey.getKey());

        if (highKeyDown && !highKeyWasDown) {
            startNormalSequence(NormalMode.HIGH);
        }
        if (lowKeyDown && !lowKeyWasDown) {
            startNormalSequence(NormalMode.LOW);
        }
        if (instantKeyDown && !instantKeyWasDown) {
            startInstantSequence();
        }

        highKeyWasDown = highKeyDown;
        lowKeyWasDown = lowKeyDown;
        instantKeyWasDown = instantKeyDown;
    }

    private void startNormalSequence(NormalMode mode) {
        if (state != State.IDLE || instantState != InstantState.IDLE) return;
        if (hasPearlCatchCooldown()) return;

        int pearlSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearlSlot == -1) return;

        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windChargeSlot == -1) return;

        if (!SwapStateManager.swapToIfNeeded(this, pearlSlot, false, -1, false)) return;
        sampledDelayTicks = mode == NormalMode.HIGH ? Math.max(0, Math.round(highDelay.getRandom())) : 0;
        state = State.THROW_PEARL;
        tickCounter = 0;
        noSolutionTicks = 0;
    }

    private void handlePearlThrow() {
        int pearlSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearlSlot == -1) {
            resetState(false);
            return;
        }

        if (!SwapStateManager.swapToIfNeeded(this, pearlSlot, false, -1, false)) {
            resetState(false);
            return;
        }
        updatePearlPrediction();
        pearlThrowTick = mc.player.tickCount;

        ((MinecraftAccessor) mc).invokeStartUseItem();
        state = State.WAIT;
        tickCounter = 0;
    }

    private void handleWait() {
        tickCounter++;
        if (tickCounter < sampledDelayTicks) return;

        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windChargeSlot == -1) {
            resetState(false);
            return;
        }

        if (!ensureWindChargeSelected(windChargeSlot)) {
            resetState(false);
            return;
        }

        if (!autoAim.getValue()) {
            state = State.THROW_WIND_CHARGE;
            return;
        }

        if (silentAim.getValue()) {
            RotationUtils.initFromPlayer();
        }

        int ticksSinceThrow = Math.max(0, mc.player.tickCount - pearlThrowTick);
        if (!updateInterceptSolution(ticksSinceThrow)) {
            state = State.THROW_WIND_CHARGE;
            return;
        }

        state = State.TRACKING;
    }

    private void handleTracking() {
        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windChargeSlot == -1) {
            resetState(false);
            return;
        }

        if (!ensureWindChargeSelected(windChargeSlot)) {
            resetState(false);
            return;
        }

        int ticksSinceThrow = mc.player.tickCount - pearlThrowTick;
        if (ticksSinceThrow > TRACKING_TIMEOUT_TICKS || ticksSinceThrow >= predictedPearlPath.size() - 5) {
            state = State.THROW_WIND_CHARGE;
            return;
        }

        boolean found = updateInterceptSolution(ticksSinceThrow);
        if (found) {
            noSolutionTicks = 0;
            return;
        }

        noSolutionTicks++;
        if (noSolutionTicks > 5) {
            state = State.THROW_WIND_CHARGE;
        }
    }

    private void handlePreThrow() {
        int windChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windChargeSlot == -1) {
            resetState(false);
            return;
        }

        if (!ensureWindChargeSelected(windChargeSlot)) {
            resetState(false);
            return;
        }

        int ticksSinceThrow = mc.player.tickCount - pearlThrowTick;
        updateInterceptSolution(ticksSinceThrow);
        if (targetAimPoint == null || !hasReachedAimPoint(targetAimPoint)) {
            state = State.TRACKING;
            return;
        }

        tickCounter++;
        if (tickCounter >= PRE_THROW_AIM_TICKS) {
            handleThrowWindCharge();
        }
    }

    private void handleThrowWindCharge() {
        ((MinecraftAccessor) mc).invokeStartUseItem();

        if (switchBack.getValue()) {
            state = State.SWITCH_BACK;
            tickCounter = 0;
        } else {
            resetState(false);
        }
    }

    private void handleSwitchBack() {
        tickCounter++;
        if (tickCounter < 1) return;

        resetState(switchBack.getValue());
    }

    private boolean ensureWindChargeSelected(int windChargeSlot) {
        if (SwapStateManager.getActiveTargetSlot(this) == windChargeSlot) {
            return true;
        }

        if (SwapStateManager.swapTo(this, windChargeSlot, false, -1, false)) {
            return true;
        }

        return mc.player.getInventory().getSelectedSlot() == windChargeSlot;
    }

    private void resetState(boolean restore) {
        SwapStateManager.cancel(this, restore);
        state = State.IDLE;
        tickCounter = 0;
        noSolutionTicks = 0;
        pearlThrowTick = -1;
        sampledDelayTicks = 2;
        targetAimPoint = null;
        targetPlan = null;
        predictedPearlPath.clear();
    }

    private void startInstantSequence() {
        if (state != State.IDLE || instantState != InstantState.IDLE) return;
        if (hasPearlCatchCooldown()) return;

        originalSelectedSlot = mc.player.getInventory().getSelectedSlot();
        instantMovedWindChargeToOffhand = !mc.player.getOffhandItem().is(Items.WIND_CHARGE);
        instantWindChargeSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        instantPearlSlot = InventoryUtils.findItemWithPredicateInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);

        if (isInstantReady()) {
            AutoTotem.blocked = true;
            InputHandler.simulateClick(mc.options.keyUse, false);
            resetInstantState();
            return;
        }

        if (instantPearlSlot == -1) {
            resetInstantState();
            return;
        }
        if (instantMovedWindChargeToOffhand && instantWindChargeSlot == -1) {
            resetInstantState();
            return;
        }

        if (instantMovedWindChargeToOffhand) {
            AutoTotem.blocked = true;
            setInstantState(InstantState.SELECT_WIND_CHARGE);
            return;
        }

        AutoTotem.blocked = true;
        setInstantState(InstantState.SELECT_PEARL);
    }

    private void handleInstantState() {
        if (instantTicksInState > 10) {
            resetInstantState();
            return;
        }

        instantTicksInState++;

        switch (instantState) {
            case SELECT_WIND_CHARGE -> {
                InventoryUtils.setInvSlot(instantWindChargeSlot, true);
                setInstantState(InstantState.WAIT_WIND_CHARGE_SELECTED);
            }
            case WAIT_WIND_CHARGE_SELECTED -> {
                if (mc.player.getInventory().getSelectedSlot() != instantWindChargeSlot) return;
                setInstantState(InstantState.SWAP_WIND_CHARGE_TO_OFFHAND);
            }
            case SWAP_WIND_CHARGE_TO_OFFHAND -> {
                InputHandler.simulateClick(mc.options.keySwapOffhand, false);
                setInstantState(InstantState.WAIT_WIND_CHARGE_OFFHAND);
            }
            case WAIT_WIND_CHARGE_OFFHAND -> {
                if (!mc.player.getOffhandItem().is(Items.WIND_CHARGE)) return;
                setInstantState(InstantState.SELECT_PEARL);
            }
            case SELECT_PEARL -> {
                InventoryUtils.setInvSlot(instantPearlSlot, true);
                setInstantState(InstantState.WAIT_PEARL_SELECTED);
            }
            case WAIT_PEARL_SELECTED -> {
                if (!mc.player.getMainHandItem().is(Items.ENDER_PEARL)) return;
                setInstantState(InstantState.THROW_BOTH);
            }
            case THROW_BOTH -> {
                if (hasPearlCatchCooldown()) {
                    resetInstantState();
                    return;
                }
                InputHandler.simulateClick(mc.options.keyUse, false);
                // needs to be called another time for offhand to be used
                InputHandler.simulateClick(mc.options.keyUse, false);
                if (!instantMovedWindChargeToOffhand) {
                    resetInstantState();
                    return;
                }
                setInstantState(InstantState.RESTORE_SELECT_WIND_CHARGE);
            }
            case RESTORE_SELECT_WIND_CHARGE -> {
                InventoryUtils.setInvSlot(instantWindChargeSlot, true);
                setInstantState(InstantState.RESTORE_WAIT_WIND_CHARGE_SELECTED);
            }
            case RESTORE_WAIT_WIND_CHARGE_SELECTED -> {
                if (mc.player.getInventory().getSelectedSlot() != instantWindChargeSlot) return;
                setInstantState(InstantState.RESTORE_SWAP_OFFHAND);
            }
            case RESTORE_SWAP_OFFHAND -> {
                InputHandler.simulateClick(mc.options.keySwapOffhand, false);
                setInstantState(InstantState.RESTORE_WAIT_OFFHAND);
            }
            case RESTORE_WAIT_OFFHAND -> {
                if (mc.player.getOffhandItem().is(Items.WIND_CHARGE)) return;
                setInstantState(InstantState.RESTORE_SELECTED_SLOT);
            }
            case RESTORE_SELECTED_SLOT -> {
                if (originalSelectedSlot != -1) {
                    InventoryUtils.setInvSlot(originalSelectedSlot, true);
                }
                resetInstantState();
            }
            default -> {}
        }
    }

    private void setInstantState(InstantState nextState) {
        instantState = nextState;
        instantTicksInState = 0;
    }

    private boolean isInstantReady() {
        return mc.player.getMainHandItem().is(Items.ENDER_PEARL)
            && mc.player.getOffhandItem().is(Items.WIND_CHARGE);
    }

    private boolean hasPearlCatchCooldown() {
        return mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))
            || mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE));
    }

    private void resetInstantState() {
        AutoTotem.blocked = false;
        instantState = InstantState.IDLE;
        instantTicksInState = 0;
        originalSelectedSlot = -1;
        instantWindChargeSlot = -1;
        instantPearlSlot = -1;
        instantMovedWindChargeToOffhand = false;
    }

    private Vec3 getInheritedVelocity(Vec3 rawVelocity) {
        return mc.player.onGround() ? new Vec3(rawVelocity.x, 0.0, rawVelocity.z) : rawVelocity;
    }

    private Vec3 getPredictedEyePos() {
        Vec3 predictedPos = mc.player.position().add(mc.player.getDeltaMovement());
        return new Vec3(predictedPos.x, predictedPos.y + mc.player.getEyeHeight(), predictedPos.z);
    }

    private void updatePearlPrediction() {
        predictedPearlPath.clear();

        Vec3 spawnPos = TrajectoryLaunchUtils.getProjectileSpawnPos(mc.player, 1.0F);
        Vec3 velocity = TrajectoryLaunchUtils.getLaunchDirection(mc.player, 1.0F)
            .scale(PEARL_SPEED)
            .add(TrajectoryLaunchUtils.getInheritedVelocity(mc.player));

        PearlSimulation simulation = new PearlSimulation(spawnPos, velocity, mc.player);
        predictedPearlPath.add(simulation.getPosition());
        for (int i = 0; i < predictionTicks.getValue() && !simulation.hasHit(); i++) {
            simulation.simulateTick();
            predictedPearlPath.add(simulation.getPosition());
        }
    }

    private boolean updateInterceptSolution(int currentPearlTick) {
        if (predictedPearlPath.isEmpty()) {
            targetAimPoint = null;
            targetPlan = null;
            return false;
        }

        InterceptPlan result = solveInterceptWithPrediction(currentPearlTick, getPredictedEyePos(), mc.player.getDeltaMovement());
        if (result == null) {
            targetAimPoint = null;
            targetPlan = null;
            return false;
        }

        targetAimPoint = result.aimPoint;
        targetPlan = result;
        return true;
    }

    private InterceptPlan solveInterceptWithPrediction(int currentPearlTick, Vec3 predictedEyePos, Vec3 predictedVel) {
        Vec3 inheritedVelocity = getInheritedVelocity(predictedVel);
        InterceptPlan bestResult = null;

        int maxTravelTicks = Math.min(
            TRACKING_SEARCH_AHEAD_TICKS,
            predictedPearlPath.size() - currentPearlTick - PEARL_WIND_ALIGNMENT_OFFSET - 1
        );

        for (int travelTicks = 1; travelTicks <= maxTravelTicks; travelTicks++) {
            int pearlTick = currentPearlTick + travelTicks + PEARL_WIND_ALIGNMENT_OFFSET;
            InterceptPlan directPlan = buildInterceptPlan(predictedEyePos, inheritedVelocity, pearlTick, travelTicks);
            bestResult = pickBetter(bestResult, directPlan);

            if (directPlan != null && directPlan.missDistance < REFINED_ACCEPT_DISTANCE) {
                return refineAim(predictedEyePos, inheritedVelocity, directPlan);
            }
        }

        return bestResult;
    }

    private InterceptPlan refineAim(Vec3 windStartPos, Vec3 inheritedVelocity, InterceptPlan coarse) {
        InterceptPlan bestResult = coarse;

        for (double yawOff = -AIM_OFFSET_RADIANS; yawOff <= AIM_OFFSET_RADIANS; yawOff += AIM_OFFSET_RADIANS) {
            for (double pitchOff = -AIM_OFFSET_RADIANS; pitchOff <= AIM_OFFSET_RADIANS; pitchOff += AIM_OFFSET_RADIANS) {
                Vec3 testAim = rotateVector(coarse.aimDirection, pitchOff, yawOff);
                InterceptPlan testPlan = validateAim(windStartPos, inheritedVelocity, testAim, coarse.pearlTick, coarse.travelTicks);
                bestResult = pickBetter(bestResult, testPlan);
            }
        }

        return bestResult;
    }

    private InterceptPlan buildInterceptPlan(Vec3 windStartPos, Vec3 inheritedVelocity, int pearlTick, int travelTicks) {
        Vec3 targetPearlPos = predictedPearlPath.get(pearlTick);
        Vec3 requiredProjectileMovement = targetPearlPos
            .subtract(windStartPos)
            .subtract(inheritedVelocity.scale(travelTicks));

        double requiredDistance = requiredProjectileMovement.length();
        if (requiredDistance < 1.0E-6) return null;

        double speedError = Math.abs(requiredDistance - WIND_CHARGE_SPEED * travelTicks);
        if (speedError > COARSE_ACCEPT_DISTANCE) return null;

        Vec3 aim = requiredProjectileMovement.scale(1.0 / requiredDistance);
        return validateAim(windStartPos, inheritedVelocity, aim, pearlTick, travelTicks);
    }

    private InterceptPlan validateAim(Vec3 windStartPos, Vec3 inheritedVelocity, Vec3 aim, int pearlTick, int travelTicks) {
        Vec3 windVelocity = aim.scale(WIND_CHARGE_SPEED).add(inheritedVelocity);
        WindChargeSimulation simulation = new WindChargeSimulation(windStartPos, windVelocity, mc.player);

        Vec3 windPos = windStartPos;
        for (int i = 0; i < travelTicks && !simulation.hasHit(); i++) {
            simulation.simulateTick();
            windPos = simulation.getPosition();
        }

        double missDistance = windPos.distanceTo(predictedPearlPath.get(pearlTick));
        if (missDistance > COARSE_ACCEPT_DISTANCE) return null;

        return new InterceptPlan(windStartPos.add(aim.scale(100.0)), aim, missDistance, pearlTick, travelTicks);
    }

    private InterceptPlan pickBetter(InterceptPlan current, InterceptPlan candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        return candidate.missDistance < current.missDistance ? candidate : current;
    }

    private Vec3 rotateVector(Vec3 vec, double pitchOffset, double yawOffset) {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;

        double cosYaw = Math.cos(yawOffset);
        double sinYaw = Math.sin(yawOffset);
        double newX = x * cosYaw - z * sinYaw;
        double newZ = x * sinYaw + z * cosYaw;

        double cosPitch = Math.cos(pitchOffset);
        double sinPitch = Math.sin(pitchOffset);
        double newY = y * cosPitch - newZ * sinPitch;
        newZ = y * sinPitch + newZ * cosPitch;

        Vec3 rotated = new Vec3(newX, newY, newZ);
        return rotated.lengthSqr() < 1.0E-8 ? vec : rotated.normalize();
    }

    private RotationUtils.AimType getAimType() {
        if (aimMath.is(Concat.of("WindMouse"))) return RotationUtils.AimType.WINDMOUSE;
        return aimMath.is(Concat.of("Blatant")) ? RotationUtils.AimType.BLATANT : RotationUtils.AimType.REGULAR;
    }

    private float getDynamicAimSpeed(Vec3 aimPoint) {
        if (mc.player == null || aimPoint == null) return MIN_AIM_SPEED;
        if (getAimType() == RotationUtils.AimType.BLATANT) return MAX_AIM_SPEED;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float[] targetRot = RotationUtils.calculate(mc.player.getEyePosition(pt), aimPoint);
        float[] currentRot = getCurrentAimRotations();

        float yawDiff = Math.abs(Mth.wrapDegrees(targetRot[0] - currentRot[0]));
        float pitchDiff = Math.abs(targetRot[1] - currentRot[1]);
        float largestDiff = Math.max(yawDiff, pitchDiff);

        float seconds = getAimBudgetTicks() / 20.0F;
        float internalMultiplier = getRotationSpeedMultiplier();
        float requiredSpeed = largestDiff / Math.max(0.001F, seconds * internalMultiplier);
        return Mth.clamp(requiredSpeed * AIM_SPEED_BUFFER, MIN_AIM_SPEED, MAX_AIM_SPEED);
    }

    private int getAimBudgetTicks() {
        if (targetPlan == null) return 1;
        return Math.max(1, Math.min(4, targetPlan.travelTicks - 1));
    }

    private float getRotationSpeedMultiplier() {
        return silentAim.getValue() ? 2.5F : 0.5F;
    }

    private float[] getCurrentAimRotations() {
        if (silentAim.getValue() && RotationUtils.hasSilentRotation()) {
            return new float[]{RotationUtils.getSilentYaw(), RotationUtils.getSilentPitch()};
        }

        return new float[]{mc.player.getYRot(), mc.player.getXRot()};
    }

    private boolean hasReachedAimPoint(Vec3 aimPoint) {
        if (mc.player == null) return false;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float[] targetRot = RotationUtils.calculate(mc.player.getEyePosition(pt), aimPoint);
        float[] currentRot = getCurrentAimRotations();

        float yawDiff = Math.abs(Mth.wrapDegrees(targetRot[0] - currentRot[0]));
        float pitchDiff = Math.abs(targetRot[1] - currentRot[1]);
        float threshold = getAimType() == RotationUtils.AimType.BLATANT ? 2.0f : 3.0f;
        return yawDiff <= threshold && pitchDiff <= threshold;
    }

    private record InterceptPlan(Vec3 aimPoint, Vec3 aimDirection, double missDistance, int pearlTick, int travelTicks) {}
}
