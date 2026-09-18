package hack.echo.client.features.impl.player;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import hack.echo.client.api.MinecraftCompat;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventItemUse;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.mixin.accessors.AbstractContainerScreenAccessor;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.inventory.ContainerUtils;
import hack.echo.client.utils.inventory.CursorUtil;
import hack.echo.client.utils.inventory.CursorUtil.ScreenInfo;
import hack.echo.client.utils.inventory.CursorUtil.SlotResult;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoTotem extends Feature {
    public static boolean blocked = false;

    private enum SilentStage {
        IDLE,
        WAIT_BEFORE_OPEN,
        WAIT_AFTER_OPEN,
        WAIT_BEFORE_CLOSE
    }

    private static final CharSequence OPEN_INVENTORY = Concat.of("Open Inventory");
    private static final CharSequence SILENT_OPEN = Concat.of("Silent Open");
    private static final CharSequence DONT_OPEN = Concat.of("Dont Open");

    private static final CharSequence AUTOMATIC = Concat.of("Automatic");
    private static final CharSequence HOVER = Concat.of("Hover");
    private static final CharSequence CURSOR_SIMULATION = Concat.of("Cursor Simulation");

    private static final float CURSOR_MIN_TIME_MS = 50.0f;
    private static final float CURSOR_MAX_TIME_MS = 250.0f;

    private final ModeSetting mode = new ModeSetting(
        Concat.of("Mode"),
        OPEN_INVENTORY,
        OPEN_INVENTORY,
        SILENT_OPEN,
        DONT_OPEN
    );

    private final ModeSetting selectionMode = new ModeSetting(
        Concat.of("Selection"),
        AUTOMATIC,
        AUTOMATIC,
        HOVER,
        CURSOR_SIMULATION
    );

    private final BoolSetting autoClose = new BoolSetting(Concat.of("Auto Close"), true);
    private final RangeSetting closeDelay = new RangeSetting(
        Concat.of("Close Delay"),
        50,
        150,
        0,
        500,
        25,
        Concat.of(" ms")
    );

    private final RangeSetting silentMoveDelay = new RangeSetting(
        Concat.of("Silent Move Delay"),
        100,
        300,
        0,
        500,
        25,
        Concat.of(" ms")
    );

    private final BoolSetting randomSlot = new BoolSetting(Concat.of("Random Slot"), false);

    private final RangeSetting delay = new RangeSetting(
        Concat.of("Delay"),
        50,
        150,
        0,
        500,
        25,
        Concat.of(" ms")
    );

    private final BoolSetting refillHotbar = new BoolSetting(Concat.of("Refill Hotbar"), false);
    private final IntSetting refillSlot = new IntSetting(Concat.of("Slot to Refill Totem"), 9, 1, 9);

    private final TimerUtils actionTimer = new TimerUtils();
    private final TimerUtils silentMoveTimer = new TimerUtils();
    private final TimerUtils silentStageTickTimer = new TimerUtils();
    private final TimerUtils closeTimer = new TimerUtils();
    private final Random random = new Random();

    private long nextActionDelayMs;
    private long activeSilentMoveDelayMs;
    private long nextCloseDelayMs;
    private boolean waitingToCloseInventory;
    private boolean openedInventoryForHoverCycle;
    private boolean waitingForOpenInventoryDelay;
    private SilentStage silentStage = SilentStage.IDLE;
    private int pendingSilentSourceSlot = -1;
    private int pendingSilentButton = -1;

    private boolean cursorAnimating;
    private boolean waitingForCursorSwap;
    private long cursorAnimStartMs;
    private long cursorAnimDurationMs;
    private double cursorStartX;
    private double cursorStartY;
    private double cursorEndX;
    private double cursorEndY;
    private int pendingCursorSourceSlot = -1;
    private int pendingCursorButton = -1;

    public AutoTotem() {
        super(new FeatureInfo(
            Concat.of("Auto Totem"),
            Concat.of("Automatically equips a totem in your offhand"),
            Category.UTILITY
        ));

        silentMoveDelay.setDependency(setting -> mode.is(SILENT_OPEN));
        selectionMode.setDependency(setting -> usesSelectionMode());
        randomSlot.setDependency(setting -> mode.is(SILENT_OPEN) || isAutomaticSubMode());
        refillSlot.setDependency(setting -> refillHotbar.getValue());
        closeDelay.setDependency(setting -> autoClose.getValue());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        waitingToCloseInventory = false;
        activeSilentMoveDelayMs = 0L;
        actionTimer.reset();
        silentMoveTimer.reset();
        silentStageTickTimer.reset();
        closeTimer.reset();
        nextActionDelayMs = randomDelayMs();
        nextCloseDelayMs = randomCloseDelayMs();
        openedInventoryForHoverCycle = false;
        waitingForOpenInventoryDelay = false;
        resetSilentState();
        resetCursorSimulationState();
    }

    @Override
    public void onDisable() {
        waitingToCloseInventory = false;
        activeSilentMoveDelayMs = 0L;
        openedInventoryForHoverCycle = false;
        waitingForOpenInventoryDelay = false;
        resetSilentState();
        resetCursorSimulationState();
        super.onDisable();
    }

    @EventSubscribe
    private void onTick(EventTick event) {
        if (blocked) return;
        if (isNull() || mc.gameMode == null) return;
        if (isBlockedByScreen()) {
            if (isCursorSimulationActive()) {
                resetCursorSimulationState();
            }
            return;
        }

        if (!mode.is(SILENT_OPEN) && isSilentActionActive()) {
            resetSilentState();
        }

        if (!isCursorSimulationSubMode() && isCursorSimulationActive()) {
            resetCursorSimulationState();
        }

        if (mode.is(SILENT_OPEN) && isSilentActionActive()) {
            processSilentSequence();
            return;
        }

        if (waitingForCursorSwap) {
            executeCursorSimulationSwap();
            return;
        }

        handlePendingClose();
        updateOpenInventoryDelayState();

        if (isCursorSimulationActive()) {
            return;
        }

        if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            handleOffhandRefill();
            return;
        }

        openedInventoryForHoverCycle = false;

        if (refillHotbar.getValue()) {
            handleHotbarRefill();
        }
    }

    @EventSubscribe
    private void onRender2D(EventRender2DGui event) {
        if (!cursorAnimating) return;
        processCursorSimulationRender();
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGH)
    private void onMovementInput(EventMovementInput event) {
        if (!isSilentActionActive()) return;

        event.forward = false;
        event.back = false;
        event.left = false;
        event.right = false;
        event.jump = false;
        event.sneak = false;
        event.sprint = false;
        event.forceMovement = true;
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGH)
    private void onItemUse(EventItemUse.Pre event) {
        if (!isSilentActionActive()) return;
        event.cancel();
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGH)
    private void onStartAttack(EventStartAttack event) {
        if (!isSilentActionActive()) return;
        event.cancel();
    }

    private void handleOffhandRefill() {
        if (mode.is(OPEN_INVENTORY) && !isInventoryScreenOpen()) {
            if (!hasTotemInInventory(-1)) return;
            if (!beginOpenInventoryDelay()) return;
            if (!isActionDelayReady()) return;
            if (isHoverSubMode() && openedInventoryForHoverCycle) return;
            prepareModeForAction();
            resetOpenInventoryDelay();
            if (isHoverSubMode()) {
                openedInventoryForHoverCycle = true;
            }
            return;
        }

        int sourceMenuSlot = findOffhandSourceSlot();
        if (sourceMenuSlot == -1) return;
        if (!isActionDelayReady()) return;

        if (mode.is(SILENT_OPEN)) {
            startSilentSequence(sourceMenuSlot, 40);
            return;
        }

        if (!prepareModeForAction()) return;

        if (isCursorSimulationSubMode()) {
            startCursorSimulation(sourceMenuSlot, 40);
            return;
        }

        clickSwap(sourceMenuSlot, 40);
    }

    private void handleHotbarRefill() {
        int targetHotbarIndex = refillSlot.getValue() - 1;
        if (mc.player.getInventory().getItem(targetHotbarIndex).is(Items.TOTEM_OF_UNDYING)) return;

        if (mode.is(OPEN_INVENTORY) && !isInventoryScreenOpen()) {
            if (!hasTotemInInventory(targetHotbarIndex)) return;
            if (!beginOpenInventoryDelay()) return;
            if (!isActionDelayReady()) return;
            prepareModeForAction();
            resetOpenInventoryDelay();
            return;
        }

        int sourceMenuSlot = findHotbarRefillSourceSlot(targetHotbarIndex);
        if (sourceMenuSlot == -1) return;
        if (!isActionDelayReady()) return;

        if (mode.is(SILENT_OPEN)) {
            startSilentSequence(sourceMenuSlot, targetHotbarIndex);
            return;
        }

        if (!prepareModeForAction()) return;

        if (isCursorSimulationSubMode()) {
            startCursorSimulation(sourceMenuSlot, targetHotbarIndex);
            return;
        }

        clickSwap(sourceMenuSlot, targetHotbarIndex);
    }

    private boolean isActionDelayReady() {
        return actionTimer.hasReached(nextActionDelayMs);
    }

    private boolean beginOpenInventoryDelay() {
        if (waitingForOpenInventoryDelay) {
            return true;
        }

        waitingForOpenInventoryDelay = true;
        actionTimer.reset();
        nextActionDelayMs = randomDelayMs();
        return false;
    }

    private void updateOpenInventoryDelayState() {
        if (!waitingForOpenInventoryDelay) return;
        if (shouldKeepOpenInventoryDelay()) return;
        resetOpenInventoryDelay();
    }

    private boolean shouldKeepOpenInventoryDelay() {
        if (!mode.is(OPEN_INVENTORY)) return false;
        if (isInventoryScreenOpen()) return false;

        if (!mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return hasTotemInInventory(-1);
        }

        if (!refillHotbar.getValue()) {
            return false;
        }

        int targetHotbarIndex = refillSlot.getValue() - 1;
        if (mc.player.getInventory().getItem(targetHotbarIndex).is(Items.TOTEM_OF_UNDYING)) {
            return false;
        }

        return hasTotemInInventory(targetHotbarIndex);
    }

    private void resetOpenInventoryDelay() {
        waitingForOpenInventoryDelay = false;
    }

    private boolean prepareModeForAction() {
        if (mode.is(OPEN_INVENTORY)) {
            if (isInventoryScreenOpen()) return true;

            MinecraftCompat.setScreen(new InventoryScreen(mc.player));
            return false;
        }

        if (mode.is(DONT_OPEN)) {
            return isInventoryScreenOpen();
        }

        return !mode.is(SILENT_OPEN);
    }

    private int findOffhandSourceSlot() {
        if (isHoverSubMode()) {
            return findHoveredTotemSlot();
        }

        if (isCursorSimulationSubMode()) {
            return findNearestCursorSimulationSlot(-1);
        }

        if (randomSlot.getValue()) {
            return findRandomTotemMenuSlot(-1);
        }

        return findFirstTotemMenuSlot(-1);
    }

    private int findHotbarRefillSourceSlot(int targetHotbarIndex) {
        if (isHoverSubMode()) {
            int hovered = findHoveredTotemSlot();
            if (hovered == -1) return -1;
            if (hovered == toMenuSlot(targetHotbarIndex)) return -1;
            return hovered;
        }

        if (isCursorSimulationSubMode()) {
            return findNearestCursorSimulationSlot(targetHotbarIndex);
        }

        if (randomSlot.getValue()) {
            return findRandomTotemMenuSlot(targetHotbarIndex);
        }

        return findFirstTotemMenuSlot(targetHotbarIndex);
    }

    private int findHoveredTotemSlot() {
        if (!(MinecraftCompat.getScreen() instanceof InventoryScreen || MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen)) {
            return -1;
        }

        if (!(MinecraftCompat.getScreen() instanceof AbstractContainerScreenAccessor accessor)) {
            return -1;
        }

        Slot hoveredSlot = accessor.getHoveredSlot();
        if (hoveredSlot == null) return -1;
        if (!hoveredSlot.getItem().is(Items.TOTEM_OF_UNDYING)) return -1;

        int menuSlot = mc.player.containerMenu.slots.indexOf(hoveredSlot);
        if (menuSlot == -1) return -1;
        if (menuSlot == 40) return -1;
        if (menuSlot >= 36 && menuSlot <= 44) return -1;

        return menuSlot;
    }

    private int findNearestCursorSimulationSlot(int excludedHotbarIndex) {
        SlotResult nearest = findNearestCursorSimulationTarget(excludedHotbarIndex);
        if (nearest == null) return -1;
        return nearest.slotIndex();
    }

    private SlotResult findNearestCursorSimulationTarget(int excludedHotbarIndex) {
        if (!isInventoryScreenOpen()) return null;

        ScreenInfo screenInfo = CursorUtil.getScreenInfo(MinecraftCompat.getScreen());
        if (screenInfo == null) return null;

        Window window = mc.getWindow();
        int[] origin = CursorUtil.getScreenOrigin(screenInfo, window);
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        boolean isCreative = MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen;
        return CursorUtil.findNearestSlotWithItem(
            screenInfo.handler().slots,
            Items.TOTEM_OF_UNDYING,
            isCreative,
            origin[0],
            origin[1],
            window,
            mouseX,
            mouseY,
            slot -> isCursorSimulationCandidate(slot, excludedHotbarIndex)
        );
    }

    private boolean isCursorSimulationCandidate(Slot slot, int excludedHotbarIndex) {
        if (!(slot.container instanceof Inventory)) return true;

        int containerSlot = slot.getContainerSlot();
        if (containerSlot == 40) return false;
        if (containerSlot == excludedHotbarIndex) return false;
        if (containerSlot >= 0 && containerSlot <= 8) return false;
        return true;
    }

    private int findFirstTotemMenuSlot(int excludedHotbarIndex) {
        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            if (inventorySlot == excludedHotbarIndex) continue;
            if (!mc.player.getInventory().getItem(inventorySlot).is(Items.TOTEM_OF_UNDYING)) continue;
            return toMenuSlot(inventorySlot);
        }

        return -1;
    }

    private int findRandomTotemMenuSlot(int excludedHotbarIndex) {
        List<Integer> candidates = new ArrayList<>();

        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            if (inventorySlot == excludedHotbarIndex) continue;
            if (!mc.player.getInventory().getItem(inventorySlot).is(Items.TOTEM_OF_UNDYING)) continue;
            candidates.add(toMenuSlot(inventorySlot));
        }

        if (candidates.isEmpty()) {
            return -1;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean hasTotemInInventory(int excludedHotbarIndex) {
        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            if (inventorySlot == excludedHotbarIndex) continue;
            if (mc.player.getInventory().getItem(inventorySlot).is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedByScreen() {
        if (MinecraftCompat.getScreen() == null) return false;
        if (MinecraftCompat.getScreen() instanceof InventoryScreen) return false;
        if (MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen) return false;
        return true;
    }

    private boolean isInventoryScreenOpen() {
        return MinecraftCompat.getScreen() instanceof InventoryScreen || MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen;
    }

    private void clickSwap(int sourceMenuSlot, int button) {
        clickSwap(sourceMenuSlot, button, true);
    }

    private void clickSwap(int sourceMenuSlot, int button, boolean scheduleClose) {
        InventoryClickCompat.handleClick(
            mc.player.containerMenu.containerId,
            sourceMenuSlot,
            button,
            InventoryClickType.SWAP,
            mc.player
        );

        actionTimer.reset();
        nextActionDelayMs = randomDelayMs();
        openedInventoryForHoverCycle = false;
        if (scheduleClose) {
            startCloseCountdownIfNeeded();
        }
    }

    private void startCursorSimulation(int sourceMenuSlot, int button) {
        if (isCursorSimulationActive()) return;
        if (!isInventoryScreenOpen()) return;

        ScreenInfo screenInfo = CursorUtil.getScreenInfo(MinecraftCompat.getScreen());
        if (screenInfo == null) return;
        if (sourceMenuSlot < 0 || sourceMenuSlot >= screenInfo.handler().slots.size()) return;

        Window window = mc.getWindow();
        int[] origin = CursorUtil.getScreenOrigin(screenInfo, window);
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        boolean isCreative = MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen;
        double maxDistance = CursorUtil.maxSlotDistance(
            screenInfo.handler().slots,
            isCreative,
            origin[0],
            origin[1],
            window,
            mouseX,
            mouseY,
            slot -> isCursorSimulationCandidate(slot, button)
        );

        Slot targetSlot = screenInfo.handler().slots.get(sourceMenuSlot);
        double[] targetPos = CursorUtil.slotToAbsPos(targetSlot, origin[0], origin[1], window);
        double distance = Math.hypot(targetPos[0] - mouseX, targetPos[1] - mouseY);

        cursorStartX = mouseX;
        cursorStartY = mouseY;
        cursorEndX = targetPos[0];
        cursorEndY = targetPos[1];
        cursorAnimDurationMs = CursorUtil.computeAnimDuration(distance, maxDistance, CURSOR_MIN_TIME_MS, CURSOR_MAX_TIME_MS);
        cursorAnimStartMs = System.currentTimeMillis();
        pendingCursorSourceSlot = sourceMenuSlot;
        pendingCursorButton = button;
        cursorAnimating = true;
        waitingForCursorSwap = false;

        mc.execute(() -> mc.mouseHandler.releaseMouse());
    }

    private void processCursorSimulationRender() {
        if (mc.player == null || mc.player.isDeadOrDying()) {
            resetCursorSimulationState();
            return;
        }

        if (!isCursorSimulationSubMode()) {
            resetCursorSimulationState();
            return;
        }

        if (!isInventoryScreenOpen()) {
            resetCursorSimulationState();
            return;
        }

        long elapsed = System.currentTimeMillis() - cursorAnimStartMs;
        double progress = Math.max(0.0, Math.min(1.0, (double) elapsed / cursorAnimDurationMs));
        double eased = MathUtil.smoothStepLerp(progress, 0.0, 1.0);

        double x = cursorStartX + (cursorEndX - cursorStartX) * eased;
        double y = cursorStartY + (cursorEndY - cursorStartY) * eased;
        InputConstants.grabOrReleaseMouse(mc.getWindow(), InputConstants.CURSOR_NORMAL, x, y);

        if (progress < 1.0) return;

        cursorAnimating = false;
        waitingForCursorSwap = true;
    }

    private void executeCursorSimulationSwap() {
        ScreenInfo screenInfo = CursorUtil.getScreenInfo(MinecraftCompat.getScreen());
        if (screenInfo == null || mc.gameMode == null) {
            resetCursorSimulationState();
            return;
        }

        if (pendingCursorSourceSlot < 0 || pendingCursorSourceSlot >= screenInfo.handler().slots.size()) {
            resetCursorSimulationState();
            return;
        }

        if (!needsPendingCursorSwap()) {
            resetCursorSimulationState();
            return;
        }

        Slot slot = screenInfo.handler().slots.get(pendingCursorSourceSlot);
        if (!slot.getItem().is(Items.TOTEM_OF_UNDYING)) {
            resetCursorSimulationState();
            return;
        }

        clickSwap(pendingCursorSourceSlot, pendingCursorButton);
        resetCursorSimulationState();
    }

    private boolean needsPendingCursorSwap() {
        if (pendingCursorButton == 40) {
            return !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        }

        if (pendingCursorButton < 0 || pendingCursorButton > 8) {
            return false;
        }

        return !mc.player.getInventory().getItem(pendingCursorButton).is(Items.TOTEM_OF_UNDYING);
    }

    private int toMenuSlot(int inventorySlot) {
        if (inventorySlot < 9) return inventorySlot + 36;
        return inventorySlot;
    }

    private long randomDelayMs() {
        return Math.max(0L, (long) delay.getRandom());
    }

    private long randomSilentDelayMs() {
        return Math.max(0L, (long) silentMoveDelay.getRandom());
    }

    private long randomCloseDelayMs() {
        return Math.max(0L, (long) closeDelay.getRandom());
    }

    private void startCloseCountdownIfNeeded() {
        if (!autoClose.getValue()) return;

        nextCloseDelayMs = randomCloseDelayMs();
        if (nextCloseDelayMs <= 0L) {
            closeInventoryNow();
            return;
        }

        closeTimer.reset();
        waitingToCloseInventory = true;
    }

    private void handlePendingClose() {
        if (!waitingToCloseInventory) return;
        if (!autoClose.getValue()) {
            waitingToCloseInventory = false;
            return;
        }

        if (mode.is(OPEN_INVENTORY) && !isInventoryScreenOpen()) {
            waitingToCloseInventory = false;
            return;
        }
        if (!closeTimer.hasReached(nextCloseDelayMs)) return;

        closeInventoryNow();
    }

    private void closeInventoryNow() {
        if (mc.player == null) {
            waitingToCloseInventory = false;
            return;
        }

        if (isInventoryScreenOpen()) {
            mc.player.closeContainer();
        } else {
            ContainerUtils.closeContainer(mc.player.containerMenu.containerId);
        }
        waitingToCloseInventory = false;
    }

    private void startSilentSequence(int sourceMenuSlot, int button) {
        if (isSilentActionActive()) return;

        pendingSilentSourceSlot = sourceMenuSlot;
        pendingSilentButton = button;
        silentStage = SilentStage.WAIT_BEFORE_OPEN;
        activeSilentMoveDelayMs = randomSilentDelayMs();
        silentMoveTimer.reset();
    }

    private void processSilentSequence() {
        if (silentStage == SilentStage.WAIT_BEFORE_OPEN) {
            if (!silentMoveTimer.hasReached(activeSilentMoveDelayMs)) return;

            mc.player.sendOpenInventory();
            silentStage = SilentStage.WAIT_AFTER_OPEN;
            silentStageTickTimer.reset();
            return;
        }

        if (silentStage == SilentStage.WAIT_AFTER_OPEN) {
            if (!silentStageTickTimer.hasReachedTicks(1)) return;

            clickSwap(pendingSilentSourceSlot, pendingSilentButton, false);

            if (!autoClose.getValue()) {
                resetSilentState();
                return;
            }

            silentStage = SilentStage.WAIT_BEFORE_CLOSE;
            activeSilentMoveDelayMs = randomSilentDelayMs();
            silentMoveTimer.reset();
            return;
        }

        if (silentStage == SilentStage.WAIT_BEFORE_CLOSE) {
            if (!silentMoveTimer.hasReached(activeSilentMoveDelayMs)) return;
            ContainerUtils.closeContainer(mc.player.containerMenu.containerId);
            resetSilentState();
        }
    }

    private boolean usesSelectionMode() {
        return mode.is(OPEN_INVENTORY) || mode.is(DONT_OPEN);
    }

    private boolean isAutomaticSubMode() {
        return usesSelectionMode() && selectionMode.is(AUTOMATIC);
    }

    private boolean isHoverSubMode() {
        return usesSelectionMode() && selectionMode.is(HOVER);
    }

    private boolean isCursorSimulationSubMode() {
        return usesSelectionMode() && selectionMode.is(CURSOR_SIMULATION);
    }

    private boolean isSilentActionActive() {
        return silentStage != SilentStage.IDLE;
    }

    private boolean isCursorSimulationActive() {
        return cursorAnimating || waitingForCursorSwap;
    }

    private void resetSilentState() {
        silentStage = SilentStage.IDLE;
        pendingSilentSourceSlot = -1;
        pendingSilentButton = -1;
        activeSilentMoveDelayMs = 0L;
    }

    private void resetCursorSimulationState() {
        cursorAnimating = false;
        waitingForCursorSwap = false;
        cursorAnimStartMs = 0L;
        cursorAnimDurationMs = 0L;
        pendingCursorSourceSlot = -1;
        pendingCursorButton = -1;
    }
}
