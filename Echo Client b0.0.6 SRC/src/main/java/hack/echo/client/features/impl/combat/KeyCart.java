package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class KeyCart extends Feature {

    private final KeybindSetting activateKeybind = new KeybindSetting(Concat.of("Activate Key"), -1);

    private boolean activated;
    private int oldSlot = -1;
    private boolean railPlaced;
    private boolean tntPlaced;
    private BlockPos target;

    public KeyCart() {
        super(new FeatureInfo(
                Concat.of("Key Cart"),
                Concat.of("places tnt carts on rails on keypress"),
                Category.COMBAT));
    }

    @EventSubscribe
    private void onKeyPress(EventHandleInput.Early event) {
        if (isNull())
            return;
        if (activateKeybind.getKey() != -1 && InputHandler.isKeyDown(activateKeybind.getKey())) {
            activated = true;
        }
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
        activated = false;
        target = null;
        railPlaced = false;
        tntPlaced = false;
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
}
