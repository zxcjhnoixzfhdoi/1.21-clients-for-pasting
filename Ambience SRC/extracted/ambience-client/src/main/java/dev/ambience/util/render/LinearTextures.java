package dev.ambience.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class LinearTextures {
   private static final AtomicInteger IDS = new AtomicInteger();

   private LinearTextures() {
   }

   public static Identifier register(String var0, NativeImage var1) {
      int var2 = IDS.getAndIncrement();
      LinearTextures.LinearDynamicTexture var3 = new LinearTextures.LinearDynamicTexture(() -> "Ambience/" + var0 + var2, var1);
      var3.upload();
      Identifier var4 = Identifier.of("ambience", "linear/" + var0 + "_" + var2);
      MinecraftClient.getInstance().getTextureManager().registerTexture(var4, var3);
      return var4;
   }

   public static NativeImage fromArgb(int[] var0, int var1, int var2) {
      NativeImage var3 = new NativeImage(Format.RGBA, var1, var2, true);
      IntBuffer var4 = MemoryUtil.memIntBuffer(var3.imageId(), var1 * var2);

      for (int var8 : var0) {
         int var9 = var8 >>> 24 & 0xFF;
         int var10 = var8 >>> 16 & 0xFF;
         int var11 = var8 >>> 8 & 0xFF;
         int var12 = var8 & 0xFF;
         var4.put(var9 << 24 | var12 << 16 | var11 << 8 | var10);
      }

      return var3;
   }

   public static NativeImage readPng(InputStream var0) throws java.io.IOException {
      return NativeImage.read(var0);
   }

   private static final class LinearDynamicTexture extends NativeImageBackedTexture {
      LinearDynamicTexture(Supplier<String> var1, NativeImage var2) {
         super(var1, var2);
         this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      }
   }
}
