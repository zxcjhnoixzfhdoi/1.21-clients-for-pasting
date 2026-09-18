package hack.echo.client.features.impl.combat;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.world.WorldUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

//TODO: genuiinely needs recoding :sob:
public class ShieldBreaker extends Feature {

    private static final CharSequence CLICK_MODE = Concat.of("Click");
    private static final CharSequence NORMAL_MODE = Concat.of("Normal");

	public ShieldBreaker() {
		super(new FeatureInfo(
			Concat.of("Shield Breaker"),
			Concat.of("Breaks enemy shields"),
			Category.COMBAT
		));

        inputSimulation.onChanged(this::syncSwapBackBounds);
        syncSwapBackBounds();

        reactionDelay.setDependency(p -> modeSetting.is(NORMAL_MODE));
        attackDelay.setDependency(p -> modeSetting.is(NORMAL_MODE));
        autoSwapToAxe.setDependency(p -> modeSetting.is(NORMAL_MODE));
        initialDelay.setDependency(s -> modeSetting.is(NORMAL_MODE) && autoSwapToAxe.getValue());
        unblockShield.setDependency(p -> modeSetting.is(NORMAL_MODE));

        returnBack.setDependency(p -> modeSetting.is(CLICK_MODE) || (modeSetting.is(NORMAL_MODE) && autoSwapToAxe.getValue()));
        swapBackDelay.setDependency(g -> returnBack.getValue()
            && (modeSetting.is(CLICK_MODE) || (modeSetting.is(NORMAL_MODE) && autoSwapToAxe.getValue())));
	}

    private final ModeSetting modeSetting = new ModeSetting(
        Concat.of("Mode"),
        CLICK_MODE,
        CLICK_MODE,
        NORMAL_MODE
    );

    private final IntSetting reactionDelay = new IntSetting(
        Concat.of("Reaction Delay"), 0, 0, 500);
    private final IntSetting attackDelay = new IntSetting(
        Concat.of("Attack Delay"), 0, 0, 500);

    private final BoolSetting autoSwapToAxe = new BoolSetting(
        Concat.of("Auto Swap"), true);
    private final IntSetting initialDelay = new IntSetting(
        Concat.of("Delay"), 1, 0, 20);

    private final BoolSetting returnBack = new BoolSetting(
        Concat.of("Swap Back"), false);
    private final IntSetting swapBackDelay = new IntSetting(
        Concat.of("Delay"), 1, 0, 20);

    private final TimerUtils reactionTimer = new TimerUtils();
    private final TimerUtils firstSwapTimer = new TimerUtils();
    private final TimerUtils attackTimer = new TimerUtils();

    private final BoolSetting unblockShield = new BoolSetting(Concat.of("Unblock Own Shield"), true);
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Simulate Input"), false);

    private boolean hasPlayerUnblocked = false;
    private boolean awaitingSwapBack = false;

    private Player pendingTarget = null;

