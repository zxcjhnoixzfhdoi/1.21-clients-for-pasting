package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.mixin.accessors.AbstractContainerScreenAccessor;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import hack.echo.client.api.MobEffectCompat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.lwjgl.glfw.GLFW;

public class PotRefill extends Feature {

	public PotRefill() {
		super(new FeatureInfo(
			Concat.of("Pot Refill"),
			Concat.of("Automatically refills potions"),
			Category.UTILITY
		));
	}

    private static final CharSequence HOVER = Concat.of("Hover");
    private static final CharSequence SHIFT_LMB = Concat.of("ShiftLMB");
    private static final CharSequence AUTO = Concat.of("Auto");

    private static final ModeSetting mode = new ModeSetting(
        Concat.of("Mode"), HOVER, HOVER, SHIFT_LMB, AUTO
    );
    private static final BoolSetting refillBuffPotions = new BoolSetting(
        Concat.of("Refill Buff Potions"), true
    );
    private static final RangeSetting delay = new RangeSetting(
        Concat.of("Delay"), 10, 50, 0, 400, 5, Concat.of("ms")
    );

    private final TimerUtils refillTimer = new TimerUtils();

    @EventSubscribe
    private void onTick(EventTick event) {
        if (isNull()) return;

        if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof InventoryScreen inventoryScreen) {
                if (mode.is(HOVER)) {
                    handleHoverRefill(inventoryScreen);
                } else if (mode.is(SHIFT_LMB)) {
                    handleShiftClickRefill(inventoryScreen);
                } else if (mode.is(AUTO)) {
                handleAutoRefill(inventoryScreen);
            }
        }


    }
    private void handleHoverRefill(InventoryScreen inventoryScreen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) inventoryScreen;
        Slot focusedSlot = accessor.getHoveredSlot();

        if (focusedSlot == null) return;

        ItemStack stack = focusedSlot.getItem();
        if (shouldRefillPotion(stack) && !isInHotbar(focusedSlot.getContainerSlot())) {
            int emptySlot = findEmptyHotbarSlot();
            if (emptySlot != -1 && refillTimer.hasReached(delay.getRandom())) {
                refillTimer.reset();
                InventoryClickCompat.handleClick(
                    inventoryScreen.getMenu().containerId,
                    focusedSlot.getContainerSlot(),
                    emptySlot,
                    InventoryClickType.SWAP,
                    mc.player
                );
            }
        }
    }

    private void handleShiftClickRefill(InventoryScreen inventoryScreen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) inventoryScreen;
        Slot focusedSlot = accessor.getHoveredSlot();

        if (focusedSlot == null) return;

        if (GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS && GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {

            ItemStack stack = focusedSlot.getItem();
            if (shouldRefillPotion(stack) && !isInHotbar(focusedSlot.getContainerSlot())) {
                int emptySlot = findEmptyHotbarSlot();
                if (emptySlot != -1 && refillTimer.hasReached(delay.getRandom())) {
                    refillTimer.reset();
                    InventoryClickCompat.handleClick(
                        inventoryScreen.getMenu().containerId,
                        focusedSlot.getContainerSlot(),
                        emptySlot,
                        InventoryClickType.SWAP,
                        mc.player
                    );
                }
            }
        }
    }

    private void handleAutoRefill(InventoryScreen inventoryScreen) {
        if (!refillTimer.hasReached(delay.getRandom())) return;
        refillTimer.reset();

        for (int invSlot = 9; invSlot < 45; invSlot++) { // Inventory slots 9-44
            ItemStack stack = inventoryScreen.getMenu().getSlot(invSlot).getItem();
            if (shouldRefillPotion(stack)) {
                int emptySlot = findEmptyHotbarSlot();
                if (emptySlot != -1) {
                    InventoryClickCompat.handleClick(
                        inventoryScreen.getMenu().containerId,
                        invSlot,
                        emptySlot,
                        InventoryClickType.SWAP,
                        mc.player
                    );
                    break; // Only move one at a time
                }
            }
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION;
    }

    private boolean shouldRefillPotion(ItemStack stack) {
        if (!isSplashPotion(stack) || mc.player == null) return false;
        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) return false;

        for (var effect : potionContents.getAllEffects()) {
            var effectHolder = effect.getEffect();
            var mobEffect = effectHolder.value();

            if (isInstantEffect(mobEffect) && mobEffect.isBeneficial()) {
                return true;
            }

            if (!refillBuffPotions.getValue()) {
                continue;
            }

            if (mobEffect.isBeneficial()
                && !isInstantEffect(mobEffect)
                && !mc.player.hasEffect(effectHolder)
                && !hasBuffPotionInHotbar(effectHolder)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBuffPotionInHotbar(Holder<MobEffect> targetEffect) {
        for (int i = 0; i < 9; i++) {
            if (hasMatchingBuffEffect(mc.player.getInventory().getItem(i), targetEffect)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingBuffEffect(ItemStack stack, Holder<MobEffect> targetEffect) {
        if (!isSplashPotion(stack)) return false;
        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) return false;

        for (var effect : potionContents.getAllEffects()) {
            var effectHolder = effect.getEffect();
            var mobEffect = effectHolder.value();
            if (mobEffect.isBeneficial() && !isInstantEffect(mobEffect) && effectHolder.equals(targetEffect)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInHotbar(int slotIndex) {
        return slotIndex >= 0 && slotIndex <= 8;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInstantEffect(MobEffect effect) {
        return MobEffectCompat.isInstantaneous(effect);
    }
}
