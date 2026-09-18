package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.ambience.inject.InjectHooks;
import dev.ambience.util.render.EspDraw;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;

import org.joml.Matrix4f;
import org.joml.Vector4f;

@Mixin(WorldRenderer.class)
public class MixinLevelRendererInject {
   @Inject(method = "render", at = "HEAD", captureArgs = true)
   public static void captureTracerOrigin(
      WorldRenderer var0,
      Object var1,
      RenderTickCounter var2,
      boolean var3,
      Camera var4,
      Matrix4f var5,
      Matrix4f var6,
      Matrix4f var7,
      GpuBufferSlice var8,
      Vector4f var9,
      boolean var10
   ) {
      EspDraw.updateTracerOrigin(var6, var5);
      InjectHooks.fireRender3D(var2.getTickProgress(false));
   }
}
