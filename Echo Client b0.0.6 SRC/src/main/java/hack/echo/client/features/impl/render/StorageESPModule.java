package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.utils.blocks.StorageBlockUtils;
import hack.echo.client.utils.strings.Concat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class StorageESPModule extends Feature {

    public StorageESPModule() {
        super(new FeatureInfo(Concat.of("Storage ESP"), Concat.of("Highlights storage blocks"), Category.RENDER));
    }

    private final ColorSetting chestColor = new ColorSetting(Concat.of("Chest Color"), 255, 165, 0, 200, true);
    private final ColorSetting barrelColor = new ColorSetting(Concat.of("Barrel Color"), 139, 69, 19, 200, true);
    private final ColorSetting shulkerColor = new ColorSetting(Concat.of("Shulker Color"), 147, 112, 219, 200, true);
    private final ColorSetting enderChestColor = new ColorSetting(Concat.of("Ender Chest Color"), 0, 0, 255, 200, true);
    private final ColorSetting otherStorageColor = new ColorSetting(Concat.of("Other Storage Color"), 128, 128, 128,
            200, true);
    {
        chestColor.onChanged(this::update);
        barrelColor.onChanged(this::update);
        shulkerColor.onChanged(this::update);
        enderChestColor.onChanged(this::update);
        otherStorageColor.onChanged(this::update);
    }

    private void update() {
        if (!isEnabled())
            return;

        Object2IntMap<Block> map = new Object2IntOpenHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (StorageBlockUtils.isStorageBlock(block)) {
                map.put(block, getColorForBlock(block));
            }
        }
        Echo.blockEspManager.enableBlocks(this, map);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        update();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.disableBlocks(this);
    }

    private int getColorForBlock(Block block) {
        int chestColorARGB = chestColor.getARGB();
        int barrelColorARGB = barrelColor.getARGB();
        int shulkerColorARGB = shulkerColor.getARGB();
        int enderChestColorARGB = enderChestColor.getARGB();
        int otherStorageColorARGB = otherStorageColor.getARGB();

        return switch (block) {
            case EnderChestBlock ignored -> enderChestColorARGB;
            case AbstractChestBlock<?> ignored -> chestColorARGB;
            case BarrelBlock ignored -> barrelColorARGB;
            case ShulkerBoxBlock ignored -> shulkerColorARGB;
            case null, default -> otherStorageColorARGB;
        };
    }
}
