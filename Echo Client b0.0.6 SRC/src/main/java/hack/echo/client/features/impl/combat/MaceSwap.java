package hack.echo.client.features.impl.combat;

import com.mojang.blaze3d.platform.InputConstants;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.features.settings.impl.*;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.inventory.ItemGroups;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.world.WorldUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.EntityHitResult;

import static net.minecraft.world.item.enchantment.Enchantments.BREACH;
import static net.minecraft.world.item.enchantment.Enchantments.DENSITY;

public class MaceSwap extends Feature {

	public MaceSwap() {
		super(new FeatureInfo(
			Concat.of("Mace Swap"),
			Concat.of("Swaps to mace for attacks"),
			Category.COMBAT
		));

        inputSimulation.onChanged(this::syncDelayBounds);
        syncDelayBounds();
	}

    private static final RangeSetting delay = new RangeSetting(Concat.of("Delay"), 1, 1, 0, 20, 1, Concat.of(" Ticks"));
    public static final ItemPickerSetting itemWhitelist = new ItemPickerSetting(Concat.of("Item Whitelist"));
    private static final FloatSetting useDensityAboveFallDistance = new FloatSetting(Concat.of("Density above"), 1.5f, 0.0f, 8.0f, 0.1f, Concat.of(" Blocks"));
    private static final BoolSetting stunSlamBool = new BoolSetting(Concat.of("Stun Slam"), true);
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Input Simulation"), false, obj -> stunSlamBool.getValue());

    private static boolean shouldUseAfterAttack = false;

    @Override
    public void onEnable() {
        SwapStateManager.cancel(this);
        shouldUseAfterAttack = false;
        TargetControlModule.pendingShieldBreakTarget = null;
        super.onEnable();
    }

    @EventSubscribe
    public void onAttack(EventStartAttack.Pre event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!isAllowedWeapon()) return;
        if (SwapStateManager.hasActiveSwaps() && !SwapStateManager.isOwnerActive(this)) return;
        syncDelayBounds();

        boolean preferDensity = mc.player.fallDistance > useDensityAboveFallDistance.getValue();
        ResourceKey<Enchantment> maceEnchantment = preferDensity ? DENSITY : BREACH;

        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            if (!isEntityAllowed(target)) return;

            if (target instanceof Player playerTarget) {
                boolean isShielding = playerTarget.isUsingItem() && playerTarget.getUseItem().getItem() instanceof ShieldItem;
                if ((isShielding && !WorldUtils.isShieldFacingAway(playerTarget))) return;
            }
            int maceSlot = InventoryUtils.findMaceWithEnchantmentInHotbar(maceEnchantment, preferDensity);

            // Spear compatibility
            if (mc.options.keyUse.isDown()) {
                mc.options.keyUse.setDown(false);
                mc.player.stopUsingItem();
                shouldUseAfterAttack = true;
            }

            if (maceSlot != -1 && !SwapStateManager.isOwnerActive(this)) {
                int ticks = Math.max(0, (int) delay.getRandom());
                if (!SwapStateManager.swapTo(this, maceSlot, inputSimulation.getValue(), ticks, this::restoreUseKeyIfNeeded)) {
                    return;
                }
            }
        }
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGH)
    private void onShieldBreakPredicted(EventHandleInput.Early event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (!stunSlamBool.getValue()) return;
        if (!isAllowedWeapon()) return;

        // Shield break predictions should only live for the next input pass.
        LivingEntity target = TargetControlModule.pendingShieldBreakTarget;
        TargetControlModule.pendingShieldBreakTarget = null;

        if (target == null) return;
        if (SwapStateManager.hasActiveSwaps() && !SwapStateManager.isOwnerActive(this) && !SwapStateManager.isOwnerActive(ShieldBreaker.class)) return;
        syncDelayBounds();
        if (!isEntityAllowed(target)) return;

        boolean preferDensity = mc.player.fallDistance > useDensityAboveFallDistance.getValue();
        ResourceKey<Enchantment> maceEnchantment = preferDensity ? DENSITY : BREACH;
        int maceSlot = InventoryUtils.findMaceWithEnchantmentInHotbar(maceEnchantment, preferDensity);
        if (maceSlot == -1) return;
        if (!SwapStateManager.swapTo(this, maceSlot, inputSimulation.getValue(), Math.max(0, (int) delay.getRandom()), this::restoreUseKeyIfNeeded)) {
            return;
        }
        ((MinecraftAccessor) mc).invokeStartAttack();
    }

    private boolean isAllowedWeapon() {
        if (itemWhitelist.getSelectedCount() == 0) return true;
        
        ItemStack mainHand = InventoryUtils.getMainHandItem();
        if (mainHand.isEmpty()) {
            return itemWhitelist.isSelected(Items.AIR);
        }
        
        return itemWhitelist.isSelected(mainHand.getItem());
    }

	private boolean isEntityAllowed(LivingEntity entity) {
        if (!TargetUtils.isTargetAllowed(entity)) {
            return false;
        }
        return true;
    }
    
    public static boolean allowsAxes() {
        if (itemWhitelist.getSelectedCount() == 0) return true;
        for (Item item : BuiltInRegistries.ITEM) {
            if (ItemGroups.axes().matches(item) && itemWhitelist.isSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSwapActive() {
        return SwapStateManager.isOwnerActive(MaceSwap.class);
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onPacketReceive(EventPacketReceive e) {
        if (isNull()) return;
        Packet<?> packet = e.getPacket();
        if (packet instanceof ClientboundRespawnPacket
                || packet instanceof ClientboundPlayerPositionPacket) {
            resetSwapState();
            TargetControlModule.pendingShieldBreakTarget = null;
        }
    }

    @Override
    public void onDisable() {
        SwapStateManager.cancel(this);
        resetSwapState();
        TargetControlModule.pendingShieldBreakTarget = null;
        super.onDisable();
    }

    private static void resetSwapState() {
        shouldUseAfterAttack = false;
    }

    private void restoreUseKeyIfNeeded() {
        if (!shouldUseAfterAttack) return;
        shouldUseAfterAttack = false;
        if (mc.options.keyUse != null) {
            KeyMappingAccessor accessor = (KeyMappingAccessor) mc.options.keyUse;
            InputConstants.Key key = accessor.getKey();
            if (key != null) {
                int bindCode = key.getType() == InputConstants.Type.MOUSE
                        ? (0x80000000 | key.getValue())
                        : key.getValue();
                if (InputHandler.isBindDown(bindCode)) {
                    mc.options.keyUse.setDown(true);
                }
            }
        }
    }

    private void syncDelayBounds() {
        float minBound = inputSimulation.getValue() ? 1f : 0f;
        delay.setLowerBound(minBound);

        if (delay.getMinValue() < minBound) {
            delay.setMinValue(minBound);
        }

        if (delay.getMaxValue() < minBound) {
            delay.setMaxValue(minBound);
        }
    }

}
