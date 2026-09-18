package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.mixin.accessors.MultiPlayerGameModeAccessor;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Lowk weird ass module
public class AttributeMineModule extends Feature {

    private BlockPos lastBlockPos = null;
    private int pendingToolSlot = -1;
    private int ticksRemaining = 0;

    public AttributeMineModule() {
        super(new FeatureInfo(
                Concat.of("Attribute Mine"),
                Concat.of("Technique to mine faster."),
                Category.UTILITY
        ));
    }

    @EventSubscribe
    private void onTick(EventHandleInput.Early event) {
        if (isNull()) return;

        if (pendingToolSlot != -1) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                mc.player.getInventory().setSelectedSlot(pendingToolSlot);
                ((MultiPlayerGameModeAccessor) mc.gameMode).syncSlot();
                pendingToolSlot = -1;
            }
            return;
        }

        if (!mc.options.keyAttack.isDown()) {
            lastBlockPos = null;
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            lastBlockPos = null;
            return;
        }

        BlockPos currentPos = blockHitResult.getBlockPos();
        BlockState state = mc.level.getBlockState(currentPos);

        if (state.isAir()) {
            lastBlockPos = null;
            return;
        }

        if (currentPos.equals(lastBlockPos)) return;
        lastBlockPos = currentPos;

        int efficiencySlot = getBestEfficiencySlot();
        int bestToolSlot = getBestToolSlot(state);

        if (efficiencySlot == -1) return;

        Inventory inventory = mc.player.getInventory();

        inventory.setSelectedSlot(efficiencySlot);
        ((MultiPlayerGameModeAccessor) mc.gameMode).syncSlot();

        if (bestToolSlot != -1 && bestToolSlot != efficiencySlot) {
            pendingToolSlot = bestToolSlot;
            ticksRemaining = 2;
        }
    }

    private int getBestEfficiencySlot() {
        Inventory inventory = mc.player.getInventory();
        int bestSlot = -1;
        int bestLevel = 0;

        var efficiencyHolder = mc.level.registryAccess().get(Enchantments.EFFICIENCY);
        if (efficiencyHolder.isEmpty()) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            int level = EnchantmentHelper.getItemEnchantmentLevel(efficiencyHolder.get(), stack);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private int getBestToolSlot(BlockState state) {
        Inventory inventory = mc.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }
}
