package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

public class AutoTool extends Feature {

	public AutoTool() {
		super(new FeatureInfo(
			Concat.of("Auto Tool"),
			Concat.of("Automatically selects best tool"),
			Category.UTILITY
		));
	}

    private final BoolSetting switchBack = new BoolSetting(Concat.of("Switch back"), true);
    private final BoolSetting requireClick = new BoolSetting(Concat.of("Require click"), true);
    private final BoolSetting requireShift = new BoolSetting(Concat.of("Require shift"), true);

    private int previousSlot = -1;

    @EventSubscribe
    public void onTick(EventHandleInput.Early event) {
        if (isNull()) return;

        if (mc.hitResult instanceof BlockHitResult blockHitResult
                && !mc.level.getBlockState(blockHitResult.getBlockPos()).isAir()
                && (!requireClick.getValue() || mc.options.keyAttack.isDown())
                && (!requireShift.getValue() || mc.options.keyShift.isDown())) {

            int bestSlot = getBestTool(blockHitResult.getBlockPos());

            if (bestSlot != -1) {
                if (previousSlot == -1) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                }
                mc.player.getInventory().setSelectedSlot(bestSlot);
            } else if (switchBack.getValue() && previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        } else if (switchBack.getValue() && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }


    private int getBestTool(BlockPos pos) {
        int bestSlot = -1;
        float bestSpeed = 1f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (stack.isEmpty()) continue;

            float speed = getMiningSpeed(stack, pos);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private float getMiningSpeed(ItemStack stack, BlockPos pos) {
        if (stack.isEmpty()) return 0f;

        float speed = stack.getDestroySpeed(mc.level.getBlockState(pos));

        if (speed > 1f) {
            // Formula: level^2 + 1.0 (from efficiency.json: levels_squared with added: 1.0)
            /*
             *         "amount": {
             *           "type": "minecraft:levels_squared",
             *           "added": 1.0
             *         },
             *         which means the efficiency level is squared and 1.0 is added to the speed
             */
            var efficiencyEntry = mc.level.registryAccess().get(Enchantments.EFFICIENCY);
            
            if (efficiencyEntry.isPresent()) {
                int effLvl = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEntry.get(), stack);
                if (effLvl > 0) {
                    speed += effLvl * effLvl + 1;
                }
            }
        }

        return speed;
    }
}
