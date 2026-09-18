package hack.echo.client.utils.inventory;

import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class BlockGroups {

    private BlockGroups() {}

    private static List<RegistryPickerSetting.EntryGroup<Block>> createGroups() {
        List<RegistryPickerSetting.EntryGroup<Block>> groups = new ArrayList<>();
        groups.add(createTagGroup(Concat.of("Coal Ores"), BlockTags.COAL_ORES));
        groups.add(createTagGroup(Concat.of("Iron Ores"), BlockTags.IRON_ORES));
        groups.add(createTagGroup(Concat.of("Gold Ores"), BlockTags.GOLD_ORES));
        groups.add(createTagGroup(Concat.of("Diamond Ores"), BlockTags.DIAMOND_ORES));
        groups.add(createTagGroup(Concat.of("Emerald Ores"), BlockTags.EMERALD_ORES));
        groups.add(createTagGroup(Concat.of("Lapis Ores"), BlockTags.LAPIS_ORES));
        groups.add(createTagGroup(Concat.of("Redstone Ores"), BlockTags.REDSTONE_ORES));
        groups.add(createTagGroup(Concat.of("Copper Ores"), BlockTags.COPPER_ORES));
        return groups;
    }

    private static RegistryPickerSetting.EntryGroup<Block> createTagGroup(CharSequence name, TagKey<Block> tag) {
        return new RegistryPickerSetting.EntryGroup<>(
                name,
                block -> {
                    BlockState state = block.defaultBlockState();
                    return state.is(tag);
                }
        );
    }

    private static final class Holder {
        private static final List<RegistryPickerSetting.EntryGroup<Block>> ALL_GROUPS = createGroups();
    }

    public static List<RegistryPickerSetting.EntryGroup<Block>> getAllGroups() {
        return new ArrayList<>(Holder.ALL_GROUPS);
    }
}
