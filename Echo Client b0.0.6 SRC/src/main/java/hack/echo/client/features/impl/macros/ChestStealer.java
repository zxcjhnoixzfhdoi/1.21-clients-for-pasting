package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventSystemUpdate;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.Timer;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

public class ChestStealer extends Feature {

	public ChestStealer() {
		super(new FeatureInfo(
			Concat.of("Chest Stealer"),
			Concat.of("Automatically loots chests"),
			Category.MACROS
		));
	}

    public final IntSetting initialDelay = new IntSetting(Concat.of("Initial Delay"), 150, 1, 1000);
    public final IntSetting takeDelay = new IntSetting(Concat.of("Take Delay"), 70, 1, 1000);
    public final IntSetting closeDelay = new IntSetting(Concat.of("Close Delay"), 50, 1, 1000);
    public final BoolSetting onlyImportant = new BoolSetting(Concat.of("Only Important"), false);

    private final Timer initTimer = new Timer();
    private final Timer takeTimer = new Timer();
    private final Timer closeTimer = new Timer();

    private boolean tookAny = false;

    @EventSubscribe
    public void onSystemUpdateEvent(EventSystemUpdate event) {
        if (mc.player == null || mc.level == null) return;
        if (!this.isEnabled()) return;
        if (!(mc.player.containerMenu instanceof ChestMenu chest)) {
            initTimer.reset();
            takeTimer.reset();
            closeTimer.reset();
            tookAny = false;
            return;
        }

        if (!initTimer.passedMs(initialDelay.getValue())) {
            takeTimer.reset();
            return;
        }

        if (isChestEmpty(chest)) {
            if (tookAny && closeTimer.passedMs(closeDelay.getValue())) {
                mc.player.closeContainer();
            }
            return;
        }

        int chestSize = (chest.getContainer().getContainerSize() == 90 ? 54 : 27);
        for (int i = 0; i < chestSize; i++) {

            Slot slot = chest.getSlot(i);
            if (!slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            if (!isAllowed(stack)) continue;

            if (!takeTimer.passedMs(takeDelay.getValue())) continue;

            InventoryClickCompat.handleClick(
                mc.player.containerMenu.containerId,
                i,
                0,
                InventoryClickType.QUICK_MOVE,
                mc.player
            );

            tookAny = true;
            takeTimer.reset();
            closeTimer.reset();
        }
    }

    public boolean isChestEmpty(ChestMenu handler) {
        int chestSize = (handler.getContainer().getContainerSize() == 90 ? 54 : 27);

        for (int i = 0; i < chestSize; i++) {
            Slot slot = handler.getSlot(i);
            if (slot.hasItem() && isAllowed(slot.getItem())) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowed(ItemStack stack) {
        if (!onlyImportant.getValue()) return true;

        return stack.getItem() instanceof AxeItem
                || stack.has(DataComponents.FOOD) // Food All consumable food items including raw/cooked meats, fish, fruits/vegetables, prepared foods, special foods, and other consumables
                || stack.getItem() instanceof BlockItem // Blocks All placeable blocks including building materials, decorative blocks, functional blocks, and natural blocks
                || InventoryUtils.isSword(stack.getItem())
                || stack.has(DataComponents.WEAPON) // Weapons Items with WEAPON component including swords, axes, mace, trident, and spears
                || stack.has(DataComponents.TOOL) // Tools Items with TOOL component including pickaxes, shovels, hoes, shears, mace, and trident
                || stack.has(DataComponents.EQUIPPABLE) // Equippable Items that can be equipped including armor, horse armor, nautilus armor, wolf armor, elytra, shield, carpets, harnesses, saddle, and carved pumpkin
                || stack.getItem() instanceof EnderpearlItem
                || stack.getItem() instanceof BowItem // Bows and crossbows
                || stack.getItem() instanceof ArrowItem // All arrow types including standard, spectral, and tipped arrows
                || stack.getItem() instanceof FishingRodItem // Fishing Rod
                || stack.getItem() instanceof PotionItem; // Potions All potion variants including drinkable, splash, and lingering potions
    }
    @Override
    public void onDisable() {
        closeTimer.reset();
        takeTimer.reset();
        takeTimer.reset();
    }
}
