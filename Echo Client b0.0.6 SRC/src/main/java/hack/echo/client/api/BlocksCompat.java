package hack.echo.client.api;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class BlocksCompat {

    private static Block copperBlock;

    private BlocksCompat() {
    }

    public static Block getCopperBlock() {
        if (copperBlock != null) return copperBlock;

        //? if >26.1.2 {
        /*copperBlock = Blocks.COPPER_BLOCK.pick(net.minecraft.world.level.block.WeatheringCopper.WeatherState.UNAFFECTED, false);
        *///?} else {
        copperBlock = Blocks.COPPER_BLOCK;
        //?}

        return copperBlock;
    }
}
