package dev.ambience.features.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ambience.Ambience;
import dev.ambience.features.Feature;
import dev.ambience.features.modules.client.DebugModule;
import dev.ambience.features.settings.BezierCurve;
import dev.ambience.features.settings.Bind;
import dev.ambience.features.settings.Setting;
import dev.ambience.loader.auth.PayloadGuard;
import dev.ambience.manager.ConfigManager;
import dev.ambience.util.DebugLogger;
import dev.ambience.util.traits.JsonSerializable;
import dev.ambience.util.traits.Toggleable;
import java.awt.Color;
import net.minecraft.util.Formatting;
import org.joml.Vector2f;
import x.ChatMessenger;
import x.FeatureUpdateEvent;
import x.Render2DEvent;
import x.Render3DEvent;

public class Module extends Feature implements JsonSerializable, Toggleable {
   private final String description;
   private final Module.Category category;
   public final Setting<Boolean> enabled = this.bool("Enabled", false).setPage("General");
   public final Setting<Bind> bind = this.key("Keybind", new Bind(-1)).setPage("General");
   public final Setting<Module.BindMode> bindMode = this.mode("BindMode", Module.BindMode.TOGGLE).setPage("General");
   public final Setting<String> displayName;
   public boolean hidden;

   public Module(String var1, String var2, Module.Category var3) {
      super(var1);
      this.displayName = this.str("DisplayName", var1).setPage("General");
      this.description = var2;
      this.category = var3;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onToggle() {
   }

   public void onLoad() {
   }

   public void onTick() {
   }

   public void onPreTick() {
   }

   public void onPostPositionTick() {
   }

   public void onRender2D(Render2DEvent var1) {
   }

   public void onRender3D(Render3DEvent var1) {
   }

   public void onUnload() {
   }

   public String getDebugState() {
      return null;
   }

   public String getDisplayInfo() {
      return null;
   }

   public String getMeta() {
      return null;
   }

   @Override
   public void enable() {
      if (this.category != Module.Category.CLIENT && !PayloadGuard.authorized()) {
         this.enabled.setValue(false);
      } else {
         this.enabled.setValue(true);
         EVENT_BUS.register(this);
         EVENT_BUS.post(new FeatureUpdateEvent(FeatureUpdateEvent.Type.TOGGLE_MODULE, this));
         this.onToggle();
         this.onEnable();
         if (!(this instanceof DebugModule)) {
            DebugLogger.a(this.getName(), "enable", this.getDebugState());
         }
      }
   }

   @Override
   public void disable() {
      this.enabled.setValue(false);
      EVENT_BUS.unregister(this);
      EVENT_BUS.post(new FeatureUpdateEvent(FeatureUpdateEvent.Type.TOGGLE_MODULE, this));
      this.onToggle();
      this.onDisable();
      if (!(this instanceof DebugModule)) {
         DebugLogger.a(this.getName(), "disable", "");
      }
   }

   public String getDisplayName() {
      return this.displayName.getValue();
   }

   public void setDisplayName(String var1) {
      Module var2 = Ambience.e.getModuleByDisplayName(var1);
      Module var3 = Ambience.e.getModuleByName(var1);
      if (var2 == null && var3 == null) {
         ChatMessenger.a(this.getDisplayName() + ", name: " + this.getName() + ", has been renamed to: " + var1);
         this.displayName.setValue(var1);
      } else {
         ChatMessenger.a("{red} A module of this name already exists.");
      }
   }

   @Override
   public boolean isEnabled() {
      return this.enabled.getValue();
   }

   @Override
   public boolean isToggled() {
      return this.isEnabled();
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public String getInfo() {
      return null;
   }

   public Bind getBind() {
      return this.bind.getValue();
   }

   public void setBind(int var1) {
      this.bind.setValue(new Bind(var1));
   }

   public String getFullArrayString() {
      return this.getDisplayName()
         + Formatting.GRAY
         + (this.getDisplayInfo() != null ? " [" + Formatting.WHITE + this.getDisplayInfo() + Formatting.GRAY + "]" : "");
   }

   @Override
   public JsonElement toJson() {
      JsonObject var1 = new JsonObject();

      for (Setting var3 : this.getSettings()) {
         try {
            if (var3.getValue() instanceof Bind var4) {
               var1.addProperty(var3.getName(), var4.getKey());
            } else if (var3.getValue() instanceof Color var5) {
               var1.addProperty(var3.getName(), var5.getRed() + "," + var5.getGreen() + "," + var5.getBlue() + "," + var5.getAlpha());
            } else if (var3.getValue() instanceof Vector2f var6) {
               var1.addProperty(var3.getName(), var6.x() + "," + var6.y());
            } else if (var3.getValue() instanceof BezierCurve var7) {
               var1.addProperty(var3.getName(), var7.toString());
            } else {
               var1.addProperty(var3.getName(), var3.getValueAsString());
            }
         } catch (Throwable var9) {
            Ambience.a.error("Failed to create JSON field", var9);
         }
      }

      return var1;
   }

   @Override
   public void fromJson(JsonElement var1) {
      if (var1 != null && !var1.isJsonNull()) {
         JsonObject var2 = var1.getAsJsonObject();
         if (var2.has("Enabled")) {
            try {
               boolean var3 = var2.get("Enabled").getAsBoolean();
               if (var3 && !this.isEnabled()) {
                  this.enable();
               } else if (!var3 && this.isEnabled()) {
                  this.disable();
               }
            } catch (Throwable var7) {
               Ambience.a.error("Failed to toggle module {} during load", this.getName(), var7);
            }
         }

         for (Setting var4 : this.getSettings()) {
            if (var4 != this.enabled) {
               try {
                  JsonElement var5 = var2.get(var4.getName());
                  if (var5 != null && !var5.isJsonNull()) {
                     ConfigManager.a(this, var4, var5);
                  }
               } catch (Throwable var6) {
                  Ambience.a.error("Failed to load from JSON", var6);
               }
            }
         }
      }
   }

   public Module.BindMode getBindMode() {
      return this.bindMode.getValue();
   }

   public enum BindMode {
      TOGGLE,
      HOLD;
   }

   public enum Category {
      COMBAT("Combat"),
      WORLD("World"),
      RENDER("Render"),
      VISUALS("Visuals"),
      MOVEMENT("Movement"),
      PLAYER("Player"),
      FUNNY("Funny"),
      CLIENT("Client"),
      CONFIGS("Configs"),
      HUD("Hud");

      private final String name;

      Category(String var3) {
         this.name = var3;
      }

      public String getName() {
         return this.name;
      }
   }
}
