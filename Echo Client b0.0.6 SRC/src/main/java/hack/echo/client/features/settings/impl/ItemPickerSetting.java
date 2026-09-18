package hack.echo.client.features.settings.impl;

import hack.echo.client.utils.inventory.ItemGroups;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ItemPickerSetting extends RegistryPickerSetting<Item> {

    public ItemPickerSetting(CharSequence name) {
        super(name, () -> BuiltInRegistries.ITEM);
    }

    public ItemPickerSetting(CharSequence name, Predicate<Object> dependency) {
        super(name, () -> BuiltInRegistries.ITEM, dependency);
    }

    @Override
    public String getTypeId() { return "itm"; }

    @Override
    public List<EntryGroup<Item>> getGroups() {
        return ItemGroups.getAllGroups();
    }

    @Override
    public Function<Item, String> getNameProvider() {
        return item -> new ItemStack(item).getHoverName().getString();
    }
}
