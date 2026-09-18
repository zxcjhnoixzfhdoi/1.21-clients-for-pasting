package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.inventory.InventoryUtils;
import net.minecraft.world.item.Items;
import hack.echo.client.utils.strings.Concat;

public class ElytraSwap extends Feature {

	public ElytraSwap() {
		super(new FeatureInfo(
			Concat.of("Elytra Swap"),
			Concat.of("Quick elytra swap"),
			Category.UTILITY
		));
	}
    private final BoolSetting switchBack = new BoolSetting(Concat.of("Switch Back"), true);
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Click Simulation"), false);
    private final BoolSetting silentSwap = new BoolSetting(Concat.of("Silent Swap"), false, obj -> !inputSimulation.getValue());
    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("Activate Key"), -1);

    private boolean startedWithElytra = false;
    private boolean swapped = false;
    private boolean keyWasDown = false;
    private boolean activated = false;
    private boolean shouldSwapBack = false;

    @EventSubscribe
    private void onTick(EventHandleInput.Early event) {
        if (mc.player == null || mc.level == null || mc.player.isCreative()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        // Activation check
        boolean keyDown = InputHandler.isBindDown(activateKey.getKey());
        if (keyDown && !keyWasDown && !activated) {
            startSequence();
        }
        keyWasDown = keyDown;

        if (!activated) return;

        boolean wearingElytra = InventoryUtils.isWearingElytra();
        var mainHand = mc.player.getMainHandItem().getItem();

        if (!swapped) {
            if ((InventoryUtils.isHoldingChestplate() && startedWithElytra) || (mainHand == Items.ELYTRA && !startedWithElytra)) {
                useItem();
                swapped = true;
                if (silentSwap.getValue()) {
                    shouldSwapBack = true;
                }
            } else {
                int slot = startedWithElytra ?
                        InventoryUtils.findChestplateSlot() :
                        InventoryUtils.findItemWithPredicateInHotbar(item -> item.getItem() == Items.ELYTRA);
                if (slot == -1) {
                    reset();
                    return;
                }
                if (!SwapStateManager.swapToIfNeeded(this, slot, inputSimulation.getValue(), -1, false)) {
                    reset();
                    return;
                }
                if (silentSwap.getValue()) {
                    useItem();
                    swapped = true;
                    shouldSwapBack = true;
                }
            }
        }

        if (swapped) {
            boolean shouldReset = startedWithElytra ? !wearingElytra : wearingElytra;
            
            if (shouldReset) {
                SwapStateManager.cancel(this, switchBack.getValue());
                reset();
            }
        }
    }

    private void useItem() {
        if (inputSimulation.getValue()) {
            InputHandler.simulateClick(mc.options.keyUse, true);
        } else {
            ((MinecraftAccessor) mc).invokeStartUseItem();
        }
    }

    @EventSubscribe
    private void onSilentSwap(EventHandleInput.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (!silentSwap.getValue() || !shouldSwapBack) return;
        
        SwapStateManager.cancel(this, switchBack.getValue());
        shouldSwapBack = false;
    }
    private void startSequence() {
        if (mc.player == null) return;
        swapped = false;
        startedWithElytra = InventoryUtils.isWearingElytra();
        activated = true;
    }

    private void reset() {
        SwapStateManager.cancel(this, false);
        swapped = false;
        activated = false;
        shouldSwapBack = false;
    }

    @Override
    public void onEnable() {
        activated = false;
        swapped = false;
        keyWasDown = false;
        shouldSwapBack = false;
        SwapStateManager.cancel(this, false);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }
}