    @Override
    public void onDisable() {
        SwapStateManager.cancel(this);
        super.onDisable();
        hasPlayerUnblocked = false;
        awaitingSwapBack = false;
        pendingTarget = null;
        attackTimer.reset();
        reactionTimer.reset();
        firstSwapTimer.reset();
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onMousePress(EventStartAttack.Pre event) {
        if (!modeSetting.is(CLICK_MODE)) return;
        if (mc.player == null || mc.level == null || hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (InventoryUtils.isHoldingAxeItem()) return;
        if (SwapStateManager.hasActiveSwaps() && !SwapStateManager.isOwnerActive(this)) return;
        syncSwapBackBounds();

        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player target) {
            if (!TargetUtils.isTargetAllowed(target)) return;
            boolean canBreakShield = !WorldUtils.isShieldFacingAway(target);
            boolean isBlocking = target.isBlocking() && target.isUsingItem() && target.getUseItem().getItem() instanceof ShieldItem;
            ItemStack mainHand = InventoryUtils.getMainHandItem();

            MaceSwap maceSwap = Echo.featureManager.getFeatureByClass(MaceSwap.class);
            boolean maceSwapAllowsAxes = maceSwap != null && maceSwap.isEnabled() && MaceSwap.allowsAxes();

            if (isBlocking && canBreakShield && (!(mainHand.getItem() instanceof AxeItem) || maceSwapAllowsAxes)) {
                int itemSlot = InventoryUtils.findItemWithPredicateInHotbar(
                    ItemStack -> ItemStack.getItem() instanceof AxeItem
                );
                if (itemSlot != -1 && !SwapStateManager.isOwnerActive(this)) {
                    if (!SwapStateManager.swapTo(this, itemSlot, inputSimulation.getValue(), swapBackDelay.getValue(), returnBack.getValue())) {
                        return;
                    }
                    
                    /*
                     Schedule shield break prediction for MaceSwap.
                     Changing a field causes a natural tick delay.
                     At least for the next event. I could like maybe explain it more but basically it takes
                     until the next tick for the events to recognize a field has changed values.
                     However if you want something to happen on the same event. Just make a fake event.
                    */
                    pendingTarget = target;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    private void onTick(EventHandleInput.Early event) {
        if (mc.player == null || mc.level == null || hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        if (awaitingSwapBack && !SwapStateManager.hasActiveSwaps() && !(mc.player.getMainHandItem().getItem() instanceof AxeItem)) {
            clearSwapBackState();
        }

        if (modeSetting.is(NORMAL_MODE)) {
            handleNormalMode();
        }

        if (pendingTarget != null) {
            TargetControlModule.pendingShieldBreakTarget = pendingTarget;
            pendingTarget = null;
        }
    }

    private void handleNormalMode() {
        if (SwapStateManager.hasActiveSwaps() && !SwapStateManager.isOwnerActive(this)) return;
        syncSwapBackBounds();
        boolean isPlayerOffhandShielding = PlayerUtils.isPlayerShieldingInOffHand();
        boolean holdingAxe = mc.player.getMainHandItem().getItem() instanceof AxeItem;

        if (awaitingSwapBack) {
            if (!SwapStateManager.hasActiveSwaps() && !holdingAxe) {
                clearSwapBackState();
                return;
            }

            if (!holdingAxe) {
                return;
            }
        }

        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player target) {
            boolean canBreakShield = !WorldUtils.isShieldFacingAway(target);
            boolean isBlocking = target.isBlocking() && target.isUsingItem() && target.getUseItem().getItem() instanceof ShieldItem;
            ItemStack mainHand = InventoryUtils.getMainHandItem();

            if (isBlocking && canBreakShield){
                if (isPlayerOffhandShielding){
                    if (unblockShield.getValue()){
                        mc.options.keyUse.setDown(false);
                        hasPlayerUnblocked = true;
                    }
                }
                if (mainHand.getItem() instanceof AxeItem){
                    if (attackTimer.hasReached(attackDelay.getValue())){
                        InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());
                        
                        pendingTarget = target;

                        // Causing duplicate inputs with the other attack
                        attackTimer.reset();
                    }
                } else {
                    if (!autoSwapToAxe.getValue()) return;
                    if (reactionTimer.hasReached(reactionDelay.getValue())){
                        if (firstSwapTimer.hasReachedTicks(initialDelay.getValue())){
                            int itemSlot = InventoryUtils.findItemWithPredicateInHotbar
                                    (ItemStack -> ItemStack.getItem() instanceof AxeItem);
                            if (itemSlot != -1 && !SwapStateManager.isOwnerActive(this)){
                                InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());
                                firstSwapTimer.reset();
                                attackTimer.reset();
                                if (!SwapStateManager.swapTo(this, itemSlot, inputSimulation.getValue(), swapBackDelay.getValue(), returnBack.getValue(), this::onSwapEnded)) {
                                    return;
                                }

                                awaitingSwapBack = returnBack.getValue();
                            }
                        }
                    }
                }
            } else {
                reactionTimer.reset();
        
            }
        } else {
            firstSwapTimer.reset();
            reactionTimer.reset();
            attackTimer.reset();
    
        }
    }

    public void reShield(){
        if (mc.player == null || mc.level == null) return;
        if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS){
            if (mc.player.getOffhandItem().getItem() instanceof ShieldItem){
                mc.options.keyUse.setDown(true);
                hasPlayerUnblocked = false;
            }
        } else {
            hasPlayerUnblocked = false;
        }
    }

    private void onSwapEnded() {
        if (!returnBack.getValue()) {
            hasPlayerUnblocked = false;
            awaitingSwapBack = false;
            return;
        }

        if (modeSetting.is(NORMAL_MODE) && hasPlayerUnblocked) {
            reShield();
        }

        awaitingSwapBack = false;
        clearSwapBackState();
    }

    private void syncSwapBackBounds() {
        int minBound = inputSimulation.getValue() ? 1 : 0;

        if (swapBackDelay.getMinValue() < minBound) {
            swapBackDelay.setMinValue(minBound);
        }

        if (swapBackDelay.getMaxValue() < minBound) {
            swapBackDelay.setMaxValue(minBound);
        }

        if (swapBackDelay.getValue() < minBound) {
            swapBackDelay.setValue(minBound);
        }
    }

    private void clearSwapBackState() {
        awaitingSwapBack = false;
        reactionTimer.reset();
        firstSwapTimer.reset();
        attackTimer.reset();
    }
}
