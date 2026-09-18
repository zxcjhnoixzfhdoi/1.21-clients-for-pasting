package dev.ambience.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public final class Access {
   private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
   private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

   private Access() {
   }

   public static float getFloat(Object var0, String var1, String var2) {
      try {
         return field(var0.getClass(), var1, var2).getFloat(var0);
      } catch (ReflectiveOperationException var4) {
         throw new IllegalStateException(var4);
      }
   }

   public static void setFloat(Object var0, String var1, String var2, float var3) {
      try {
         field(var0.getClass(), var1, var2).setFloat(var0, var3);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException(var5);
      }
   }

   public static int getInt(Object var0, String var1, String var2) {
      try {
         return field(var0.getClass(), var1, var2).getInt(var0);
      } catch (ReflectiveOperationException var4) {
         throw new IllegalStateException(var4);
      }
   }

   public static void setInt(Object var0, String var1, String var2, int var3) {
      try {
         field(var0.getClass(), var1, var2).setInt(var0, var3);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException(var5);
      }
   }

   public static void setBoolean(Object var0, String var1, String var2, boolean var3) {
      try {
         field(var0.getClass(), var1, var2).setBoolean(var0, var3);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException(var5);
      }
   }

   public static <T> T get(Object var0, String var1, String var2) {
      try {
         return (T)field(var0.getClass(), var1, var2).get(var0);
      } catch (ReflectiveOperationException var4) {
         throw new IllegalStateException(var4);
      }
   }

   public static void set(Object var0, String var1, String var2, Object var3) {
      try {
         field(var0.getClass(), var1, var2).set(var0, var3);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException(var5);
      }
   }

   public static void invoke(Object var0, String var1, String var2, Class<?>[] var3, Object... var4) {
      try {
         method(var0.getClass(), var1, var2, var3).invoke(var0, var4);
      } catch (ReflectiveOperationException var6) {
         throw new IllegalStateException(var6);
      }
   }

   private static Field field(Class<?> var0, String var1, String var2) {
      String var3 = var0.getName() + "#" + var1;
      return FIELDS.computeIfAbsent(var3, var3x -> {
         for (Class var4 = var0; var4 != null; var4 = var4.getSuperclass()) {
            for (String var8 : new String[]{var1, var2}) {
               if (var8 != null && !var8.isBlank()) {
                  try {
                     Field var9 = var4.getDeclaredField(var8);
                     var9.setAccessible(true);
                     return var9;
                  } catch (NoSuchFieldException var10) {
                  }
               }
            }
         }

         throw new IllegalStateException("No field " + var1 + "/" + var2 + " on " + var0.getName());
      });
   }

   private static Method method(Class<?> var0, String var1, String var2, Class<?>[] var3) {
      String var4 = var0.getName() + "#" + var1 + paramsKey(var3);
      return METHODS.computeIfAbsent(var4, var4x -> {
         for (Class var5 = var0; var5 != null; var5 = var5.getSuperclass()) {
            for (String var9 : new String[]{var1, var2}) {
               if (var9 != null && !var9.isBlank()) {
                  try {
                     Method var10 = var5.getDeclaredMethod(var9, var3);
                     var10.setAccessible(true);
                     return var10;
                  } catch (NoSuchMethodException var11) {
                  }
               }
            }
         }

         throw new IllegalStateException("No method " + var1 + "/" + var2 + " on " + var0.getName());
      });
   }

   private static String paramsKey(Class<?>[] var0) {
      StringBuilder var1 = new StringBuilder();

      for (Class var5 : var0) {
         var1.append(':').append(var5.getName());
      }

      return var1.toString();
   }

   public static float yRot(Object var0) {
      return getFloat(var0, "yaw", "yRot");
   }

   public static void setYRot(Object var0, float var1) {
      setFloat(var0, "yaw", "yRot", var1);
   }

   public static float xRot(Object var0) {
      return getFloat(var0, "pitch", "xRot");
   }

   public static void setXRot(Object var0, float var1) {
      setFloat(var0, "pitch", "xRot", var1);
   }

   public static float yRotO(Object var0) {
      return getFloat(var0, "lastYaw", "yRotO");
   }

   public static void setYRotO(Object var0, float var1) {
      setFloat(var0, "lastYaw", "yRotO", var1);
   }

   public static float xRotO(Object var0) {
      return getFloat(var0, "lastPitch", "xRotO");
   }

   public static void setXRotO(Object var0, float var1) {
      setFloat(var0, "lastPitch", "xRotO", var1);
   }

   public static float yHeadRot(Object var0) {
      return getFloat(var0, "headYaw", "yHeadRot");
   }

   public static void setYHeadRot(Object var0, float var1) {
      setFloat(var0, "headYaw", "yHeadRot", var1);
   }

   public static float yHeadRotO(Object var0) {
      return getFloat(var0, "lastHeadYaw", "yHeadRotO");
   }

   public static void setYHeadRotO(Object var0, float var1) {
      setFloat(var0, "lastHeadYaw", "yHeadRotO", var1);
   }

   public static float xBob(Object var0) {
      return getFloat(var0, "renderPitch", "xBob");
   }

   public static void setXBob(Object var0, float var1) {
      setFloat(var0, "renderPitch", "xBob", var1);
   }

   public static float xBobO(Object var0) {
      return getFloat(var0, "lastRenderPitch", "xBobO");
   }

   public static void setXBobO(Object var0, float var1) {
      setFloat(var0, "lastRenderPitch", "xBobO", var1);
   }

   public static float yBob(Object var0) {
      return getFloat(var0, "renderYaw", "yBob");
   }

   public static void setYBob(Object var0, float var1) {
      setFloat(var0, "renderYaw", "yBob", var1);
   }

   public static float yBobO(Object var0) {
      return getFloat(var0, "lastRenderYaw", "yBobO");
   }

   public static void setYBobO(Object var0, float var1) {
      setFloat(var0, "lastRenderYaw", "yBobO", var1);
   }

   public static void setEffectDuration(Object var0, int var1) {
      setInt(var0, "duration", "duration", var1);
   }

   public static void setItemActivationTicks(Object var0, int var1) {
      setInt(var0, "floatingItemTimer", "itemActivationTicks", var1);
   }

   public static void setMainHandHeight(Object var0, float var1) {
      setFloat(var0, "equipProgressMainHand", "mainHandHeight", var1);
   }

   public static void setOMainHandHeight(Object var0, float var1) {
      setFloat(var0, "lastEquipProgressMainHand", "oMainHandHeight", var1);
   }

   public static void setOffHandHeight(Object var0, float var1) {
      setFloat(var0, "equipProgressOffHand", "offHandHeight", var1);
   }

   public static void setOOffHandHeight(Object var0, float var1) {
      setFloat(var0, "lastEquipProgressOffHand", "oOffHandHeight", var1);
   }

   public static <T> T lightTexture(Object var0) {
      return get(var0, "glTexture", "texture");
   }

   public static void cameraSetRotation(Object var0, float var1, float var2) {
      invoke(var0, "setRotation", "setRotation", new Class[]{float.class, float.class}, var1, var2);
   }

   public static void cameraSetPosition(Object var0, Object var1) {
      invoke(var0, "setPos", "setPosition", new Class[]{Vec3d.class}, var1);
   }

   public static void ensureHasSentCarriedItem(Object var0) {
      if (var0 != null) {
         invoke(var0, "syncSelectedSlot", "ensureHasSentCarriedItem", new Class[0]);
      }
   }

   public static int rightClickDelay(Object var0) {
      return getInt(var0, "itemUseCooldown", "rightClickDelay");
   }

   public static void setRightClickDelay(Object var0, int var1) {
      setInt(var0, "itemUseCooldown", "rightClickDelay", var1);
   }

   public static void setJumping(Object var0, boolean var1) {
      setBoolean(var0, "jumping", "jumping", var1);
   }

   public static Vec2f moveVector(Object var0) {
      return get(var0, "movementVector", "moveVector");
   }

   public static void setMoveVector(Object var0, Vec2f var1) {
      set(var0, "movementVector", "moveVector", var1);
   }

   public static GuiRenderState guiRenderState(Object var0) {
      return get(var0, "state", "guiRenderState");
   }

   public static ScreenRect scissorPeek(Object var0) {
      Object var1 = get(var0, "scissorStack", "scissorStack");

      try {
         return (ScreenRect)method(var1.getClass(), "peekLast", "peek", new Class[0]).invoke(var1);
      } catch (ReflectiveOperationException var3) {
         throw new IllegalStateException(var3);
      }
   }
}
