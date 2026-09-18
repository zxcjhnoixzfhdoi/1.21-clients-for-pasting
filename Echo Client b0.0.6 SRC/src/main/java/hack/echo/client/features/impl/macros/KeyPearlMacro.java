package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.HitResultUtils;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.HitResult;

import com.mojang.blaze3d.platform.InputConstants;
import hack.echo.client.Echo;

import hack.echo.client.utils.strings.Concat;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventHotbarChange;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.rotation.RotationUtils;

public class KeyPearlMacro extends Feature {

    public KeyPearlMacro() {
        super(new FeatureInfo(
                Concat.of("Key Pearl Macro"),
                Concat.of("Quick pearl throw"),
                Category.MACROS));
    }

    private final BoolSetting swapBack = new BoolSetting(Concat.of("Swap Back"),
            true);
    private final IntSetting pearlDelay = new IntSetting(
            Concat.of("Swap Delay"), 1, 0, 10,
            Concat.of(" ticks"), o -> swapBack.getValue());
    private final IntSetting throwDelay = new IntSetting(
            Concat.of("Throw Delay"), 0, 0, 10,
            Concat.of(" ticks"));
    private final KeybindSetting activateKey = new KeybindSetting(
            Concat.of("Activate Key"), -1);
    private final BoolSetting clickSimulation = new BoolSetting(Concat.of("Click Simulation"), false);
    {
        clickSimulation.onChanged(() -> {
            if (clickSimulation.getValue()) {
                pearlDelay.setMinValue(1);
                if (pearlDelay.getValue() < 1) {
                    pearlDelay.setValue(1);
                }
            } else {
                pearlDelay.setMinValue(0);
            }
            if (Echo.screenManager.isClickGUIOpen()) {
                Echo.screenManager.displayClickGUI();
            }
        });
    }
    private final BoolSetting syncToClient = new BoolSetting(Concat.of("Sync to Client"), true);


    int pearlSlot = -1;
    int lastSlot = -1;
    int preSequenceSlot = -1;
    private final TimerUtils timer = new TimerUtils();
    private boolean swapped = false;
    private boolean thrown = false;
    private boolean prevKeyDown = false;
    private HitResult savedHitResult = null;
    private boolean restoreHitResultNextPost = false;

    @EventSubscribe
    public void onHotbarChange(EventHotbarChange event) {
        if (isNull())
            return;

        if (!swapped) {
            preSequenceSlot = event.getPreviousSlot();
        }
    }

    @EventSubscribe
    public void onHandleInputPost(EventHandleInput.Post event) {
        if (isNull() || !restoreHitResultNextPost) return;
        
        mc.hitResult = savedHitResult;
        savedHitResult = null;
        restoreHitResultNextPost = false;
    }


    @EventSubscribe
    public void onTick(EventHandleInput.Early event) {
        if (isNull())
            return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null)
            return;

        boolean isKeyDown = InputHandler.isBindDown(activateKey.getKey());
        if (isKeyDown && !prevKeyDown) {
            lastSlot = mc.player.getInventory().getSelectedSlot();
            pearlSlot = InventoryUtils
                    .findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() instanceof EnderpearlItem);
            swapped = false;
            thrown = false;
            timer.reset();
            if (pearlSlot != -1) {
                // Suppress vanilla hotbar selection if activation key matches a hotbar slot key
                int activateKeyCode = activateKey.getKey();
                // Check if it's a keyboard key (not a mouse button)
                if ((activateKeyCode & 0x80000000) == 0 && mc.options != null) {
                    // Loop through hotbar slot keys and clear clickCount if they match
                    for (int i = 0; i < 9; i++) {
                        KeyMapping hotbarKey = mc.options.keyHotbarSlots[i];
                        KeyMappingAccessor accessor = (KeyMappingAccessor) hotbarKey;
                        InputConstants.Key key = accessor.getKey();
                        if (key != null && key.getValue() == activateKeyCode) {
                            // This is what tells the game the key was never pressed
                            // Which will prevent manual swapping
                            accessor.setClickCount(0);
                            break;
                        }
                    }
                }
                if (!mc.player.getCooldowns().isOnCooldown(createEnderPearlStack())) {
                    swapped = true;
                    if (swapBack.getValue()) {
                        if (!SwapStateManager.swapTo(this, pearlSlot, false, -1, true, null)) {
                            swapped = false;
                            return;
                        }
                    } else {
                        InventoryUtils.setInvSlot(pearlSlot);
                    }
                }
            }
        }

        if (swapped && !thrown && timer.hasReachedTicks(throwDelay.getValue())) {
            Runnable useAction = () -> {
                if (clickSimulation.getValue()) {
                    HitResult original = HitResultUtils.spoofEntityToMiss();
                    savedHitResult = original;
                    restoreHitResultNextPost = true;
                    InputHandler.simulateClick(mc.options.keyUse, true);
                } else {
                    HitResultUtils.withMissHitResult(() -> {
                        ((MinecraftAccessor) mc).invokeStartUseItem();
                    });
                }
            };

            if (syncToClient.getValue()) {
                RotationUtils.syncToClientAndRun(
                    this,
                    EventSubscribe.Priority.HIGHEST,
                    RotationUtils.AimType.REGULAR,
                    650f,
                    650f,
                    useAction
                );
            } else {
                useAction.run();
            }
            
            thrown = true;
            timer.reset();
        }

        if (thrown && swapBack.getValue() && timer.hasReachedTicks(pearlDelay.getValue())) {
            SwapStateManager.cancel(this);
            swapped = false;
            thrown = false;
        }
        prevKeyDown = isKeyDown;
    }

    private ItemStack createEnderPearlStack() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    @Override
    public void onDisable() {
        SwapStateManager.cancel(this);
        super.onDisable();
    }

}
