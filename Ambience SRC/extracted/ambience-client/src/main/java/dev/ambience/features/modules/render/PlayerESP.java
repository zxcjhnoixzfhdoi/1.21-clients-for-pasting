package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.render.EspDraw;
import java.awt.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import x.Render3DEvent;

public class PlayerESP extends Module {
   private final Setting<Boolean> self;
   private final Setting<Boolean> invisibles;
   private final Setting<Boolean> fill;
   private final Setting<Boolean> tracers;
   private final Setting<Boolean> throughWalls;
   private final Setting<Color> color;

   public PlayerESP() {
      super("PlayerESP", "Boxes and tracers on other players.", Module.Category.VISUALS);
      this.self        = this.bool("Self", false).setPage("General");
      this.invisibles  = this.bool("Invisibles", true).setPage("General");
      this.fill        = this.bool("Fill", true).setPage("General");
      this.tracers     = this.bool("Tracers", true).setPage("General");
      this.throughWalls = this.bool("ThroughWalls", true).setPage("General");
      this.color       = this.color("Color", 80, 200, 255, 220).setPage("General");
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (!nullCheck()) return;
      float partialTicks = event.b();
      Color col     = this.color.getValue();
      boolean xray  = this.throughWalls.getValue();
      boolean filled = this.fill.getValue();
      boolean trace  = this.tracers.getValue();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (!shouldShow(player)) continue;
         Box box = getLerpedBox(player, partialTicks);
         EspDraw.box(box, col, filled, xray);
         if (trace) {
            EspDraw.tracer(box.getCenter(), col, xray);
         }
      }
   }

   private boolean shouldShow(PlayerEntity player) {
      if (player == null || !player.isAlive() || player.isSpectator()) return false;
      if (player == mc.player) return this.self.getValue();
      if (player.isInvisible()) return this.invisibles.getValue();
      return true;
   }

   private static Box getLerpedBox(PlayerEntity player, float partialTicks) {
      Vec3d lerpedPos = player.getLerpedPos(partialTicks);
      Vec3d offset    = lerpedPos.subtract(player.getEntityPos());
      return player.getBoundingBox().offset(offset);
   }
}
