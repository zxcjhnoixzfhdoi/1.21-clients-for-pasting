package we.devs.opium.api.utilities;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.client.events.EventTick;

import java.util.ArrayList;
import java.util.List;

public class FastHoleUtil implements IMinecraft, EventListener {

    private FastHoleUtil() {}

    public static FastHoleUtil INSTANCE = new FastHoleUtil();

    public static @Nullable Hole getHole(BlockPos pos) {
        HoleType type = HoleType.SINGLE;
        List<BlockPos> airList = new ArrayList<>();
        int obsidian = 0;
        int bedrock = 0;
        int air = 0;
        int other = 0;

        if(!isAir(pos) || !(isObby(pos.add(0, -1, 0)) || isBedrock(pos.add(0, -1, 0)))) return null;
        else airList.add(pos);

        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = pos.add(direction.getVector());
            if(direction == Direction.UP) {
                if(!isAir(offsetPos)) return null;
                continue;
            }
            if(isObby(offsetPos)) obsidian++;
            else if(isBedrock(offsetPos)) bedrock++;
            else if(isAir(offsetPos)) {
                if(air != 0 || direction == Direction.DOWN) return null;
                else {
                    air++;
                    int obOrBed = 0;
                    for (Direction dir2 : Direction.values()) {
                        if(dir2 == Direction.UP) {
                            if(!isAir(offsetPos)) return null;
                            continue;
                        }
                        BlockPos offsetPos2 = pos.add(direction.getVector());
                        if(isObby(offsetPos2)) {
                            obsidian++;
                            obOrBed++;
                        } else if(isBedrock(offsetPos2)) {
                            bedrock++;
                            obOrBed++;
                        } else if(isAir(offsetPos2) && dir2 != direction.getOpposite()) {
                            return null;
                        }
                    }
                    if(obOrBed > 3)  {
                        type = HoleType.DOUBLE;
                        airList.add(offsetPos);
                    }
                }
            } else other++;
        }

        HoleSafety safety;
        if(((air > 0 && type == HoleType.SINGLE) || other > 0) && airList.size() < 2) safety = HoleSafety.UNSAFE;
        else if(obsidian > 0 || bedrock > 0) {
            if(obsidian > 0) {
                if(bedrock > 0) safety = HoleSafety.PARTIALLY_UNBREAKABLE;
                else safety = HoleSafety.BREAKABLE;
            } else safety = HoleSafety.UNBREAKABLE;
        } else safety = HoleSafety.UNSAFE;

        return new Hole(airList, type, safety);
    }

    static boolean isAir(BlockPos p) {
        return mc.world.getBlockState(p).isReplaceable();
    }

    static boolean isObby(BlockPos p) {
        return mc.world.getBlockState(p).getBlock().equals(Blocks.OBSIDIAN);
    }

    static boolean isBedrock(BlockPos p) {
        return mc.world.getBlockState(p).getBlock().equals(Blocks.BEDROCK);
    }

    public record Hole(List<BlockPos> air, HoleType type, HoleSafety safety) {}

    public enum HoleType {
        SINGLE,
        DOUBLE,
        QUAD // todo
    }

    public enum HoleSafety {
        UNBREAKABLE,
        PARTIALLY_UNBREAKABLE,
        BREAKABLE,
        UNSAFE // no obsidian or bedrock
    }

    public static List<FastHoleUtil.Hole> holes = new ArrayList<>();
    public static List<BlockPos> checked = new ArrayList<>();

    @Override
    public void onTick(EventTick event) {
        if(mc.world == null || mc.player == null) return;
        List<BlockPos> checked = new ArrayList<>();
        List<FastHoleUtil.Hole> holes = new ArrayList<>();
        int range = 15;
        BlockPos initPos = mc.player.getBlockPos();

        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    BlockPos pos = initPos.add(x, y, z);
                    if(checked.contains(pos)) continue;
                    FastHoleUtil.Hole hole = FastHoleUtil.getHole(pos);
                    if(hole == null) continue;
                    holes.add(hole);
                    checked.addAll(hole.air());
                }
            }
        }

        FastHoleUtil.holes = holes;
        FastHoleUtil.checked = checked;
    }
}
