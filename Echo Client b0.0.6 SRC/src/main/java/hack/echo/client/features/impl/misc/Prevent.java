package hack.echo.client.features.impl.misc;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.blocks.BlockUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.inventory.ItemGroups;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class Prevent extends Feature {

    public Prevent() {
        super(new FeatureInfo(
            Concat.of("Prevent"),
            Concat.of("Prevents certain actions"),
            Category.INTERNALS
        ));
    }
    private static final BoolSetting noAttackAir = new BoolSetting(Concat.of("No Air Hits"), false);
    private static final ItemPickerSetting allowedItems = new ItemPickerSetting(Concat.of("Prevent With"), o -> noAttackAir.getValue());

    private static final BoolSetting cancelBlockBreaking = new BoolSetting(Concat.of("No Mining"), false);
    private static final ItemPickerSetting allowedMiningItems = new ItemPickerSetting(Concat.of("Prevent With"), o -> cancelBlockBreaking.getValue());

    private static final BoolSetting singleUseRespawnAnchor = new BoolSetting(Concat.of("Anchor Charge"), true);

    private static final BoolSetting preventOpenEnderChest = new BoolSetting(Concat.of("EChest Open"), false);
    private static final BoolSetting onlyOnObsidian = new BoolSetting(Concat.of("On Obsidian"), true, o -> preventOpenEnderChest.getValue());

    public final BoolSetting preventTotemOffhandUse = new BoolSetting(Concat.of("Untotem"), false);
    public final BoolSetting inInventoryOnly = new BoolSetting(Concat.of("In Inventory"), true, o -> preventTotemOffhandUse.getValue());
    public final BoolSetting inHotbarOnly = new BoolSetting(Concat.of("In Hotbar"), false, o -> preventTotemOffhandUse.getValue());

    @EventSubscribe
    private void onAirAttack(EventStartAttack e){
        if(isNull()) return;
        if(mc.hitResult == null) return;
        if(mc.hitResult.getType() != HitResult.Type.MISS) return;
        
        if(!noAttackAir.getValue()) return;
        if(shouldSkipPrevention(allowedItems)) return;

        e.cancel();
    }

    @EventSubscribe
    private void onBlockAttack(EventStartAttack e){
        if(isNull()) return;
        if(mc.hitResult == null) return;
        if(mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        
        if(!cancelBlockBreaking.getValue()) return;
        if(shouldSkipPrevention(allowedMiningItems)) return;

        e.cancel();
    }

    @EventSubscribe
    private void onBlockBreaking(EventBlockBreaking.Pre e){
        if(isNull()) return;
        if(!cancelBlockBreaking.getValue()) return;
        if(shouldSkipPrevention(allowedMiningItems)) return;
        if(mc.hitResult == null) return;
        if(mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        e.cancel();
    }

    @EventSubscribe
    private void onUse(EventStartUseItem.Pre e){
        if(isNull()) return;
        if(!singleUseRespawnAnchor.getValue()) return;
        if(mc.hitResult == null) return;
        if(mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if(mc.hitResult instanceof BlockHitResult bhr){
            BlockPos pos = bhr.getBlockPos();
            if (BlockUtils.isRespawnAnchorCharged(pos) && InventoryUtils.isHoldingItem(Items.GLOWSTONE)) e.cancel();
        }
    }

    @EventSubscribe
    private void onEchestUse(EventStartUseItem.Pre e){
        if(isNull()) return;
        if(!preventOpenEnderChest.getValue()) return;
        if(mc.hitResult == null) return;
        if(mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if(mc.hitResult instanceof BlockHitResult bhr){
            BlockPos pos = bhr.getBlockPos();
            Block enderChest = Blocks.ENDER_CHEST;
            if (BlockUtils.isBlock(pos, enderChest)) {
                if(onlyOnObsidian.getValue()){
                    BlockPos belowPos = pos.below();
                    if(BlockUtils.isBlock(belowPos, Blocks.OBSIDIAN)){
                        e.cancel();
                    }
                } else {
                    e.cancel();
                }
            }
        }
    }

    @EventSubscribe
    private void onSwapHand(EventSwapHands e){
        if(isNull()) return;
        if(!preventTotemOffhandUse.getValue()) return;
        if(!inHotbarOnly.getValue()) return;

        // Prevent offhand slot change if it contains a totem in hotbar view only
        if (mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            e.cancel();
        }
    }

    private boolean shouldSkipPrevention(ItemPickerSetting setting){
        if (setting.getSelectedCount() == 0) return false;
        
        ItemStack mainHand = InventoryUtils.getMainHandItem();
        Item mainHandItem = mainHand.isEmpty() ? Items.AIR : mainHand.getItem();
        
        if (setting.isSelected(mainHandItem)) return false;
        
        if (ItemGroups.swords().matches(mainHandItem) && hasGroupSelected(setting, ItemGroups.swords())) return false;
        if (ItemGroups.axes().matches(mainHandItem) && hasGroupSelected(setting, ItemGroups.axes())) return false;
        if (ItemGroups.mace().matches(mainHandItem) && hasGroupSelected(setting, ItemGroups.mace())) return false;
        if (mainHandItem == Items.AIR && hasGroupSelected(setting, ItemGroups.fists())) return false;
        
        return true;
    }
    
    private boolean hasGroupSelected(ItemPickerSetting setting, RegistryPickerSetting.EntryGroup<Item> group) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (group.matches(item) && setting.isSelected(item)) {
                return true;
            }
        }
        return false;
    }
}
