package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventSwingHand;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.combat.SpearUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;

public class SpearLungeModule extends Feature {

	public SpearLungeModule() {
		super(new FeatureInfo(
			Concat.of("Spear Lunge"),
			Concat.of("Uses the spear's lunge ability to move quickly"),
			Category.COMBAT
		));
	}

    private final ModeSetting mode = new ModeSetting(Concat.of("Mode"), Concat.of("Attack"), Concat.of("Attack"), Concat.of("Keypress"));
    private final BoolSetting silent = new BoolSetting(Concat.of("Silent"), false);
    private final ModeSetting cooldown = new ModeSetting(Concat.of("Cooldown"), Concat.of("Vanilla"), Concat.of("Vanilla"), Concat.of("Paper"));
    private final IntSetting cooldownThreshold = new IntSetting(Concat.of("Cooldown %"), 85, 0, 100, sexy -> cooldown.is(Concat.of("Vanilla")));
    private final IntSetting multiplier = new IntSetting(Concat.of("Multiplier"), 1, 1, 10);
    private final ItemPickerSetting itemWhitelist = new ItemPickerSetting(Concat.of("Item Whitelist"), sexy -> mode.is(Concat.of("Attack")));
    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("Activate Key"), -1, sexy -> mode.is(Concat.of("Keypress")));

    private static final int switchDelay = 1;
    private final TimerUtils timer = new TimerUtils();
    private int originalSlot = -1;
    private boolean shouldSwitchBack = false;
    private boolean keyWasDown = false;

    private int getSpearSlotWithLunge() {
        return InventoryUtils.findItemWithEnchantmentInHotbar(
            stack -> InventoryUtils.isItemSpear(stack.getItem()),
            Enchantments.LUNGE,
            false
        );
    }

    private boolean isHoldingSpearWithLunge() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.isEmpty() || !InventoryUtils.isItemSpear(mainHand.getItem())) {
            return false;
        }
        if (mc.level == null) return false;
        var opt = mc.level.registryAccess().get(Enchantments.LUNGE);
        return opt.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(opt.get(), mainHand) > 0;
    }

    @EventSubscribe
    public final void onDoAttackPre(EventStartAttack.Pre event) {
        if (!mode.is(Concat.of("Attack"))) return;
        if (isNull()) return;
        if (!isEnabled()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (mc.isPaused()) return;
        if (isHoldingSpearWithLunge()) return;
        if (!isHoldingAllowed()) return;

        performSwap();
    }

    @EventSubscribe
    public void onStartAttack(EventStartAttack event) {
        if (!silent.getValue()) return;
        if (isNull()) return;
        if (!isEnabled()) return;

        boolean holdingLungeSpear = isHoldingSpearWithLunge();
        boolean silentSwapped = silent.getValue() && shouldSwitchBack;

        if (!holdingLungeSpear && !silentSwapped) return;

        event.cancel();
        sendStabPackets();

        if (silentSwapped) {
            InventoryUtils.silentSwapBack();
            shouldSwitchBack = false;
        }
    }

    @EventSubscribe
    public void onHandleInputKeypress(EventHandleInput.Early event) {
        if (!mode.is(Concat.of("Keypress"))) return;
        if (isNull()) return;
        if (!isEnabled()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (mc.isPaused()) return;
        if (shouldSwitchBack) return;

        boolean keyDown = InputHandler.isBindDown(activateKey.getKey());
        if (keyDown && !keyWasDown) {
            if (isHoldingSpearWithLunge()) {
                keyWasDown = true;
                return;
            }
            if (performSwap()) {
                ((MinecraftAccessor) mc).invokeStartAttack();
            }
        }
        keyWasDown = keyDown;
    }

    private boolean performSwap() {
        int spearSlot = getSpearSlotWithLunge();
        if (spearSlot == -1) return false;

        if (cooldown.is(Concat.of("Vanilla"))) {
            if (mc.player.getAttackStrengthScale(0.0F) < cooldownThreshold.getValue() / 100.0F) return false;
        } else if (cooldown.is(Concat.of("Paper"))) {
            ItemStack spearStack = mc.player.getInventory().getItem(spearSlot);
            if (!SpearUtils.isSpearSwapFullyCooled(mc.player, spearStack, 0.0F)) return false;
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (currentSlot == spearSlot) return false;

        originalSlot = currentSlot;

        if (silent.getValue()) {
            InventoryUtils.silentSwapTo(spearSlot);
        } else {
            mc.player.getInventory().setSelectedSlot(spearSlot);
        }

        timer.reset();
        shouldSwitchBack = true;
        return true;
    }

    private void sendStabPackets() {
        for (int i = 0; i < multiplier.getValue(); i++) {
            mc.player.connection.send(
                new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN)
            );
        }
    }

    @EventSubscribe
    public void onSwingHand(EventSwingHand event) {
        if (event.getPlayer() == mc.player) {
            SpearUtils.onPlayerSwing(mc.player);
        }
    }

    @EventSubscribe
    public void onHandleInputPost(EventHandleInput.Post event) {
        if (isNull()) return;

        if (shouldSwitchBack) {
            if (silent.getValue()) {
                InventoryUtils.silentSwapBack();
                shouldSwitchBack = false;
            } else if (timer.hasReachedTicks(1)) {
                if (originalSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                    originalSlot = -1;
                }
                shouldSwitchBack = false;
            }
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
        if (!isEnabled() || !cooldown.is(Concat.of("Paper")) || isNull()) {
            return ItemStack.EMPTY;
        }

        int spearSlot = getSpearSlotWithLunge();
        return spearSlot == -1 ? ItemStack.EMPTY : mc.player.getInventory().getItem(spearSlot);
    }

    @Override
    public void onDisable() {
        if (originalSlot != -1 && !silent.getValue()) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        shouldSwitchBack = false;
        keyWasDown = false;
    }
}
