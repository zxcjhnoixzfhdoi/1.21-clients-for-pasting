package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.XRayHook;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.registry.tag.BlockTags;

public class XRay extends Module {
   private static XRay INSTANCE;
   private final Setting<Boolean> ores;
   private final Setting<Boolean> containers;
   private final Setting<Boolean> portals;
   private final Setting<Boolean> hideFluids;
   private int lastRenderTick;

   public XRay() {
      super("XRay", "Hides most blocks so ores and chests show through the world.", Module.Category.VISUALS);
      this.ores       = this.bool("Ores",       true).setPage("General");
      this.containers = this.bool("Containers", true).setPage("General");
      this.portals    = this.bool("Portals",    true).setPage("General");
      this.hideFluids = this.bool("HideFluids", true).setPage("General");
      this.lastRenderTick = Integer.MIN_VALUE;
      INSTANCE = this;
      XRayHook.bind(new XRayHook.Impl() {
         @Override public boolean active()                    { return INSTANCE != null && INSTANCE.isEnabled(); }
         @Override public boolean hides(BlockState state)     { return shouldHide(state); }
         @Override public boolean hidesFluids()               { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideFluids.getValue(); }
      });
   }

   public static XRay get() { return INSTANCE; }

   // called by hooks
   public static boolean a(int unused) { return INSTANCE != null && INSTANCE.isEnabled(); }
   public static boolean a(BlockState state, int unused) { return shouldHide(state); }
   public static boolean b(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideFluids.getValue(); }

   @Override
   public void onEnable() {
      this.lastRenderTick = currentRenderTick();
      rebuildChunks();
   }

   @Override
   public void onDisable() {
      rebuildChunks();
   }

   @Override
   public void onTick() {
      int tick = currentRenderTick();
      if (tick != this.lastRenderTick) {
         this.lastRenderTick = tick;
         rebuildChunks();
      }
   }

   private static boolean shouldHide(BlockState state) {
      if (!a(0) || state == null || state.isAir()) return false;
      return !isVisible(state);
   }

   private static boolean isVisible(BlockState state) {
      if (INSTANCE.ores.getValue() && isOre(state))           return true;
      if (INSTANCE.containers.getValue() && isContainer(state)) return true;
      if (INSTANCE.portals.getValue() && isPortal(state))     return true;
      return false;
   }

   private static boolean isOre(BlockState state) {
      return state.isIn(BlockTags.COAL_ORES)
          || state.isIn(BlockTags.IRON_ORES)
          || state.isIn(BlockTags.GOLD_ORES)
          || state.isIn(BlockTags.DIAMOND_ORES)
          || state.isIn(BlockTags.EMERALD_ORES)
          || state.isIn(BlockTags.LAPIS_ORES)
          || state.isIn(BlockTags.REDSTONE_ORES)
          || state.isIn(BlockTags.COPPER_ORES)
          || state.isOf(Blocks.ANCIENT_DEBRIS)
          || state.isOf(Blocks.NETHER_QUARTZ_ORE)
          || state.isOf(Blocks.NETHER_GOLD_ORE);
   }

   private static boolean isContainer(BlockState state) {
      return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)
          || state.isOf(Blocks.ENDER_CHEST)
          || state.isIn(BlockTags.SHULKER_BOXES)
          || state.isOf(Blocks.BARREL);
   }

   private static boolean isPortal(BlockState state) {
      return state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.END_PORTAL);
   }

   private static int currentRenderTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc != null && mc.world != null ? mc.world.getTime() < 0 ? 0 : (int) mc.world.getTime() : 0;
   }

   private static void rebuildChunks() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.worldRenderer == null) return;
      WorldRenderer wr = mc.worldRenderer;
      wr.reload();
   }
}
