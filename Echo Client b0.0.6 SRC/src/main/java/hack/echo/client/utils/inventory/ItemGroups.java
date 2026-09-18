package hack.echo.client.utils.inventory;

import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;

import java.util.ArrayList;
import java.util.List;

public final class ItemGroups {

    private ItemGroups() {}

    private static RegistryPickerSetting.EntryGroup<Item> createTagGroup(CharSequence name, TagKey<Item> tag) {
        return new RegistryPickerSetting.EntryGroup<>(
                name,
                item -> {
                    ItemStack stack = new ItemStack(item);
                    return stack.is(tag);
                }
        );
    }

    private static List<RegistryPickerSetting.EntryGroup<Item>> createGroups() {
        List<RegistryPickerSetting.EntryGroup<Item>> groups = new ArrayList<>();
        groups.add(createTagGroup(Concat.of("Swords"), ItemTags.SWORDS));
        groups.add(createTagGroup(Concat.of("Axes"), ItemTags.AXES));
        groups.add(new RegistryPickerSetting.EntryGroup<>(
                Concat.of("Mace"),
                item -> item instanceof MaceItem
        ));
        groups.add(new RegistryPickerSetting.EntryGroup<>(
                Concat.of("Elytra"),
                item -> item == Items.ELYTRA
        ));
        groups.add(new RegistryPickerSetting.EntryGroup<>(
                Concat.of("Fists"),
                item -> item == Items.AIR
        ));
        groups.add(createTagGroup(Concat.of("Helmet"), ItemTags.HEAD_ARMOR));
        groups.add(createTagGroup(Concat.of("Chestplate"), ItemTags.CHEST_ARMOR));
        groups.add(createTagGroup(Concat.of("Leggings"), ItemTags.LEG_ARMOR));
        groups.add(createTagGroup(Concat.of("Boots"), ItemTags.FOOT_ARMOR));
        groups.add(createTagGroup(Concat.of("Pickaxes"), ItemTags.PICKAXES));
        groups.add(createTagGroup(Concat.of("Shovels"), ItemTags.SHOVELS));
        groups.add(createTagGroup(Concat.of("Hoes"), ItemTags.HOES));
        groups.add(createTagGroup(Concat.of("Spears"), ItemTags.SPEARS));
        groups.add(createTagGroup(Concat.of("Planks"), ItemTags.PLANKS));
        return groups;
    }

    private static final class Holder {
        private static final List<RegistryPickerSetting.EntryGroup<Item>> ALL_GROUPS = createGroups();
    }

    public static List<RegistryPickerSetting.EntryGroup<Item>> getAllGroups() {
        return new ArrayList<>(Holder.ALL_GROUPS);
    }

    public static RegistryPickerSetting.EntryGroup<Item> swords() {
        return Holder.ALL_GROUPS.get(0);
    }

    public static RegistryPickerSetting.EntryGroup<Item> axes() {
        return Holder.ALL_GROUPS.get(1);
    }

    public static RegistryPickerSetting.EntryGroup<Item> mace() {
        return Holder.ALL_GROUPS.get(2);
    }

    public static RegistryPickerSetting.EntryGroup<Item> fists() {
        return Holder.ALL_GROUPS.get(4);
    }
}
