package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.CustomSkyboxHook;
import java.awt.Color;
import net.minecraft.client.render.state.SkyRenderState;

public class CustomSkybox extends Module {
   private static CustomSkybox INSTANCE;
   private final Setting<Preset> preset;
   private final Setting<Color> topColor;
   private final Setting<Color> horizonColor;
   private final Setting<Boolean> hideSunMoon;
   private final Setting<Boolean> brightStars;
   private final Setting<Double> starBoost;

   public CustomSkybox() {
      super("CustomSkybox", "Replaces the sky with custom color presets.", Module.Category.VISUALS);
      this.preset      = this.mode("Preset", Preset.Sunset).setPage("General");
      this.topColor    = this.color("Top",     35, 60, 140, 255).setPage("General");
      this.horizonColor = this.color("Horizon", 255, 140, 70, 255).setPage("General");
      this.hideSunMoon = this.bool("HideSunMoon", true).setPage("General");
      this.brightStars = this.bool("BrightStars", true).setPage("General");
      this.starBoost   = this.num("StarBoost", 1.35, 0.5, 3.0).setPage("General");

      // Top/Horizon color settings only visible in Custom preset
      this.topColor.setVisibility(v -> this.preset.getValue() == Preset.Custom);
      this.horizonColor.setVisibility(v -> this.preset.getValue() == Preset.Custom);

      INSTANCE = this;
      CustomSkyboxHook.bind(new CustomSkyboxHook.Impl() {
         @Override public boolean enabled()       { return INSTANCE.isEnabled(); }
         @Override public boolean hideSunMoon()   { return INSTANCE.hideSunMoon.getValue(); }
         @Override public void apply(SkyRenderState sky) { INSTANCE.applyToState(sky); }
      });
   }

   public static CustomSkybox get() { return INSTANCE; }

   private void applyToState(SkyRenderState sky) {
      sky.skyColor            = toARGB(skyColor());
      sky.sunriseAndSunsetColor = toARGB(horizonColor());
      float stars = this.brightStars.getValue()
          ? (float)(sky.starBrightness * this.starBoost.getValue())
          : sky.starBrightness;
      sky.starBrightness = Math.min(stars, 1.0F);
   }

   private Color skyColor() {
      return switch (this.preset.getValue()) {
         case Sunset   -> new Color( 10,  36,  42);
         case Nebula   -> new Color( 92,  48, 108);
         case Midnight -> new Color(  6,  10,  28);
         case Aurora   -> new Color( 18,   8,  48);
         case Candy    -> new Color( 48,  36,  96);
         case Custom   -> this.topColor.getValue();
      };
   }

   private Color horizonColor() {
      return switch (this.preset.getValue()) {
         case Sunset   -> new Color(255, 118,  52);
         case Nebula   -> new Color( 30,  48, 110);
         case Midnight -> new Color(120,  60, 200);
         case Aurora   -> new Color( 72, 220, 160);
         case Candy    -> new Color(255, 150, 210);
         case Custom   -> this.horizonColor.getValue();
      };
   }

   private static int toARGB(Color c) {
      return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
   }

   public enum Preset { Sunset, Nebula, Midnight, Aurora, Candy, Custom }
}
