package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import static net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK;

public class TotemHitModule extends Feature{
    public TotemHitModule() {
        super(new FeatureInfo(
            Concat.of("Totem Hit"),
            Concat.of("Swaps to knockback sword when attacking with totem"),
            Category.COMBAT
        ));
    }
    private final BoolSetting silentSwap = new BoolSetting(Concat.of("Silent Swap"), false);
    private final RangeSetting swapBackDelay = new RangeSetting
    (Concat.of("Delay"), 1, 1, 1, 20, 1, (obj -> !silentSwap.getValue()));
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Input Simulation"), true, (obj -> !silentSwap.getValue())).describedBy(Concat.of("Whether to send hotbar key press simulations"));

    boolean attacked = false;

    private int findHotbarSlotWithKnockbackSword() {
        return InventoryUtils.findItemWithEnchantmentInHotbar(
            stack -> InventoryUtils.isSword(stack.getItem()),
            KNOCKBACK,
            false
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attacked = false;
        SwapStateManager.cancel(this);
    }

    @EventSubscribe
    private void onAttack(EventStartAttack.Pre event) {
        if (isNull()) return;
        if (mc.isPaused()) return;
        if (mc.player == null) return;
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        if (!isEntityAllowed(entity)) return;
        if (mc.player.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING) return;
        int knockbackSwordSlot = findHotbarSlotWithKnockbackSword();
        if (knockbackSwordSlot == -1) return;
        if (!SwapStateManager.swapTo(
                this,
                knockbackSwordSlot,
                inputSimulation.getValue() && !silentSwap.getValue(),
                silentSwap.getValue() ? 0 : (int) swapBackDelay.getRandom(),
                !silentSwap.getValue(),
                () -> attacked = false)) {
            return;
        }
        attacked = true;
    }

    private boolean isEntityAllowed(LivingEntity entity) {
		if (!TargetUtils.isTargetAllowed(entity)) {
            return false;
        }
        return true;
	}
}
