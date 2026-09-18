package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.inventory.BlockGroups;
import hack.echo.client.utils.strings.Concat;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Function;

public class Search extends Feature {

    public Search() {
        super(new FeatureInfo(
                "Search",
                "Renders selected blocks",
                Category.RENDER));
    }

    RegistryPickerSetting<Block> blockSetting = new RegistryPickerSetting<>(Concat.of("Blocks"), () -> BuiltInRegistries.BLOCK) {
        @Override
        public List<EntryGroup<Block>> getGroups() {
            return BlockGroups.getAllGroups();
        }

        @Override
        public Function<Block, String> getNameProvider() {
            return s -> new ItemStack(s.asItem()).getHoverName().getString();
        }
    };
    IntSetting alpha = new IntSetting("Alpha", 100, 0, 255);
    {
        blockSetting.onChanged(this::updateBlocks);
        alpha.onChanged(() -> Echo.blockEspManager.updateAlpha(this, alpha.getValue()));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.disableBlocks(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateBlocks();
    }

    private void updateBlocks() {
        if (!isEnabled()) return;
        var map = new Object2IntOpenHashMap<Block>();
        int a = alpha.getValue();
        for (Identifier id : blockSetting.getSelectedIds()) {
            BuiltInRegistries.ITEM.get(id).ifPresent(holder -> {
                Block block = Block.byItem(holder.value());
                if (block != Blocks.AIR) {
                    int rgb = block.defaultMapColor().col;
                    map.put(block, (a << 24) | (rgb & 0x00FFFFFF));
                }
            });
        }
        Echo.blockEspManager.enableBlocks(this, map);
    }
}
