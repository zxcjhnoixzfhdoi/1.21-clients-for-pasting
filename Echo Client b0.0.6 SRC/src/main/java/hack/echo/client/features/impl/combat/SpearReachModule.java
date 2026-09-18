package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventSwingHand;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.combat.CombatUtils;
import hack.echo.client.utils.combat.SpearUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;

public class SpearReachModule extends Feature {

	public SpearReachModule() {
		super(new FeatureInfo(
			Concat.of("Spear Reach"),
			Concat.of("Swaps to spears for increased reach"),
			Category.COMBAT
		));
	}

    private final BoolSetting paperSupport = new BoolSetting(Concat.of("Paper support"), true);
    private final ItemPickerSetting itemWhitelist = new ItemPickerSetting(Concat.of("Item Whitelist"));

    private static final int switchDelay = 1;
    private final TimerUtils timer = new TimerUtils();
    private boolean shouldSwitchBack = false;

    private int getSpearSlotWithoutLunge() {
        if (mc.player == null || mc.level == null) return -1;
        
        var inventory = mc.player.getInventory();
        var opt = mc.level.registryAccess().get(Enchantments.LUNGE);
        
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && !stack.isEmpty() && InventoryUtils.isItemSpear(stack.getItem())) {
                if (opt.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(opt.get(), stack) == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }

    private boolean isHoldingSpearWithoutLunge() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.isEmpty() || !InventoryUtils.isItemSpear(mainHand.getItem())) {
            return false;
        }
        if (mc.level == null) return false;
        var opt = mc.level.registryAccess().get(Enchantments.LUNGE);
        return opt.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(opt.get(), mainHand) == 0;
    }

    @EventSubscribe
    public final void onDoAttackPre(EventStartAttack event) {
        if (getSwapReadyCrosshairTarget() == null) return;

        int spearSlot = getSpearSlotWithoutLunge();
        if (spearSlot == -1) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (currentSlot == spearSlot) return;

        if (!SwapStateManager.swapTo(this, spearSlot, false, switchDelay, true, () -> shouldSwitchBack = false)) {
            return;
        }
        timer.reset();
        shouldSwitchBack = true;
    }

    @EventSubscribe
    public void onSwingHand(EventSwingHand event) {
        if (paperSupport.getValue() && event.getPlayer() == mc.player) {
            SpearUtils.onPlayerSwing(mc.player);
        }
    }

    @EventSubscribe
    public void onHandleInputPost(EventTick event) {
        if (isNull()) return;

        if (shouldSwitchBack && timer.hasReachedTicks(switchDelay)) {
            SwapStateManager.cancel(this);
            shouldSwitchBack = false;
        }
    }

    private boolean isHoldingAllowed() {
		if (mc.player == null) return false;
		if (itemWhitelist.getSelectedCount() == 0) return true;
		
		ItemStack mainHand = mc.player.getMainHandItem();
		if (mainHand.isEmpty()) {
			return itemWhitelist.isSelected(Items.AIR);
		}
		
		return itemWhitelist.isSelected(mainHand.getItem());
	}

    public ItemStack getPaperCooldownSpearStack() {
        if (!isEnabled() || !paperSupport.getValue() || isNull()) {
            return ItemStack.EMPTY;
        }

        int spearSlot = getSpearSlotWithoutLunge();
        return spearSlot == -1 ? ItemStack.EMPTY : mc.player.getInventory().getItem(spearSlot);
    }

    @Override
    public void onDisable() {
        SwapStateManager.cancel(this);
        shouldSwitchBack = false;
    }

    public LivingEntity getSwapReadyCrosshairTarget() {
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return getSwapReadyCrosshairTarget(partialTick);
    }

    public LivingEntity getSwapReadyCrosshairTarget(float partialTick) {
        if (!isEnabled()) return null;
        if (isNull()) return null;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return null;
        if (mc.isPaused()) return null;
        if (isHoldingSpearWithoutLunge()) return null;
        if (!isHoldingAllowed()) return null;
        if (CombatUtils.hasCurrentItemCrosshairTarget(mc.player, partialTick)) return null;

        LivingEntity target = CombatUtils.getSpearCrosshairTarget(mc.player, partialTick);
        if (target == null) return null;

        int spearSlot = getSpearSlotWithoutLunge();
        if (spearSlot == -1) return null;

        if (paperSupport.getValue()) {
            ItemStack spearStack = mc.player.getInventory().getItem(spearSlot);
            if (!SpearUtils.isSpearSwapFullyCooled(mc.player, spearStack, partialTick)) return null;
        } else if (mc.player.getAttackStrengthScale(partialTick) < 1.0F) {
            return null;
        }

        return target;
    }

    public static boolean isSwapActive() {
        return SwapStateManager.isOwnerActive(SpearReachModule.class);
    }

}
