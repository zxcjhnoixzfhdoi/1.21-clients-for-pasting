package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.strings.Concat;

// Dear Cyde, my dearest apologies for the mess I've made.
// I feel bad for you needing to read this
// I promise to never do this again
// I love you, give tongue soon
// - graph
public class AutoCart extends Feature {

    private boolean active;
    private int step;
    private int oldSlot;
    private boolean railPlaced;
    private boolean tntPlaced;
    private BlockPos target;
    private final TimerUtils timer = new TimerUtils();

    public AutoCart() {
        super(new FeatureInfo(
            Concat.of("AutoCart"),
            Concat.of("Places rails and TNT minecarts while aiming"),
            Category.COMBAT
        ));
    }

    @EventSubscribe
    private void onTick(EventTick event) {
        if (isNull())
            return;

        if (!active && mc.player.isUsingItem() && mc.player.getUseItem().getItem() == Items.BOW) {

            if (mc.level != null) {
                var registry = mc.level.registryAccess();
                var flameEntry = registry.get(Enchantments.FLAME);
                if (flameEntry.isPresent()
                        && EnchantmentHelper.getItemEnchantmentLevel(flameEntry.get(), mc.player.getUseItem()) == 0)
                    return;
            }

            target = getTarget();
            if (target == null)
                return;

            if (!timer.hasReached(1000))
                return;

            active = true;
            step = 0;
            oldSlot = mc.player.getInventory().getSelectedSlot();
            railPlaced = false;
            tntPlaced = false;
        }

        if (!active)
            return;

        switch (step) {
            case 0 -> placeRail();
            case 1 -> placeTnt();
            case 2 -> reset();
        }

        step++;
    }

    private void placeRail() {
        if (railPlaced)
            return;
        int slot = findRailSlot();
        if (slot == -1)
            return;
        mc.player.getInventory().setSelectedSlot(slot);
        ((MinecraftAccessor) mc).invokeStartUseItem();
        railPlaced = true;
    }

    private void placeTnt() {
        if (tntPlaced)
            return;
        int slot = findItemSlot(Items.TNT_MINECART);
        if (slot == -1)
            return;
        mc.player.getInventory().setSelectedSlot(slot);
        ((MinecraftAccessor) mc).invokeStartUseItem();
        tntPlaced = true;
    }

    private void reset() {
        if (oldSlot != -1)
            mc.player.getInventory().setSelectedSlot(oldSlot);
        active = false;
        step = 0;
        target = null;
        railPlaced = false;
        tntPlaced = false;
        timer.reset();
    }

    private BlockPos getTarget() {
        HitResult hr = mc.hitResult;
        if (hr == null)
            return null;

        if (hr.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hr).getBlockPos().relative(((BlockHitResult) hr).getDirection());
        }

        return null;
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item)
                return i;
        }
        return -1;
    }

    private int findRailSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && isRail(stack.getItem()))
                return i;
        }
        return -1;
    }

    private boolean isRail(Item item) {
        return item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.DETECTOR_RAIL
                || item == Items.ACTIVATOR_RAIL;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (active)
            reset();
    }
}
