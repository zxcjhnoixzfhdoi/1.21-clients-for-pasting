package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.render.EspDraw;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import x.Render3DEvent;

public class StorageESP extends Module {
   private final Setting<Boolean> chests;
   private final Setting<Boolean> enderChests;
   private final Setting<Boolean> barrels;
   private final Setting<Boolean> shulkers;
   private final Setting<Boolean> hoppers;
   private final Setting<Boolean> furnaces;
   private final Setting<Float>   range;
   private final Setting<Boolean> fill;
   private final Setting<Boolean> tracers;
   private final Setting<Boolean> throughWalls;
   private final Setting<Color>   chestColor;
   private final Setting<Color>   enderColor;
   private final Setting<Color>   shulkerColor;
   private final Setting<Color>   otherColor;
   private final List<Entry>      entries = new ArrayList<>();

   public StorageESP() {
      super("StorageESP", "Boxes and tracers on chests and other containers.", Module.Category.VISUALS);
      this.chests      = this.bool("Chests",      true).setPage("General");
      this.enderChests = this.bool("EnderChests", true).setPage("General");
      this.barrels     = this.bool("Barrels",     true).setPage("General");
      this.shulkers    = this.bool("Shulkers",    true).setPage("General");
      this.hoppers     = this.bool("Hoppers",    false).setPage("General");
      this.furnaces    = this.bool("Furnaces",   false).setPage("General");
      this.range       = this.num("Range",  64f, 16f, 128f).setPage("General");
      this.fill        = this.bool("Fill",       true).setPage("General");
      this.tracers     = this.bool("Tracers",   false).setPage("General");
      this.throughWalls = this.bool("ThroughWalls", true).setPage("General");
      this.chestColor  = this.color("Chest",  220, 170,  50, 220).setPage("Colors");
      this.enderColor  = this.color("Ender",  160,  50, 200, 220).setPage("Colors");
      this.shulkerColor = this.color("Shulker", 200, 80, 180, 220).setPage("Colors");
      this.otherColor  = this.color("Other",  140, 140, 140, 220).setPage("Colors");
   }

   @Override public void onDisable() { entries.clear(); }

   @Override
   public void onTick() {
      entries.clear();
      if (!nullCheck()) return;
      float r = this.range.getValue();
      double r2 = (double) r * r;
      Vec3d eye = mc.player.getEyePos();
      int chunkRadius = Math.max(1, (int) Math.ceil(r / 16.0) + 1);
      ChunkPos center = mc.player.getChunkPos();
      ClientChunkManager cm = mc.world.getChunkManager();

      for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; cx++) {
         for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; cz++) {
            WorldChunk chunk = cm.getWorldChunk(cx, cz);
            if (chunk == null) continue;
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
               double dist2 = eye.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
               if (dist2 > r2) continue;
               BlockEntity be = mc.world.getBlockEntity(pos);
               if (be == null) continue;
               Color col = colorFor(be.getCachedState());
               if (col == null) continue;
               entries.add(new Entry(pos.toImmutable(), col));
            }
         }
      }
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (!nullCheck() || entries.isEmpty()) return;
      boolean xray  = this.throughWalls.getValue();
      boolean filled = this.fill.getValue();
      boolean trace  = this.tracers.getValue();
      for (Entry e : entries) {
         Box box = new Box(e.pos);
         EspDraw.box(box, e.color, filled, xray);
         if (trace) EspDraw.tracer(box.getCenter(), e.color, xray);
      }
   }

   private Color colorFor(BlockState state) {
      if (state == null) return null;
      if (this.chests.getValue() && (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)))
         return this.chestColor.getValue();
      if (this.enderChests.getValue() && state.isOf(Blocks.ENDER_CHEST))
         return this.enderColor.getValue();
      if (this.shulkers.getValue() && state.isIn(BlockTags.SHULKER_BOXES))
         return this.shulkerColor.getValue();
      if (this.barrels.getValue() && state.isOf(Blocks.BARREL))
         return this.otherColor.getValue();
      if (this.hoppers.getValue() && (state.isOf(Blocks.HOPPER) || state.isOf(Blocks.DROPPER) || state.isOf(Blocks.DISPENSER)))
         return this.otherColor.getValue();
      if (this.furnaces.getValue() && (state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE)
                                    || state.isOf(Blocks.SMOKER) || state.isOf(Blocks.BREWING_STAND)))
         return this.otherColor.getValue();
      return null;
   }

   private record Entry(BlockPos pos, Color color) {}
}
