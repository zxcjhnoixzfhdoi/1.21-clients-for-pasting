package dev.ambience.features.settings;

import dev.ambience.features.Feature;
import dev.ambience.util.traits.Util;
import java.awt.Color;
import java.util.Objects;
import java.util.function.Predicate;
import org.joml.Vector2f;
import x.FeatureUpdateEvent;

@SuppressWarnings({"unchecked","rawtypes"})
public class Setting<T> {
   private final String name;
   private final T defaultValue;
   private T value;
   private T plannedValue;
   private T min;
   private T max;
   private boolean hasRestriction;
   private Predicate<T> visibility;
   private String description;
   private Feature feature;
   private String page = "General";
   private boolean vec2TrackBoundsSet;
   private float vec2TrackMin;
   private float vec2TrackMax;
   private boolean blockList;

   public Setting(String var1, T var2) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.plannedValue = (T)var2;
      this.description = "";
   }

   public Setting(String var1, T var2, String var3) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.plannedValue = (T)var2;
      this.description = var3;
   }

   public Setting(String var1, T var2, T var3, T var4, String var5) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.min = (T)var3;
      this.max = (T)var4;
      this.plannedValue = (T)var2;
      this.description = var5;
      this.hasRestriction = true;
   }

   public Setting(String var1, T var2, T var3, T var4) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.min = (T)var3;
      this.max = (T)var4;
      this.plannedValue = (T)var2;
      this.description = "";
      this.hasRestriction = true;
   }

   public Setting(String var1, T var2, T var3, T var4, Predicate<T> var5, String var6) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.min = (T)var3;
      this.max = (T)var4;
      this.plannedValue = (T)var2;
      this.visibility = var5;
      this.description = var6;
      this.hasRestriction = true;
   }

   public Setting(String var1, T var2, T var3, T var4, Predicate<T> var5) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.min = (T)var3;
      this.max = (T)var4;
      this.plannedValue = (T)var2;
      this.visibility = var5;
      this.description = "";
      this.hasRestriction = true;
   }

   public Setting(String var1, T var2, Predicate<T> var3) {
      this.name = var1;
      this.defaultValue = (T)var2;
      this.value = (T)var2;
      this.visibility = var3;
      this.plannedValue = (T)var2;
   }

   public String getName() {
      return this.name;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T var1) {
      this.setPlannedValue((T)var1);
      if (this.hasRestriction) {
         if (((Number)this.min).floatValue() > ((Number)var1).floatValue()) {
            this.setPlannedValue(this.min);
         }

         if (((Number)this.max).floatValue() < ((Number)var1).floatValue()) {
            this.setPlannedValue(this.max);
         }
      }

      FeatureUpdateEvent var2 = new FeatureUpdateEvent(this);
      Util.EVENT_BUS.post(var2);
      if (!var2.isCancelled()) {
         this.value = this.plannedValue;
      } else {
         this.plannedValue = this.value;
      }
   }

   public void reset() {
      this.setValue(this.getDefaultValue());
   }

   public T getPlannedValue() {
      return this.plannedValue;
   }

   public void setPlannedValue(T var1) {
      this.plannedValue = (T)var1;
   }

   public T getMin() {
      return this.min;
   }

   public void setMin(T var1) {
      this.min = (T)var1;
   }

   public T getMax() {
      return this.max;
   }

   public void setMax(T var1) {
      this.max = (T)var1;
   }

   public void setValueNoEvent(T var1) {
      this.setPlannedValue((T)var1);
      if (this.hasRestriction) {
         if (((Number)this.min).floatValue() > ((Number)var1).floatValue()) {
            this.setPlannedValue(this.min);
         }

         if (((Number)this.max).floatValue() < ((Number)var1).floatValue()) {
            this.setPlannedValue(this.max);
         }
      }

      this.value = this.plannedValue;
   }

   public Feature getFeature() {
      return this.feature;
   }

   public void setFeature(Feature var1) {
      this.feature = var1;
   }

   public int getEnum(String var1) {
      for (int var2 = 0; var2 < this.value.getClass().getEnumConstants().length; var2++) {
         Enum var3 = (Enum)this.value.getClass().getEnumConstants()[var2];
         if (var3.name().equalsIgnoreCase(var1)) {
            return var2;
         }
      }

      return -1;
   }

   public void setEnumValue(String var1) {
      if (var1 != null && var1.equalsIgnoreCase("HOMOVORE")) {
         var1 = "AMBIENCE";
      }

      for (Enum var5 : (Enum[])((Enum)this.value).getClass().getEnumConstants()) {
         if (var5.name().equalsIgnoreCase(var1)) {
            this.value = (T)var5;
         }
      }
   }

   public String currentEnumName() {
      return EnumConverter.getProperName((Enum)this.value);
   }

   public int currentEnum() {
      return EnumConverter.currentEnum((Enum)this.value);
   }

   public void increaseEnum() {
      this.plannedValue = (T)(Object)EnumConverter.increaseEnum((Enum)(Object)this.value);
      FeatureUpdateEvent var1 = new FeatureUpdateEvent(this);
      Util.EVENT_BUS.post(var1);
      if (!var1.isCancelled()) {
         this.value = this.plannedValue;
      } else {
         this.plannedValue = this.value;
      }
   }

   public void increaseEnumNoEvent() {
      this.value = (T)(Object)EnumConverter.increaseEnum((Enum)(Object)this.value);
   }

   public String getType() {
      if (this.isEnumSetting()) {
         return "Enum";
      } else if (this.isColorSetting()) {
         return "Color";
      } else if (this.isVec2fSetting()) {
         return "Pos";
      } else {
         return this.isBezierSetting() ? "Curve" : this.getClassName(this.defaultValue);
      }
   }

   public <K> String getClassName(K var1) {
      return var1.getClass().getSimpleName();
   }

   public String getDescription() {
      return Objects.requireNonNullElse(this.description, "");
   }

   public boolean isNumberSetting() {
      return this.value instanceof Number;
   }

   public boolean isEnumSetting() {
      return this.value instanceof Enum;
   }

   public boolean isStringSetting() {
      return this.value instanceof String;
   }

   public boolean isColorSetting() {
      return this.value instanceof Color;
   }

   public boolean isVec2fSetting() {
      return this.value instanceof Vector2f;
   }

   public boolean isBezierSetting() {
      return this.value instanceof BezierCurve;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public String getValueAsString() {
      return this.value.toString();
   }

   public boolean hasRestriction() {
      return this.hasRestriction;
   }

   public Setting<T> setVisibility(Predicate<T> var1) {
      this.visibility = var1;
      return this;
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.test(this.getValue());
   }

   public String getPage() {
      return this.page;
   }

   public Setting<T> setPage(String var1) {
      if (var1 == null) {
         this.page = "General";
         return this;
      } else {
         String var2 = var1.trim();
         this.page = var2.isEmpty() ? "General" : var2;
         return this;
      }
   }

   public Setting<T> setBlockList(boolean var1) {
      this.blockList = var1;
      return this;
   }

   public boolean isBlockList() {
      return this.blockList;
   }

   public Setting<T> setVec2TrackBounds(float var1, float var2) {
      this.vec2TrackMin = var1;
      this.vec2TrackMax = var2;
      this.vec2TrackBoundsSet = true;
      return this;
   }

   public boolean hasVec2TrackBounds() {
      return this.vec2TrackBoundsSet;
   }

   public float getVec2TrackMin() {
      return this.vec2TrackMin;
   }

   public float getVec2TrackMax() {
      return this.vec2TrackMax;
   }
}
