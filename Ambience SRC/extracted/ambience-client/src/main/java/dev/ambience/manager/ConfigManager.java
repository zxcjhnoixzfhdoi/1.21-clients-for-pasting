package dev.ambience.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ambience.Ambience;
import dev.ambience.features.Feature;
import dev.ambience.features.settings.BezierCurve;
import dev.ambience.features.settings.Bind;
import dev.ambience.features.settings.Setting;
import dev.ambience.util.traits.JsonSerializable;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector2f;

public class ConfigManager implements JsonSerializable {
   private static final Gson a = new GsonBuilder().setPrettyPrinting().create();
   private static final Path b = FabricLoader.getInstance().getConfigDir().resolve("ambience");
   private static final Path c = b.resolve("config.json");
   private static final Path d = b.resolve("profiles");
   private static final Pattern e = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 _.\\-]{1,46}[A-Za-z0-9]$");

   public void a() {
      if (Files.exists(c)) {
         try {
            this.a(JsonParser.parseString(Files.readString(c)).getAsJsonObject(), false);
         } catch (IOException var2) {
            Ambience.a.error("Failed to read config", var2);
         } catch (Exception var3) {
            Ambience.a.error("Failed to load config", var3);
         }
      }
   }

   public void b() {
      if (Ambience.e != null) {
         try {
            Files.createDirectories(c.getParent());
            Files.writeString(c, a.toJson(this.c()));
         } catch (IOException var2) {
            Ambience.a.error("Failed to save config", var2);
         }
      }
   }

   public JsonObject c() {
      JsonObject var1 = new JsonObject();
      if (Ambience.e != null) {
         var1.add("Modules", Ambience.e.toJson());
      }

      if (Ambience.d != null) {
         var1.add("Commands", Ambience.d.toJson());
      }

      return var1;
   }

   public void a(JsonObject var1) {
      this.a(var1, true);
   }

   public void a(JsonObject var1, boolean var2) {
      if (var1 != null) {
         if (var1.has("Modules") && Ambience.e != null) {
            Ambience.e.fromJson(var1.get("Modules"));
         }

         if (Ambience.d != null) {
            Ambience.d.fromJson(var1.has("Commands") ? var1.get("Commands") : null);
         }

         if (var2) {
            this.b();
         }
      }
   }

   public static boolean a(String var0) {
      return var0 != null && e.matcher(var0.trim()).matches();
   }

   public List<String> d() {
      ArrayList<String> var1 = new ArrayList<>();
      if (!Files.isDirectory(d)) {
         return var1;
      }

      try (DirectoryStream<Path> var2 = Files.newDirectoryStream(d, "*.json")) {
         for (Path var4 : var2) {
            String var5 = var4.getFileName().toString();
            var1.add(var5.substring(0, var5.length() - 5));
         }
      } catch (IOException var8) {
         Ambience.a.warn("Failed to list local profiles", var8);
      }

      var1.sort(Comparator.comparing(var0 -> var0.toLowerCase(Locale.ROOT)));
      return var1;
   }

   public void b(String var1) throws java.io.IOException {
      var1 = var1 == null ? "" : var1.trim();
      if (!a(var1)) {
         throw new IOException("invalid profile name");
      }

      Files.createDirectories(d);
      Path var2 = d.resolve(e(var1) + ".json");
      Files.writeString(var2, a.toJson(this.c()));
   }

   public void c(String var1) throws java.io.IOException {
      var1 = var1 == null ? "" : var1.trim();
      Path var2 = d.resolve(e(var1) + ".json");
      if (!Files.isRegularFile(var2)) {
         throw new IOException("profile not found");
      }

      this.a(JsonParser.parseString(Files.readString(var2)).getAsJsonObject());
   }

   public void d(String var1) throws java.io.IOException {
      var1 = var1 == null ? "" : var1.trim();
      Path var2 = d.resolve(e(var1) + ".json");
      Files.deleteIfExists(var2);
   }

   private static String e(String var0) {
      return var0.trim().replace('/', '_').replace('\\', '_');
   }

   public static void a(Feature var0, Setting var1, JsonElement var2) {
      if (var2 != null && !var2.isJsonNull()) {
         switch (var1.getType()) {
            case "Boolean":
               var1.setValueNoEvent(var2.getAsBoolean());
               break;
            case "Double":
               var1.setValueNoEvent(var2.getAsDouble());
               break;
            case "Float":
               var1.setValueNoEvent(var2.getAsFloat());
               break;
            case "Integer":
               var1.setValueNoEvent(var2.getAsInt());
               break;
            case "String":
               var1.setValueNoEvent(var2.getAsString().replace("_", " "));
               break;
            case "Bind":
               var1.setValueNoEvent(new Bind(var2.getAsInt()));
               break;
            case "Color":
               try {
                  String[] var9 = var2.getAsString().split(",");
                  if (var9.length == 4) {
                     var1.setValueNoEvent(new Color(Integer.parseInt(var9[0]), Integer.parseInt(var9[1]), Integer.parseInt(var9[2]), Integer.parseInt(var9[3])));
                  }
               } catch (Exception var7) {
               }
               break;
            case "Pos":
               try {
                  String[] var8 = var2.getAsString().split(",");
                  if (var8.length == 2) {
                     var1.setValueNoEvent(new Vector2f(Float.parseFloat(var8[0]), Float.parseFloat(var8[1])));
                  }
               } catch (Exception var6) {
               }
               break;
            case "Curve":
               BezierCurve var5 = BezierCurve.parse(var2.getAsString());
               if (var5 != null) {
                  var1.setValueNoEvent(var5);
               }
               break;
            case "Enum":
               var1.setEnumValue(var2.getAsString());
         }
      }
   }

   @Override
   public JsonElement toJson() {
      return null;
   }

   @Override
   public void fromJson(JsonElement var1) {
   }
}
