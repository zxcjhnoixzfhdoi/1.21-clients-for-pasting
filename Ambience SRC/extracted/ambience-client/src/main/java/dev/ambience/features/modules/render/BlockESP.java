package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Bind;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.BlockUtil;
import dev.ambience.util.render.EspDraw;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import x.Render3DEvent;

public class BlockESP extends Module {
   private static final int MAX_RESULTS = 512;
   private final Setting<Float>   range;
   private final Setting<String>  blocks;
   private final Setting<Bind>    pick;
   private final Setting<Boolean> fill;
   private final Setting<Boolean> tracers;
   private final Setting<Boolean> throughWalls;
   private final Setting<Color>   color;
   private final Set<Block>       targetBlocks = new LinkedHashSet<>();
   private final List<BlockPos>   found        = new ArrayList<>();
   private String                 lastBlockStr = "";

   public BlockESP() {
      super("BlockESP", "Boxes on block types you add, pick, or search.", Module.Category.VISUALS);
      this.range       = this.num("Range",       32f,  8f,  64f).setPage("General");
      this.blocks      = this.str("Blocks",      "").setBlockList(true).setPage("General");
      this.pick        = this.key("Pick",        Bind.none()).setPage("General");
      this.fill        = this.bool("Fill",       true).setPage("General");
      this.tracers     = this.bool("Tracers",   false).setPage("General");
      this.throughWalls = this.bool("ThroughWalls", true).setPage("General");
      this.color       = this.color("Color", 180, 80, 255, 220).setPage("Colors");
   }

   @Override public String getDisplayInfo() {
      if (this.targetBlocks.isEmpty()) return "no blocks";
      if (this.targetBlocks.size() == 1) return BlockUtil.a(java.util.Collections.singleton(this.targetBlocks.iterator().next()));
      return this.targetBlocks.size() + " blocks";
   }

   @Override public void onDisable() { this.found.clear(); }

   @Override
   public void onTick() {
      // pick key: add looked-at block
      if (this.pick.getValue().isDown() && nullCheck()) {
         var hit = mc.crosshairTarget;
         if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
            Block b = mc.world.getBlockState(bhr.getBlockPos()).getBlock();
            this.targetBlocks.add(b);
         }
      }

      // sync block list from string setting
      String str = this.blocks.getValue();
      if (!str.equals(this.lastBlockStr)) {
         this.lastBlockStr = str;
         this.targetBlocks.clear();
         this.targetBlocks.addAll(BlockUtil.a(str));
      }

      // scan chunks
      this.found.clear();
      if (!nullCheck() || this.targetBlocks.isEmpty()) return;
      float r = this.range.getValue();
      double r2 = (double) r * r;
      Vec3d eye = mc.player.getEyePos();
      BlockPos center = mc.player.getBlockPos();
      int ir = (int) Math.ceil(r);

      for (BlockPos pos : BlockPos.iterate(center.add(-ir, -ir, -ir), center.add(ir, ir, ir))) {
         if (eye.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r2) continue;
         BlockState state = mc.world.getBlockState(pos);
         if (!this.targetBlocks.contains(state.getBlock())) continue;
         this.found.add(pos.toImmutable());
         if (this.found.size() >= MAX_RESULTS) break;
      }
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (!nullCheck() || this.found.isEmpty()) return;
      Color col = this.color.getValue();
      boolean filled = this.fill.getValue();
      boolean xray   = this.throughWalls.getValue();
      boolean trace  = this.tracers.getValue();
      for (BlockPos pos : this.found) {
         Box box = new Box(pos);
         EspDraw.box(box, col, filled, xray);
         if (trace) EspDraw.tracer(box.getCenter(), col, xray);
      }
   }
}
