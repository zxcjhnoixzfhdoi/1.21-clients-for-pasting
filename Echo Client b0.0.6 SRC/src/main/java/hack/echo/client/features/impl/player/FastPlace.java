package hack.echo.client.features.impl.player;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.inventory.BlockGroups;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;

// @see hack.echo.client.mixin.client.Minecraft
public class FastPlace extends Feature {

	public FastPlace() {
		super(new FeatureInfo(
			Concat.of("Fast Place"),
			Concat.of("Faster block placement"),
			Category.UTILITY
		));
	}

    public final IntSetting fastPlaceDelay = new IntSetting("Delay", 0, 0, 4);
	public final RegistryPickerSetting<Block> blockSetting = new RegistryPickerSetting<>(Concat.of("Blocks"), () -> BuiltInRegistries.BLOCK) {
		@Override
		public List<EntryGroup<Block>> getGroups() {
			return BlockGroups.getAllGroups();
		}

		@Override
		public Function<Block, String> getNameProvider() {
			return s -> new ItemStack(s.asItem()).getHoverName().getString();
		}
	};
}
